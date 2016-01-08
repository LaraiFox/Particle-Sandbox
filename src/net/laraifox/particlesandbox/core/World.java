package net.laraifox.particlesandbox.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.laraifox.particlesandbox.UtilCL;
import net.laraifox.particlesandbox.collision.AABBCollider;
import net.laraifox.particlesandbox.collision.Quadtree;
import net.laraifox.particlesandbox.interfaces.ICollidable;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;
import net.laraifox.particlesandbox.interfaces.IRenderObject;
import net.laraifox.particlesandbox.physicstasks.CollisionThread;
import net.laraifox.particlesandbox.physicstasks.EnvironmentCollisionTask;
import net.laraifox.particlesandbox.physicstasks.MouseForceTask;
import net.laraifox.particlesandbox.physicstasks.PhysicsThread;
import net.laraifox.particlesandbox.physicstasks.QuadtreeSetupTask;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.OpenCLException;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;

public class World {
	public static final float GRAVITATIONAL_CONSTANT = 0.00006673f;
	public static final float PARTICLE_MASS = 100.0f;
	public static final float PARTICLE_RADIUS = 2.0f;

	private PhysicsThread[] physicsThreads;
	private CollisionThread[] collisionThreads;

	private Random random;

	private float width, height;

	private final float CAMERA_SPEED = 1.5f;
	private Vector2f cameraStartPosition;
	private Camera camera;
	private Vector2f cameraSize;

	private Quadtree quadtree;

	private Shader shader;

	private int particleVBO;
	private int particleCount;
	private Particle[] particles;

	ArrayList<Vector2f> averagePositions = new ArrayList<Vector2f>();

	private int wallVBO;
	private ArrayList<Wall> walls;

	private AABBCollider testCollider;
	private int collidingParticleVBO;
	private ArrayList<ICollidable> collidingParticles;

	private HashMap<Integer, ArrayList<IRenderObject>> renderMap;

	private boolean doGlobalGravity;

	private float force = 60.0f;
	private float threshold = 20.0f;

	private CLContext context;
	private CLCommandQueue queue;
	private CLKernel kernel;
	private CLProgram program;
	private PointerBuffer kernel1DGlobalOffset;
	private PointerBuffer kernel1DGlobalWorkSize;
	private CLMem positionMem;
	private CLMem velocityMem;
	private CLMem particleCountMem;
	private CLMem frameDeltaMem;
	private FloatBuffer positionBuffer;
	private FloatBuffer velocityBuffer;
	private IntBuffer particleCountBuffer;
	private FloatBuffer frameDeltaBuffer;

	private Shader testShader;
	private int framebufferID;
	private int colorTextureID;

	public World(float width, float height, int particleCount, Random random) {
		Particle.setWorld(this);

		this.physicsThreads = new PhysicsThread[GameManager.THREAD_COUNT];
		this.collisionThreads = new CollisionThread[GameManager.THREAD_COUNT];

		this.random = random;

		this.width = width * 1;
		this.height = height * 1;

		this.cameraStartPosition = new Vector2f(this.width / 2.0f - width / 2.0f, this.height / 2.0f - height / 2.0f);
		this.camera = new Camera(createProjectionMatrix(0, width, 0, height, 0, 1), cameraStartPosition);
		this.cameraSize = new Vector2f(width, height);

		float quadtreeDepth = this.width;
		if (this.height > this.width)
			quadtreeDepth = this.height;
		quadtreeDepth = (float) Math.ceil(Math.sqrt((quadtreeDepth / 100.0f)));
		this.quadtree = new Quadtree(new AABBCollider(0, 0, this.width, this.height), 100, (int) quadtreeDepth);

		try {
			this.shader = new Shader("res/shaders/GLSL 1.2/Particle Basic.vs", "res/shaders/GLSL 1.2/Particle Basic.fs", true);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		this.particleVBO = GL15.glGenBuffers();
		this.particleCount = particleCount;
		this.particles = new Particle[particleCount];
		FloatBuffer buffer = ByteBuffer.allocateDirect(4 * particleCount * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < particleCount; i++) {
			particles[i] = new Particle(random);

			buffer.put(particles[i].position.getX());
			buffer.put(particles[i].position.getY());
		}
		buffer.flip();

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, particleVBO);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_DYNAMIC_DRAW);

		this.wallVBO = GL15.glGenBuffers();
		this.walls = new ArrayList<Wall>();
		final float WALL_INSET = 0.01f;
		walls.add(new Wall(new Vector2f(WALL_INSET, WALL_INSET), new Vector2f(this.width - WALL_INSET, WALL_INSET)));
		walls.add(new Wall(new Vector2f(this.width - WALL_INSET, WALL_INSET), new Vector2f(this.width - WALL_INSET, this.height - WALL_INSET)));
		walls.add(new Wall(new Vector2f(this.width - WALL_INSET, this.height - WALL_INSET), new Vector2f(WALL_INSET, this.height - WALL_INSET)));
		walls.add(new Wall(new Vector2f(WALL_INSET, this.height - WALL_INSET), new Vector2f(WALL_INSET, WALL_INSET)));

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

		testCollider = new AABBCollider(width / 5 * 2, height / 5 * 2, width / 5, height / 5);
		this.collidingParticleVBO = GL15.glGenBuffers();
		this.collidingParticles = new ArrayList<ICollidable>();

		this.setRenderMap(new HashMap<Integer, ArrayList<IRenderObject>>());

		this.positionBuffer = ByteBuffer.allocateDirect(particleCount * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		this.velocityBuffer = ByteBuffer.allocateDirect(particleCount * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		this.particleCountBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
		this.frameDeltaBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer();

		particleCountBuffer.put(0, particleCount);

		try {
			this.setupOpenCL();
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		try {
			this.testShader = new Shader("res/shaders/glsl 1.2/postprocessing/Gaussian Blur.vs", "res/shaders/glsl 1.2/postprocessing/Gaussian Blur H.fs", true);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		this.framebufferID = GL30.glGenFramebuffers();
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferID);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTextureID);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 512, 512, 0, GL11.GL_RGBA, GL11.GL_INT, (java.nio.ByteBuffer) null);
		GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTextureID, 0);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	private Matrix4f createProjectionMatrix(float left, float right, float bottom, float top, float near, float far) {
		Matrix4f projection = new Matrix4f();

		float xRange = right - left;
		float yRange = top - bottom;
		float zRange = far - near;

		projection.m00 = 2.0f / xRange;
		projection.m01 = 0;
		projection.m02 = 0;
		projection.m03 = 0;

		projection.m10 = 0;
		projection.m11 = 2.0f / yRange;
		projection.m12 = 0;
		projection.m13 = 0;

		projection.m20 = 0;
		projection.m21 = 0;
		projection.m22 = -2.0f / zRange;
		projection.m23 = 0;

		projection.m30 = -((right + left) / xRange);
		projection.m31 = -((top + bottom) / yRange);
		projection.m32 = -((far + near) / zRange);
		projection.m33 = 1;

		return projection;
	}

	private void setupOpenCL() throws LWJGLException, IOException {
		CLPlatform platform = CLPlatform.getPlatforms().get(1);
		List<CLDevice> devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
		this.context = CLContext.create(platform, devices, null, null, null);
		this.queue = CL10.clCreateCommandQueue(context, devices.get(0), CL10.CL_QUEUE_PROFILING_ENABLE, null);

		// Allocate memory for our two input buffers and our result buffer
		this.positionMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, positionBuffer, null);
		this.velocityMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, velocityBuffer, null);
		this.particleCountMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, particleCountBuffer, null);
		this.frameDeltaMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, frameDeltaBuffer, null);
		CL10.clEnqueueWriteBuffer(queue, positionMem, 1, 0, positionBuffer, null, null);
		CL10.clEnqueueWriteBuffer(queue, velocityMem, 1, 0, velocityBuffer, null, null);
		CL10.clEnqueueWriteBuffer(queue, particleCountMem, 1, 0, particleCountBuffer, null, null);
		CL10.clEnqueueWriteBuffer(queue, frameDeltaMem, 1, 0, frameDeltaBuffer, null, null);
		CL10.clFinish(queue);

		// Load the source from a resource file
		String source = UtilCL.getResourceAsString("./res/kernels/Fluid Dynamics.cl");

		// Create our program and kernel
		this.program = CL10.clCreateProgramWithSource(context, source, null);
		if (CL10.clBuildProgram(program, devices.get(0), "", null) != CL10.CL_SUCCESS) {
			throw new OpenCLException(program.getBuildInfoString(devices.get(0), CL10.CL_PROGRAM_BUILD_LOG));
		}
		// sum has to match a kernel method name in the OpenCL source
		this.kernel = CL10.clCreateKernel(program, "main", null);

		this.kernel1DGlobalOffset = BufferUtils.createPointerBuffer(1);
		this.kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(1);
		kernel.setArg(0, positionMem);
		kernel.setArg(1, velocityMem);
		kernel.setArg(2, particleCountMem);
		kernel.setArg(3, frameDeltaMem);
	}

	private void resetParticles() {
		Random random = new Random();
		for (int i = 0; i < particleCount; i++) {
			particles[i] = new Particle(random);
		}
	}

	@Override
	public void finalize() {
		CL10.clReleaseKernel(kernel);
		CL10.clReleaseProgram(program);
		CL10.clReleaseMemObject(positionMem);
		CL10.clReleaseMemObject(velocityMem);
		CL10.clReleaseCommandQueue(queue);
		CL10.clReleaseContext(context);
	}

	/**
	 * <Update Order> 1) Check and or handle for user input 2) Perform acceleration type tasks (i.e. gravity wells, mouse force, etc.) 3) Calculate drag or
	 * basic deceleration of particles 4) Handle collisions and particle movement (combined into one function)
	 * 
	 */
	public void update(float delta) {
		if (InputHandler.isKeyPressed(InputHandler.KEY_R) && (InputHandler.isKeyDown(InputHandler.KEY_LCONTROL) || InputHandler.isKeyDown(InputHandler.KEY_RCONTROL))) {
			this.resetParticles();
		}

		if (InputHandler.isButtonDown(2)) {
			camera.translate(-InputHandler.getMouseDX(), -InputHandler.getMouseDY());

			if (camera.getPosition().getX() < -cameraSize.getX() * 0.9f) {
				camera.setX(-cameraSize.getX() * 0.9f);
			} else if (camera.getPosition().getX() > width - cameraSize.getX() * 0.1f) {
				camera.setX(width - cameraSize.getX() * 0.1f);
			}

			if (camera.getPosition().getY() < -cameraSize.getY() * 0.9f) {
				camera.setY(-cameraSize.getY() * 0.9f);
			} else if (camera.getPosition().getY() > height - cameraSize.getY() * 0.1f) {
				camera.setY(height - cameraSize.getY() * 0.1f);
			}
		} else {
			if (InputHandler.isKeyDown(InputHandler.KEY_DOWN) && !InputHandler.isKeyDown(InputHandler.KEY_UP)) {
				camera.translate(0.0f, -CAMERA_SPEED);
			} else if (InputHandler.isKeyDown(InputHandler.KEY_UP) && !InputHandler.isKeyDown(InputHandler.KEY_DOWN)) {
				camera.translate(0.0f, CAMERA_SPEED);
			}

			if (InputHandler.isKeyDown(InputHandler.KEY_LEFT) && !InputHandler.isKeyDown(InputHandler.KEY_RIGHT)) {
				camera.translate(-CAMERA_SPEED, 0.0f);
			} else if (InputHandler.isKeyDown(InputHandler.KEY_RIGHT) && !InputHandler.isKeyDown(InputHandler.KEY_LEFT)) {
				camera.translate(CAMERA_SPEED, 0.0f);
			}

			if (InputHandler.isKeyDown(InputHandler.KEY_NUMPAD0) && (InputHandler.isKeyDown(InputHandler.KEY_LCONTROL) || InputHandler.isKeyDown(InputHandler.KEY_RCONTROL))) {
				camera.setPosition(cameraStartPosition);
			}
		}

		if (InputHandler.isKeyPressed(InputHandler.KEY_U)) {
			doGlobalGravity = !doGlobalGravity;
		}

		for (Particle particle : particles) {
			particle.update();
		}

		// collidingParticles.clear();
		// for (ICollidable collidable : particles) {
		// collidingParticles.add(collidable);
		// }
		// collidingParticles = quadtree.retrieve(testCollider);
		//
		// // System.out.println(collidingParticles.size());
		// Iterator<ICollidable> collidingParticlesIterator = collidingParticles.iterator();
		// while (collidingParticlesIterator.hasNext()) {
		// ICollidable collidable = collidingParticlesIterator.next();
		// if (!collidable.getCollider().getCollision(testCollider).isColliding()) {
		// collidingParticlesIterator.remove();
		// }
		// }

		// System.out.println(collidingParticles.size());
		// System.out.println();

		ArrayList<IPhysicsTask> physicsTasks = new ArrayList<IPhysicsTask>();

		if (false) {
			quadtree.clear();
			physicsTasks.add(new QuadtreeSetupTask(quadtree));
		}

		if (InputHandler.isButtonDown(0) && !InputHandler.isButtonDown(1)) {
			physicsTasks.add(new MouseForceTask(InputHandler.getMouseX() + camera.getPosition().getX(), InputHandler.getMouseY() + camera.getPosition().getY(), force, threshold));
		} else if (InputHandler.isButtonDown(1) && !InputHandler.isButtonDown(0)) {
			physicsTasks.add(new MouseForceTask(InputHandler.getMouseX() + camera.getPosition().getX(), InputHandler.getMouseY() + camera.getPosition().getY(), -force, threshold));
		}

		if (walls.size() > 0) {
			physicsTasks.add(new EnvironmentCollisionTask(walls));
		}

		if (doGlobalGravity) {
			// physicsTasks.add(new GlobalGravityTask(particles));
		}

		// physicsTasks.add(new ParticleMovementTask(width, height, random));

		// for (PhysicsThread thread : physicsThreads) {
		// thread.syncStartPhysicsObjectNotified = true;
		// synchronized (thread.syncStartPhysicsObject) {
		// thread.syncStartPhysicsObject.notifyAll();
		// }
		// }
		//
		// for (PhysicsThread thread : physicsThreads) {
		// synchronized (thread.syncFinishPhysicsObject) {
		// try {
		// while (!thread.syncFinishPhysicsObjectNotified) {
		// thread.syncFinishPhysicsObject.wait();
		// }
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		//
		// thread.syncFinishPhysicsObjectNotified = false;
		// }
		// }

		for (int i = 0; i < GameManager.THREAD_COUNT; i++) {
			physicsThreads[i] = new PhysicsThread(i, particles, physicsTasks);
			physicsThreads[i].start();
		}

		this.waitForThreads();

		// this.quadtreeCollisionDetection();

		frameDeltaBuffer.put(0, delta);

		this.writeCLBuffers();

		// TODO: Separate this kernel into two parts: global gravity kernel, particle movement kernel
		// if (doGlobalGravity) {
		this.runKernel();
		// }

		this.readCLBuffers();

		CL10.clFinish(queue);

		// for (int i = 0; i < GameManager.THREAD_COUNT; i++) {
		// collisionThreads[i] = new CollisionThread(i, walls, quadtree);
		// collisionThreads[i].start();
		// }
		//
		// try {
		// for (int i = 0; i < GameManager.THREAD_COUNT; i++) {
		// collisionThreads[i].join();
		// }
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
	}

	private void quadtreeCollisionDetection() {
		int totalCollisionTests = 0;
		ArrayList<ICollidable> possibleCollidables = new ArrayList<ICollidable>();

		for (Wall wall : walls) {
			possibleCollidables = quadtree.retrieve(wall);
			totalCollisionTests += possibleCollidables.size();

			for (ICollidable collidable : possibleCollidables) {
				Particle particle = (Particle) collidable;

				if (particle.getVelocityLine2D().intersectsLine(wall.getLine2D())) {
					Vector2f v = particle.velocity;
					Vector2f n = new Vector2f(wall.getNormal().normalize());
					n.setX(Math.abs(n.getX()));
					n.setY(Math.abs(n.getY()));

					float dot = (float) Vector2f.dot(v, n);

					Vector2f r = Vector2f.subtract(v, Vector2f.scale(n, 2.0f * dot)).scale(0.5f);

					particle.velocity.set(r);
				}
			}
		}
		System.out.println(totalCollisionTests);
	}

	private void waitForThreads() {
		try {
			for (int i = 0; i < GameManager.THREAD_COUNT; i++) {
				physicsThreads[i].join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void runKernel() {
		int maxWorkSize = positionBuffer.capacity() / 2;
		for (int i = positionBuffer.capacity() / 2, j = 0; i > 0; i -= maxWorkSize, j += maxWorkSize) {
			kernel1DGlobalWorkSize.put(0, Math.min(i, maxWorkSize));

			kernel1DGlobalOffset.put(0, j);
			CL10.clEnqueueNDRangeKernel(queue, kernel, 1, kernel1DGlobalOffset, kernel1DGlobalWorkSize, null, null, null);
		}
	}

	private void readCLBuffers() {
		CL10.clEnqueueReadBuffer(queue, positionMem, 1, 0, positionBuffer, null, null);
		CL10.clEnqueueReadBuffer(queue, velocityMem, 1, 0, velocityBuffer, null, null);

		for (int i = 0; i < particleCount; i++) {
			particles[i].position.set(positionBuffer.get(i * 2), positionBuffer.get(i * 2 + 1));
			particles[i].velocity.set(velocityBuffer.get(i * 2), velocityBuffer.get(i * 2 + 1));
		}

		positionBuffer.rewind();
		velocityBuffer.rewind();
	}

	private void writeCLBuffers() {
		for (Particle particle : particles) {
			positionBuffer.put(particle.position.getX());
			positionBuffer.put(particle.position.getY());

			velocityBuffer.put(particle.velocity.getX());
			velocityBuffer.put(particle.velocity.getY());
		}

		positionBuffer.rewind();
		velocityBuffer.rewind();

		CL10.clEnqueueWriteBuffer(queue, positionMem, 1, 0, positionBuffer, null, null);
		CL10.clEnqueueWriteBuffer(queue, velocityMem, 1, 0, velocityBuffer, null, null);
		// CL10.clEnqueueWriteBuffer(queue, frameDeltaMem, 1, 0, frameDeltaBuffer, null, null);
	}

	public void render() {
		// HashMap<Integer, ArrayList<IRenderObject>> renderMapInstance = this.getRenderMap();
		// GL11.glTranslatef(-100, -100, 0.0f);
		// FloatBuffer viewProjectionBuffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		// camera.getViewProjectionMatrix().store(viewProjectionBuffer);
		// viewProjectionBuffer.flip();
		// GL11.glMultMatrix(viewProjectionBuffer);

		shader.bindShader();
		shader.setUniform("uViewPojectionMatrix", camera.getViewProjectionMatrix());

		GL11.glDisable(GL11.GL_TEXTURE_2D);

		GL11.glPointSize(1.0f);
		GL11.glLineWidth(1.0f);

		GL11.glColor3f(1.0f, 1.0f, 1.0f);

		GL20.glEnableVertexAttribArray(0);

		/****************************************
		 * Particle rendering section
		 */
		FloatBuffer particleBuffer = ByteBuffer.allocateDirect(particleCount * Particle.SIZE_IN_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < particleCount; i++) {
			particleBuffer.put(particles[i].position.getX());
			particleBuffer.put(particles[i].position.getY());
		}
		particleBuffer.flip();

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, particleVBO);
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, particleBuffer);
		GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * 4, 0);
		GL11.glDrawArrays(GL11.GL_POINTS, 0, particleCount);

		/****************************************
		 * Colliding particle rendering section
		 */
		FloatBuffer collidingParticleBuffer = ByteBuffer.allocateDirect(collidingParticles.size() * Particle.SIZE_IN_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < collidingParticles.size(); i++) {
			collidingParticleBuffer.put(((Particle) collidingParticles.get(i)).position.getX());
			collidingParticleBuffer.put(((Particle) collidingParticles.get(i)).position.getY());
		}
		collidingParticleBuffer.flip();

		GL11.glColor3f(1.0f, 0.0f, 0.0f);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, collidingParticleVBO);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, collidingParticleBuffer, GL15.GL_STREAM_DRAW);
		GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * 4, 0);
		GL11.glDrawArrays(GL11.GL_POINTS, 0, collidingParticles.size());
		GL11.glColor3f(1.0f, 1.0f, 1.0f);

		/****************************************
		 * Wall rendering section
		 */
		int wallCount = walls.size();
		FloatBuffer wallBuffer = ByteBuffer.allocateDirect(wallCount * Wall.SIZE_IN_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (Wall wall : walls) {
			wallBuffer.put(wall.getStart().getX());
			wallBuffer.put(wall.getStart().getY());

			wallBuffer.put(wall.getEnd().getX());
			wallBuffer.put(wall.getEnd().getY());
		}
		wallBuffer.flip();

		GL11.glColor3f(1.0f, 1.0f, 0.0f);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, wallVBO);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, wallBuffer, GL15.GL_STREAM_DRAW);
		GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * 4, 0);
		GL11.glDrawArrays(GL11.GL_LINES, 0, wallCount * 2);
		GL11.glColor3f(1.0f, 1.0f, 1.0f);

		GL20.glDisableVertexAttribArray(0);

		GL11.glPointSize(5.0f);

		GL11.glColor3f(1.0f, 0.0f, 0.0f);

		GL11.glBegin(GL11.GL_POINTS);
		for (Vector2f position : averagePositions) {
			GL11.glVertex2f(position.getX(), position.getY());
		}
		GL11.glEnd();

		GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.5f);

		// GL11.glBegin(GL11.GL_LINE_LOOP);
		// GL11.glVertex2f(testCollider.getX(), testCollider.getY());
		// GL11.glVertex2f(testCollider.getX() + testCollider.getWidth(), testCollider.getY());
		// GL11.glVertex2f(testCollider.getX() + testCollider.getWidth(), testCollider.getY() + testCollider.getHeight());
		// GL11.glVertex2f(testCollider.getX(), testCollider.getY() + testCollider.getHeight());
		// GL11.glEnd();

		ArrayList<Float> quadtreeVertices = quadtree.getVertices();
		// System.out.println("Vertices = " + (quadtreeVertices.size() / 6));

		GL11.glBegin(GL11.GL_LINES);
		for (int i = 0; i < quadtreeVertices.size() / 6; i++) {
			GL11.glColor4f(quadtreeVertices.get(i * 6 + 0) * 0.0f, quadtreeVertices.get(i * 6 + 1) * 1.0f, quadtreeVertices.get(i * 6 + 2) * 0.0f,
					quadtreeVertices.get(i * 6 + 3) * 1.0f);

			GL11.glVertex2f(quadtreeVertices.get(i * 6 + 4), quadtreeVertices.get(i * 6 + 5));
		}
		GL11.glEnd();

		this.drawMouseIndicator();

		GL11.glColor3f(0.0f, 1.0f, 1.0f);
		for (Wall wall : walls) {
			AABBCollider collider = (AABBCollider) wall.getCollider();
			Vector2f min = collider.getMin();
			Vector2f max = collider.getMax();

			GL11.glBegin(GL11.GL_LINE_LOOP);
			GL11.glVertex2f(min.getX(), min.getY());
			GL11.glVertex2f(max.getX(), min.getY());
			GL11.glVertex2f(max.getX(), max.getY());
			GL11.glVertex2f(min.getX(), max.getY());
			GL11.glEnd();
		}

		shader.unbindShader();
	}

	private void drawMouseIndicator() {
		float mx = InputHandler.getMouseX() + camera.getPosition().getX();
		float my = InputHandler.getMouseY() + camera.getPosition().getY();
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		if (InputHandler.isButtonDown(0) && !InputHandler.isButtonDown(1)) {
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
			GL11.glVertex2f(mx, my);
			GL11.glColor4f(0.0f, 0.0f, 1.0f, 0.1f);
		} else if (InputHandler.isButtonDown(1) && !InputHandler.isButtonDown(0)) {
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
			GL11.glVertex2f(mx, my);
			GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.01f);
		} else {
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.25f);
			GL11.glVertex2f(mx, my);
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.0f);
		}
		for (int i = 0; i < 360; i++) {
			GL11.glVertex2f((float) (mx + Math.cos(Math.toRadians(i)) * force), (float) (my + Math.sin(Math.toRadians(i)) * force));
		}
		GL11.glVertex2f(mx + force, my);
		GL11.glEnd();

		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.0f);
		GL11.glVertex2f(mx, my);
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
		for (int i = 0; i < 360; i++) {
			GL11.glVertex2f((float) (mx + Math.cos(Math.toRadians(i)) * threshold), (float) (my + Math.sin(Math.toRadians(i)) * threshold));
		}
		GL11.glVertex2f(mx + threshold, my);
		GL11.glEnd();
	}

	private HashMap<Integer, ArrayList<IRenderObject>> getRenderMap() {
		synchronized (renderMap) {
			HashMap<Integer, ArrayList<IRenderObject>> result = new HashMap<Integer, ArrayList<IRenderObject>>();
			for (Integer key : renderMap.keySet()) {
				ArrayList<IRenderObject> listCopy = new ArrayList<IRenderObject>();
				for (IRenderObject object : renderMap.get(key)) {
					listCopy.add(object);
				}

				result.put(key.intValue(), listCopy);
			}

			return result;
		}
	}

	private void setRenderMap(HashMap<Integer, ArrayList<IRenderObject>> renderMap) {
		synchronized (renderMap) {
			this.renderMap = renderMap;
		}
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}
}
