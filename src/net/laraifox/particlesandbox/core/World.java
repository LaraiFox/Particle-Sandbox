package net.laraifox.particlesandbox.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import net.laraifox.particlesandbox.collision.AABBCollider;
import net.laraifox.particlesandbox.collision.Quadtree;
import net.laraifox.particlesandbox.interfaces.ICollidable;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;
import net.laraifox.particlesandbox.objects.Particle;
import net.laraifox.particlesandbox.objects.ParticleEmitter;
import net.laraifox.particlesandbox.objects.Wall;
import net.laraifox.particlesandbox.opencl.CLFloatBuffer;
import net.laraifox.particlesandbox.opencl.CLIntBuffer;
import net.laraifox.particlesandbox.opencl.Kernel;
import net.laraifox.particlesandbox.physicstasks.CollisionThread;
import net.laraifox.particlesandbox.physicstasks.EnvironmentCollisionTask;
import net.laraifox.particlesandbox.physicstasks.GlobalGravityTask;
import net.laraifox.particlesandbox.physicstasks.MouseForceTask;
import net.laraifox.particlesandbox.physicstasks.PhysicsThread;
import net.laraifox.particlesandbox.physicstasks.QuadtreeSetupTask;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;

public class World {
	public static final float GRAVITATIONAL_CONSTANT = 0.00006673f;
	private static final float CAMERA_SPEED = 1.5f;

	private final float halfWidth, halfHeight;
	private final int maxParticleCount;
	private final int particleCount;
	private final Random random;

	private PhysicsThread[] physicsThreads;
	private CollisionThread[] collisionThreads;

	private CLContext context;
	private CLCommandQueue queue;
	private Kernel globalGravityKernel;
	private Kernel fluidDynamicsKernel;
	private CLFloatBuffer positionBuffer;
	private CLFloatBuffer velocityBuffer;
	private CLIntBuffer particleCountBuffer;
	private CLFloatBuffer frameDeltaBuffer;

	private Vector2f cameraSize;
	private Camera camera;
	private Shader basicShader;
	private int particleVBO;
	private int wallVBO;

	private ArrayList<Particle> particles;
	private ArrayList<ParticleEmitter> particleEmitters;
	private ArrayList<Wall> walls;

	private Quadtree quadtree;

	private float mouseForceStrength;
	private float mouseForceThreshold;

	private boolean doGlobalGravity;

	private DeveloperHUD developerHUD;

	// private int framebufferID;
	// private int colorTextureID;

	private boolean renderGlowingParticles;
	private FrameBuffer frameBuffer1;
	private FrameBuffer frameBuffer2;
	private Shader blurShaderH;
	private Shader blurShaderV;

	public World(float width, float height, int particleCount, Random random) throws IOException, LWJGLException {
		this.halfWidth = width * 1 / 2.0f;
		this.halfHeight = height * 1 / 2.0f;
		this.maxParticleCount = 150000;
		this.particleCount = particleCount;
		this.random = random;

		this.physicsThreads = new PhysicsThread[GameManager.THREAD_COUNT];
		this.collisionThreads = new CollisionThread[GameManager.THREAD_COUNT];

		this.setupOpenCL();

		this.cameraSize = new Vector2f(width / 2.0f, height / 2.0f);
		this.camera = new Camera(createProjectionMatrix(-cameraSize.getX(), cameraSize.getX(), -cameraSize.getY(), cameraSize.getY(), 0, 1), Vector2f.Zero());
		this.basicShader = new Shader("res/shaders/glsl 1.2/Particle Basic.vs", "res/shaders/glsl 1.2/Particle Basic.fs", true);
		this.particleVBO = GL15.glGenBuffers();
		this.wallVBO = GL15.glGenBuffers();

		this.particles = new ArrayList<Particle>();
		this.particleEmitters = new ArrayList<ParticleEmitter>();
		this.walls = new ArrayList<Wall>();

		float quadtreeDepth = this.halfWidth;
		if (this.halfHeight > this.halfWidth)
			quadtreeDepth = this.halfHeight;
		quadtreeDepth = (float) Math.ceil(Math.sqrt((quadtreeDepth / 100.0f)));
		this.quadtree = new Quadtree(new AABBCollider(-halfWidth, -halfHeight, halfWidth * 2.0f, halfHeight * 2.0f), 100, 100);

		this.mouseForceStrength = 60.0f;
		this.mouseForceThreshold = 20.0f;

		this.developerHUD = new DeveloperHUD();

		FloatBuffer buffer = ByteBuffer.allocateDirect(4 * maxParticleCount * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < particleCount; i++) {
			this.addParticle(new Particle(halfWidth, halfHeight, random));

			buffer.put(particles.get(i).position.getX());
			buffer.put(particles.get(i).position.getY());
		}
		buffer.flip();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, particleVBO);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_DYNAMIC_DRAW);

		final float WALL_INSET = 0.01f;
		walls.add(new Wall(new Vector2f(WALL_INSET - halfWidth, WALL_INSET - halfHeight), new Vector2f(halfWidth - WALL_INSET, WALL_INSET - halfHeight)));
		walls.add(new Wall(new Vector2f(halfWidth - WALL_INSET, WALL_INSET - halfHeight), new Vector2f(halfWidth - WALL_INSET, halfHeight - WALL_INSET)));
		walls.add(new Wall(new Vector2f(halfWidth - WALL_INSET, halfHeight - WALL_INSET), new Vector2f(WALL_INSET - halfWidth, halfHeight - WALL_INSET)));
		walls.add(new Wall(new Vector2f(WALL_INSET - halfWidth, halfHeight - WALL_INSET), new Vector2f(WALL_INSET - halfWidth, WALL_INSET - halfHeight)));

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		this.frameBuffer1 = new FrameBuffer((int) width, (int) height);
		this.frameBuffer2 = new FrameBuffer((int) width, (int) height);
		try {
			this.blurShaderH = new Shader("res/shaders/glsl 1.2/postprocessing/Gaussian Blur.vs", "res/shaders/glsl 1.2/postprocessing/Gaussian Blur H.fs", true);
			this.blurShaderV = new Shader("res/shaders/glsl 1.2/postprocessing/Gaussian Blur.vs", "res/shaders/glsl 1.2/postprocessing/Gaussian Blur V.fs", true);
			System.out.println("SUCCESS!");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// this.framebufferID = GL30.glGenFramebuffers();
		// GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferID);
		// GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTextureID);
		// GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		// GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 512, 512, 0, GL11.GL_RGBA, GL11.GL_INT, (java.nio.ByteBuffer) null);
		// GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTextureID, 0);
		// GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
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
		CLPlatform platform = CLPlatform.getPlatforms().get(0);
		List<CLDevice> devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
		this.context = CLContext.create(platform, devices, null, null, null);
		this.queue = CL10.clCreateCommandQueue(context, devices.get(0), CL10.CL_QUEUE_PROFILING_ENABLE, null);

		this.globalGravityKernel = new Kernel(context, devices.get(0), "res/kernels/Global Gravity.cl", 1, null, BufferUtils.createPointerBuffer(1), null);
		this.fluidDynamicsKernel = new Kernel(context, devices.get(0), "res/kernels/Fluid Dynamics.cl", 1, null, BufferUtils.createPointerBuffer(1), null);
		this.positionBuffer = new CLFloatBuffer(maxParticleCount * 2, context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, null);
		this.velocityBuffer = new CLFloatBuffer(maxParticleCount * 2, context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, null);
		this.particleCountBuffer = new CLIntBuffer(1, context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, null);
		this.frameDeltaBuffer = new CLFloatBuffer(1, context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, null);
		positionBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);
		velocityBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);
		particleCountBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);
		frameDeltaBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);
		globalGravityKernel.setArg(0, positionBuffer);
		globalGravityKernel.setArg(1, velocityBuffer);
		globalGravityKernel.setArg(2, particleCountBuffer);
		globalGravityKernel.setArg(3, frameDeltaBuffer);

		fluidDynamicsKernel.setArg(0, positionBuffer);
		fluidDynamicsKernel.setArg(1, velocityBuffer);
		fluidDynamicsKernel.setArg(2, particleCountBuffer);
		fluidDynamicsKernel.setArg(3, frameDeltaBuffer);
		CL10.clFinish(queue);
	}

	private void resetParticles() {
		particles.clear();

		HashSet<Vector2f> usedLocations = new HashSet<Vector2f>();
		for (int i = 0; i < particleCount; i++) {
			Vector2f position = null;
			do {
				position = new Vector2f((random.nextFloat() * 2.0f - 1.0f) * halfWidth, (random.nextFloat() * 2.0f - 1.0f) * halfHeight);
			} while (usedLocations.contains(position));

			usedLocations.add(position);

			this.addParticle(new Particle(position, Vector2f.Zero()));
		}
	}

	@Override
	public void finalize() {
		CL10.clReleaseCommandQueue(queue);
		CL10.clReleaseContext(context);
	}

	/**
	 * <Update Order> 1) Check and or handle for user input 2) Perform acceleration type tasks (i.e. gravity wells, mouse force, etc.) 3) Calculate drag or
	 * basic deceleration of particles 4) Handle collisions and particle movement (combined into one function)
	 * 
	 */
	public void update(float delta) {
		developerHUD.update();

		if (InputHandler.isKeyPressed(InputHandler.KEY_R) && (InputHandler.isKeyDown(InputHandler.KEY_LCONTROL) || InputHandler.isKeyDown(InputHandler.KEY_RCONTROL))) {
			this.resetParticles();
		}

		if (InputHandler.isButtonDown(2)) {
			camera.translate(-InputHandler.getMouseDX(), -InputHandler.getMouseDY());

			if (camera.getPosition().getX() < -halfWidth - cameraSize.getX() * 0.9f) {
				camera.setX(-halfWidth - cameraSize.getX() * 0.9f);
			} else if (camera.getPosition().getX() > halfWidth + cameraSize.getX() * 0.9f) {
				camera.setX(halfWidth + cameraSize.getX() * 0.9f);
			}

			if (camera.getPosition().getY() < -halfHeight - cameraSize.getY() * 0.9f) {
				camera.setY(-halfHeight - cameraSize.getY() * 0.9f);
			} else if (camera.getPosition().getY() > halfHeight + cameraSize.getY() * 0.9f) {
				camera.setY(halfHeight + cameraSize.getY() * 0.9f);
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
				camera.setPosition(Vector2f.Zero());
			}
		}

		if (InputHandler.isKeyPressed(InputHandler.KEY_E)) {
			particleEmitters.add(new ParticleEmitter(new Transform2D(Vector2f.add(InputHandler.getMousePosition(), camera.getPosition()).subtract(cameraSize)), 1.0f, 5.0f, 5.0f,
					this, random));
		}

		if (InputHandler.isKeyPressed(InputHandler.KEY_U)) {
			doGlobalGravity = !doGlobalGravity;
		}

		if (InputHandler.isKeyPressed(InputHandler.KEY_F5)) {
			renderGlowingParticles = !renderGlowingParticles;
		}

		for (ParticleEmitter emitter : particleEmitters) {
			emitter.update(delta);
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
			physicsTasks.add(new MouseForceTask(InputHandler.getMouseX() + camera.getPosition().getX() - cameraSize.getX(), InputHandler.getMouseY() + camera.getPosition().getY()
				- cameraSize.getY(), mouseForceStrength, mouseForceThreshold));
		} else if (InputHandler.isButtonDown(1) && !InputHandler.isButtonDown(0)) {
			physicsTasks.add(new MouseForceTask(InputHandler.getMouseX() + camera.getPosition().getX() - cameraSize.getX(), InputHandler.getMouseY() + camera.getPosition().getY()
				- cameraSize.getY(), -mouseForceStrength, mouseForceThreshold));
		}

		if (walls.size() > 0) {
			physicsTasks.add(new EnvironmentCollisionTask(walls));
		}

		if (doGlobalGravity) {
			physicsTasks.add(new GlobalGravityTask(particles));
		}

		// physicsTasks.add(new ParticleMovementTask(width, height, random));

		for (int i = 0; i < GameManager.THREAD_COUNT; i++) {
			physicsThreads[i] = new PhysicsThread(i, particles, physicsTasks);
			physicsThreads[i].start();
		}

		this.waitForThreads();

		// this.quadtreeCollisionDetection();

		frameDeltaBuffer.put(0, delta);

		if (particles.size() > 0) {
			this.writeCLBuffers();
			this.runKernel();
			this.readCLBuffers();
		}

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
		// globalGravityKernel.setGlobalWorkSize(0, particles.size());
		// globalGravityKernel.enqueueNDRangeKernel(queue, null, null);

		CL10.clFinish(queue);

		fluidDynamicsKernel.setGlobalWorkSize(0, particles.size());
		fluidDynamicsKernel.enqueueNDRangeKernel(queue, null, null);
	}

	private void readCLBuffers() {
		positionBuffer.enqueueReadBuffer(queue, 1, 0, null, null);
		velocityBuffer.enqueueReadBuffer(queue, 1, 0, null, null);

		for (int i = 0; i < particles.size(); i++) {
			particles.get(i).position.set(positionBuffer.get(i * 2), positionBuffer.get(i * 2 + 1));
			particles.get(i).velocity.set(velocityBuffer.get(i * 2), velocityBuffer.get(i * 2 + 1));
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

		particleCountBuffer.put(0, particles.size());

		positionBuffer.rewind();
		velocityBuffer.rewind();

		positionBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);
		velocityBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);
		particleCountBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);
	}

	public void render() {
		basicShader.setUniform("uViewPojectionMatrix", camera.getViewProjectionMatrix());

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		if (renderGlowingParticles) {
			this.renderParticleGlow();
		}

		this.renderScene();

		this.drawQuadtreeOutline();

		// this.drawMouseIndicator();
	}

	private void renderParticleGlow() {
		GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		frameBuffer1.bindFrameBuffer();
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

		this.prepareSceneRender();

		this.renderParticles();

		this.finishSceneRender();

		GL11.glEnable(GL11.GL_TEXTURE_2D);

		frameBuffer2.bindFrameBuffer();
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

		blurShaderH.bindShader();

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, frameBuffer1.getTextureID());
		this.renderTexturedQuad();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

		frameBuffer2.unbindCurrentFrameBuffer();

		blurShaderV.bindShader();

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, frameBuffer2.getTextureID());
		this.renderTexturedQuad();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}

	private void renderScene() {
		this.prepareSceneRender();

		this.renderParticles();
		this.renderWalls();

		this.finishSceneRender();
	}

	private void prepareSceneRender() {
		GL11.glDisable(GL11.GL_TEXTURE_2D);

		GL11.glPointSize(1.0f);
		GL11.glLineWidth(1.0f);

		GL11.glColor3f(1.0f, 1.0f, 1.0f);

		GL20.glEnableVertexAttribArray(0);

		basicShader.bindShader();
	}

	private void renderParticles() {
		/****************************************
		 * Particle rendering section
		 */
		FloatBuffer particleBuffer = ByteBuffer.allocateDirect(particles.size() * Particle.SIZE_IN_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		for (int i = 0; i < particles.size(); i++) {
			particleBuffer.put(particles.get(i).position.getX());
			particleBuffer.put(particles.get(i).position.getY());
		}
		particleBuffer.flip();

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, particleVBO);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, particleBuffer, GL15.GL_DYNAMIC_DRAW);
		GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * 4, 0);
		GL11.glDrawArrays(GL11.GL_POINTS, 0, particles.size());
	}

	private void renderWalls() {
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
	}

	private void renderTexturedQuad() {
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glTexCoord2f(0.0f, 0.0f);
		GL11.glVertex2f(-1.0f, -1.0f);
		GL11.glTexCoord2f(1.0f, 0.0f);
		GL11.glVertex2f(1.0f, -1.0f);
		GL11.glTexCoord2f(1.0f, 1.0f);
		GL11.glVertex2f(1.0f, 1.0f);
		GL11.glTexCoord2f(0.0f, 1.0f);
		GL11.glVertex2f(-1.0f, 1.0f);
		GL11.glEnd();
	}

	private void finishSceneRender() {
		GL20.glDisableVertexAttribArray(0);
	}

	private void drawQuadtreeOutline() {
		ArrayList<Float> quadtreeVertices = quadtree.getVertices();
		GL11.glBegin(GL11.GL_LINES);
		for (int i = 0; i < quadtreeVertices.size() / 6; i++) {
			GL11.glColor4f(quadtreeVertices.get(i * 6 + 0) * 0.0f, quadtreeVertices.get(i * 6 + 1) * 1.0f, quadtreeVertices.get(i * 6 + 2) * 0.0f,
					quadtreeVertices.get(i * 6 + 3) * 1.0f);
			GL11.glVertex2f(quadtreeVertices.get(i * 6 + 4), quadtreeVertices.get(i * 6 + 5));
		}
		GL11.glEnd();
	}

	private void drawMouseIndicator() {
		float mx = InputHandler.getMouseX() + camera.getPosition().getX() - cameraSize.getX();
		float my = InputHandler.getMouseY() + camera.getPosition().getY() - cameraSize.getY();
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
			GL11.glVertex2f((float) (mx + Math.cos(Math.toRadians(i)) * mouseForceStrength), (float) (my + Math.sin(Math.toRadians(i)) * mouseForceStrength));
		}
		GL11.glVertex2f(mx + mouseForceStrength, my);
		GL11.glEnd();

		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.0f);
		GL11.glVertex2f(mx, my);
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
		for (int i = 0; i < 360; i++) {
			GL11.glVertex2f((float) (mx + Math.cos(Math.toRadians(i)) * mouseForceThreshold), (float) (my + Math.sin(Math.toRadians(i)) * mouseForceThreshold));
		}
		GL11.glVertex2f(mx + mouseForceThreshold, my);
		GL11.glEnd();
	}

	public float getWidth() {
		return halfWidth;
	}

	public float getHeight() {
		return halfHeight;
	}

	public void addParticle(Particle particle) {
		if (particles.size() < maxParticleCount) {
			this.particles.add(particle);
		}
	}
}
