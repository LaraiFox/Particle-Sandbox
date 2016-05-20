package net.laraifox.particlesandbox.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;

import net.laraifox.particlesandbox.collision.AABBCollider;
import net.laraifox.particlesandbox.collision.Quadtree;
import net.laraifox.particlesandbox.interfaces.ICollidable;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;
import net.laraifox.particlesandbox.objects.GravityWell;
import net.laraifox.particlesandbox.objects.Particle;
import net.laraifox.particlesandbox.objects.ParticleEmitter;
import net.laraifox.particlesandbox.objects.Wall;
import net.laraifox.particlesandbox.opencl.CLFloatBuffer;
import net.laraifox.particlesandbox.opencl.CLIntBuffer;
import net.laraifox.particlesandbox.opencl.Kernel;
import net.laraifox.particlesandbox.physicstasks.CollisionThread;
import net.laraifox.particlesandbox.physicstasks.PhysicsThread;
import net.laraifox.particlesandbox.physicstasks.QuadtreeSetupTask;

public class World {
	public static final float GRAVITATIONAL_CONSTANT = 0.00006673f;
	private static final float CAMERA_SPEED = 1.5f;

	private final float halfWidth, halfHeight;
	private final int maxParticleCount;
	private final int maxGravityWellCount;
	private final int particleCount;
	private final Random random;

	private PhysicsThread[] physicsThreads;
	private CollisionThread[] collisionThreads;

	private CLContext context;
	private CLCommandQueue queue;
	private Kernel particleAccelerationKernel;
	private Kernel globalGravityKernel;
	private Kernel fluidDynamicsKernel;
	private CLFloatBuffer positionBuffer;
	private CLFloatBuffer velocityBuffer;
	private CLIntBuffer particleCountBuffer;
	private CLFloatBuffer frameDeltaBuffer;
	private CLFloatBuffer mouseForceBuffer;
	private CLFloatBuffer gravityWellBuffer;

	private Vector2f cameraSize;
	private Camera camera;
	private Shader basicShader;
	private int particleVBO;
	private int particleTrailVBO;
	private int wallVBO;

	private ArrayList<Particle> particles;
	private ArrayList<ParticleEmitter> particleEmitters;
	private ArrayList<GravityWell> gravityWells;
	private ArrayList<Wall> walls;

	private Quadtree quadtree;

	private Vector2f mouseForcePosition;
	private float mouseForceStrength;
	private float mouseForceThreshold;
	private float mouseForceLimit;

	private boolean doGlobalGravity;

	private DeveloperHUD developerHUD;

	// private int framebufferID;
	// private int colorTextureID;

	private boolean renderParticleTrails;
	private FrameBuffer[] particleTrailFBOs;
	private int currentTrailBufferID;
	private Shader textureBasicShader;
	private boolean renderGlowingParticles;
	private FrameBuffer frameBuffer1;
	private FrameBuffer frameBuffer2;
	private Shader blurShaderH;
	private Shader blurShaderV;

	public World(float width, float height, Random random) throws IOException, LWJGLException {
		this.halfWidth = Configuration.getInteger(EnumConfigKey.WORLD_WIDTH) / 2.0f;
		this.halfHeight = Configuration.getInteger(EnumConfigKey.WORLD_HEIGHT) / 2.0f;
		this.maxParticleCount = Configuration.getInteger(EnumConfigKey.MAX_PARTICLE_COUNT);
		this.maxGravityWellCount = Configuration.getInteger(EnumConfigKey.MAX_GRAVITY_WELL_COUNT);
		this.particleCount = Configuration.getInteger(EnumConfigKey.INITIAL_PARTICLE_COUNT);
		this.random = random;

		this.physicsThreads = new PhysicsThread[GameManager.THREAD_COUNT];
		this.collisionThreads = new CollisionThread[GameManager.THREAD_COUNT];

		this.setupOpenCL();

		this.cameraSize = new Vector2f(width / 2.0f, height / 2.0f);
		this.camera = new Camera(createProjectionMatrix(-cameraSize.getX(), cameraSize.getX(), -cameraSize.getY(), cameraSize.getY(), 0, 1), Vector2f.Zero());
		this.basicShader = new Shader("res/shaders/glsl 1.2/Particle Basic.vs", "res/shaders/glsl 1.2/Particle Basic.fs", true);
		this.particleVBO = GL15.glGenBuffers();
		this.particleTrailVBO = GL15.glGenBuffers();
		this.wallVBO = GL15.glGenBuffers();

		this.particles = new ArrayList<Particle>();
		this.particleEmitters = new ArrayList<ParticleEmitter>();
		this.gravityWells = new ArrayList<GravityWell>();
		this.walls = new ArrayList<Wall>();

		float quadtreeDepth = this.halfWidth;
		if (this.halfHeight > this.halfWidth)
			quadtreeDepth = this.halfHeight;
		quadtreeDepth = (float) Math.ceil(Math.sqrt((quadtreeDepth / 100.0f)));
		this.quadtree = new Quadtree(new AABBCollider(-halfWidth, -halfHeight, halfWidth * 2.0f, halfHeight * 2.0f), 100, 100);

		this.mouseForcePosition = null;
		this.mouseForceStrength = 60.0f;
		this.mouseForceThreshold = 20.0f;
		this.mouseForceLimit = 0.0f;

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

		this.renderParticleTrails = true;
		this.particleTrailFBOs = new FrameBuffer[Particle.PARTICLE_TRAIL_LENGTH];
		for (int i = 0; i < particleTrailFBOs.length; i++) {
			this.particleTrailFBOs[i] = new FrameBuffer((int) width, (int) height);
		}
		this.currentTrailBufferID = 0;
		this.textureBasicShader = new Shader("res/shaders/glsl 1.2/Texture Basic.vs", "res/shaders/glsl 1.2/Texture Basic.fs", true);

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		this.frameBuffer1 = new FrameBuffer((int) (width / 1.5f), (int) (height / 1.5f));
		this.frameBuffer2 = new FrameBuffer((int) (width / 1.5f), (int) (height / 1.5f));
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

		this.particleAccelerationKernel = new Kernel(context, devices.get(0), "res/kernels/Particle Acceleration.cl", 1, null, BufferUtils.createPointerBuffer(1), null);
		this.globalGravityKernel = new Kernel(context, devices.get(0), "res/kernels/Global Gravity.cl", 1, null, BufferUtils.createPointerBuffer(1), null);
		this.fluidDynamicsKernel = new Kernel(context, devices.get(0), "res/kernels/Fluid Dynamics.cl", 1, null, BufferUtils.createPointerBuffer(1), null);

		this.positionBuffer = new CLFloatBuffer(maxParticleCount * 2, context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, null);
		this.velocityBuffer = new CLFloatBuffer(maxParticleCount * 2, context, CL10.CL_MEM_READ_WRITE | CL10.CL_MEM_COPY_HOST_PTR, null);
		this.particleCountBuffer = new CLIntBuffer(1, context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, null);
		this.frameDeltaBuffer = new CLFloatBuffer(1, context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, null);
		this.mouseForceBuffer = new CLFloatBuffer(5, context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, null);
		this.gravityWellBuffer = new CLFloatBuffer(maxGravityWellCount * 5 + 1, context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR, null);

		particleAccelerationKernel.setArg(0, positionBuffer);
		particleAccelerationKernel.setArg(1, velocityBuffer);
		particleAccelerationKernel.setArg(2, mouseForceBuffer);
		particleAccelerationKernel.setArg(3, gravityWellBuffer);

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

			this.addParticle(new Particle(position, Vector2f.Zero(), random));
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
		if (Display.wasResized()) {
			cameraSize.set(Display.getWidth() / 2.0f, Display.getHeight() / 2.0f);
			camera.setProjectionMatrix(this.createProjectionMatrix(-cameraSize.getX(), cameraSize.getX(), -cameraSize.getY(), cameraSize.getY(), 0, 1));
		}

		developerHUD.update();

		if (InputHandler.isKeyPressed(InputHandler.KEY_R) && (InputHandler.isKeyDown(InputHandler.KEY_LCONTROL) || InputHandler.isKeyDown(InputHandler.KEY_RCONTROL))) {
			this.resetParticles();
		}

		if (InputHandler.isButtonDown(2)) {
			camera.translate(-InputHandler.getMouseDX(), -InputHandler.getMouseDY());

			// if (camera.getPosition().getX() < -halfWidth - cameraSize.getX() * 0.9f) {
			// camera.setX(-halfWidth - cameraSize.getX() * 0.9f);
			// } else if (camera.getPosition().getX() > halfWidth + cameraSize.getX() * 0.9f) {
			// camera.setX(halfWidth + cameraSize.getX() * 0.9f);
			// }
			//
			// if (camera.getPosition().getY() < -halfHeight - cameraSize.getY() * 0.9f) {
			// camera.setY(-halfHeight - cameraSize.getY() * 0.9f);
			// } else if (camera.getPosition().getY() > halfHeight + cameraSize.getY() * 0.9f) {
			// camera.setY(halfHeight + cameraSize.getY() * 0.9f);
			// }
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

		if (InputHandler.isKeyPressed(InputHandler.KEY_G)) {
			if (InputHandler.isKeyDown(InputHandler.KEY_LCONTROL) || InputHandler.isKeyDown(InputHandler.KEY_RCONTROL)) {
				// Toggle world gravity
			} else if (InputHandler.isKeyDown(InputHandler.KEY_LMENU) || InputHandler.isKeyDown(InputHandler.KEY_RMENU)) {
				if (InputHandler.isKeyDown(InputHandler.KEY_LSHIFT) || InputHandler.isKeyDown(InputHandler.KEY_RSHIFT)) {
					gravityWells.clear();
				} else {
					Iterator<GravityWell> iterator = gravityWells.iterator();
					while (iterator.hasNext()) {
						GravityWell gravityWell = iterator.next();

						if (Vector2f.distanceBetween(Vector2f.add(InputHandler.getMousePosition(), camera.getPosition()).subtract(cameraSize), gravityWell.position) < 20.0f) {
							iterator.remove();
							break;
						}
					}
				}
			} else {
				gravityWells.add(new GravityWell(Vector2f.add(InputHandler.getMousePosition(), camera.getPosition()).subtract(cameraSize), Vector2f.Zero(), 30.0f, 60.0f, 0.0f,
						0.9999f));
			}
		}

		if (InputHandler.isKeyPressed(InputHandler.KEY_U)) {
			doGlobalGravity = !doGlobalGravity;
		}

		if (InputHandler.isKeyPressed(InputHandler.KEY_F5)) {
			renderGlowingParticles = !renderGlowingParticles;
		}

		if (InputHandler.isKeyPressed(InputHandler.KEY_F6)) {
			renderParticleTrails = !renderParticleTrails;
			if (renderParticleTrails) {
				GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
				for (int i = 0; i < particleTrailFBOs.length; i++) {
					particleTrailFBOs[i].bindFrameBuffer();
					GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
				}
				FrameBuffer.unbindFrameBuffer();
			}
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
			if (InputHandler.isKeyDown(InputHandler.KEY_LSHIFT) || InputHandler.isKeyDown(InputHandler.KEY_RSHIFT)) {
				for (GravityWell gravityWell : gravityWells) {
					Vector2f vecToMouse = Vector2f.subtract(Vector2f.add(InputHandler.getMousePosition(), camera.getPosition()).subtract(cameraSize), gravityWell.position);

					float distance = vecToMouse.length();
					if (distance != 0.0f) {
						if (distance < gravityWell.threshold) {
							distance = gravityWell.threshold;
						}

						Vector2f acceleration = vecToMouse.normalize().scale(60.0f / distance);

						gravityWell.velocity = Vector2f.add(gravityWell.velocity, acceleration);
					}
				}
			} else {
				mouseForcePosition = Vector2f.add(InputHandler.getMousePosition(), camera.getPosition()).subtract(cameraSize);
				mouseForceStrength = 60.0f;
				// physicsTasks.add(new MouseForceTask(InputHandler.getMouseX() + camera.getPosition().getX() - cameraSize.getX(),
				// InputHandler.getMouseY() + camera.getPosition().getY() - cameraSize.getY(), mouseForceStrength, mouseForceThreshold));
			}
		} else if (InputHandler.isButtonDown(1) && !InputHandler.isButtonDown(0)) {
			if (InputHandler.isKeyDown(InputHandler.KEY_LSHIFT) || InputHandler.isKeyDown(InputHandler.KEY_RSHIFT)) {
				for (GravityWell gravityWell : gravityWells) {
					Vector2f vecToMouse = Vector2f.subtract(Vector2f.add(InputHandler.getMousePosition(), camera.getPosition()).subtract(cameraSize), gravityWell.position);

					float distance = vecToMouse.length();
					if (distance != 0.0f) {
						if (distance < gravityWell.threshold) {
							distance = gravityWell.threshold;
						}

						Vector2f acceleration = vecToMouse.normalize().scale(-60.0f / distance);

						gravityWell.velocity = Vector2f.add(gravityWell.velocity, acceleration);
					}
				}
			} else {
				mouseForcePosition = Vector2f.add(InputHandler.getMousePosition(), camera.getPosition()).subtract(cameraSize);
				mouseForceStrength = -60.0f;
				// physicsTasks.add(new MouseForceTask(InputHandler.getMouseX() + camera.getPosition().getX() - cameraSize.getX(),
				// InputHandler.getMouseY() + camera.getPosition().getY() - cameraSize.getY(), -mouseForceStrength, mouseForceThreshold));
			}
		} else {
			mouseForcePosition = null;
		}

		if (walls.size() > 0) {
			// physicsTasks.add(new EnvironmentCollisionTask(walls));
		}

		// if (doGlobalGravity) {
		// physicsTasks.add(new GlobalGravityTask(particles));
		// }

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

			CL10.clFinish(queue);
		}

		for (GravityWell gravityWell : gravityWells) {
			gravityWell.position.add(gravityWell.velocity);
			gravityWell.velocity.scale(gravityWell.veloctiyScalar);
		}

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
//		System.err.println("UPDATE!");
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
		particleAccelerationKernel.setGlobalWorkSize(0, particles.size());
		particleAccelerationKernel.enqueueNDRangeKernel(queue, null, null);

		if (doGlobalGravity) {
			globalGravityKernel.setGlobalWorkSize(0, particles.size());
			globalGravityKernel.enqueueNDRangeKernel(queue, null, null);
		}

		fluidDynamicsKernel.setGlobalWorkSize(0, particles.size());
		fluidDynamicsKernel.enqueueNDRangeKernel(queue, null, null);
	}

	private void readCLBuffers() {
		positionBuffer.enqueueReadBuffer(queue, 1, 0, null, null);
		velocityBuffer.enqueueReadBuffer(queue, 1, 0, null, null);

		this.readParticleBuffer();

		positionBuffer.rewind();
		velocityBuffer.rewind();
	}

	private void readParticleBuffer() {
		for (int i = 0; i < particles.size(); i++) {
			particles.get(i).position.set(positionBuffer.get(i * 2), positionBuffer.get(i * 2 + 1));
			particles.get(i).velocity.set(velocityBuffer.get(i * 2), velocityBuffer.get(i * 2 + 1));
		}
	}

	private void writeCLBuffers() {
		this.writeParticleBuffer();

		particleCountBuffer.put(0, particles.size());

		if (mouseForcePosition == null) {
			mouseForceBuffer.put(0, 0.0f);
			mouseForceBuffer.put(1, 0.0f);
			mouseForceBuffer.put(2, 0.0f);
		} else {
			mouseForceBuffer.put(0, mouseForcePosition.getX());
			mouseForceBuffer.put(1, mouseForcePosition.getY());
			mouseForceBuffer.put(2, mouseForceStrength);
		}
		mouseForceBuffer.put(3, mouseForceThreshold);
		mouseForceBuffer.put(4, mouseForceLimit);

		gravityWellBuffer.put(0, gravityWells.size());
		for (int i = 0; i < gravityWells.size(); i++) {
			gravityWellBuffer.put(1 + i * 5, gravityWells.get(i).position.getX());
			gravityWellBuffer.put(2 + i * 5, gravityWells.get(i).position.getY());
			gravityWellBuffer.put(3 + i * 5, gravityWells.get(i).strength);
			gravityWellBuffer.put(4 + i * 5, gravityWells.get(i).threshold);
			gravityWellBuffer.put(5 + i * 5, gravityWells.get(i).limit);
		}

		positionBuffer.rewind();
		velocityBuffer.rewind();

		positionBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);
		velocityBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);
		particleCountBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);

		mouseForceBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);
		gravityWellBuffer.enqueueWriteBuffer(queue, 1, 0, null, null);
	}

	private void writeParticleBuffer() {
		for (Particle particle : particles) {
			positionBuffer.put(particle.position.getX());
			positionBuffer.put(particle.position.getY());

			velocityBuffer.put(particle.velocity.getX());
			velocityBuffer.put(particle.velocity.getY());
		}
	}

	public void render() {
//		System.err.println("RENDER!");
		
		basicShader.setUniform("uViewPojectionMatrix", camera.getViewProjectionMatrix());

		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		if (renderGlowingParticles) {
			this.renderParticleGlow();

			GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
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

		FrameBuffer.unbindFrameBuffer();

		blurShaderV.bindShader();

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, frameBuffer2.getTextureID());
		this.renderTexturedQuad();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
	}

	private void renderScene() {
		this.prepareSceneRender();

		this.renderParticles();
		this.renderWalls();
		this.renderGravityWells();

		this.finishSceneRender();
	}

	private void renderGravityWells() {
		final float INNER_RADIUS = 10.0f;
		final float OUTER_RADIUS = 30.0f;

		for (GravityWell gravityWell : gravityWells) {
			float xPos = gravityWell.position.getX();
			float yPos = gravityWell.position.getY();

			GL11.glColor3f(0.0f, 1.0f, 1.0f);
			GL11.glLineWidth(3.0f);
			GL11.glBegin(GL11.GL_LINE_LOOP);
			for (int i = 0; i < 360; i++) {
				GL11.glVertex2f((float) (xPos + Math.cos(Math.toRadians(i)) * INNER_RADIUS), (float) (yPos + Math.sin(Math.toRadians(i)) * INNER_RADIUS));
			}
			GL11.glEnd();

			GL11.glBegin(GL11.GL_TRIANGLE_FAN);
			GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.9f);
			GL11.glVertex2f(xPos, yPos);
			for (int i = 0; i < 360; i++) {
				GL11.glVertex2f((float) (xPos + Math.cos(Math.toRadians(i)) * INNER_RADIUS), (float) (yPos + Math.sin(Math.toRadians(i)) * INNER_RADIUS));
			}
			GL11.glVertex2f(xPos + INNER_RADIUS, yPos);
			GL11.glEnd();

			GL11.glBegin(GL11.GL_TRIANGLE_FAN);
			GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
			GL11.glVertex2f(xPos, yPos);
			GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.0f);
			for (int i = 0; i < 360; i++) {
				GL11.glVertex2f((float) (xPos + Math.cos(Math.toRadians(i)) * OUTER_RADIUS), (float) (yPos + Math.sin(Math.toRadians(i)) * OUTER_RADIUS));
			}
			GL11.glVertex2f(xPos + OUTER_RADIUS, yPos);
			GL11.glEnd();
		}
	}

	private void prepareSceneRender() {
		GL11.glDisable(GL11.GL_TEXTURE_2D);

		GL11.glPointSize(1.0f);
		GL11.glLineWidth(1.0f);

		GL11.glColor3f(1.0f, 1.0f, 1.0f);

		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);

		basicShader.bindShader();
	}

	private void renderParticles() {
		/****************************************
		 * Particle rendering section
		 */
		// TODO: Consider using a framebuffer array to render particle trails... May be more efficient for greater particle counts than storing positions.
		if (renderParticleTrails && particleTrailFBOs.length > 0) {
			GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			particleTrailFBOs[currentTrailBufferID].bindFrameBuffer();
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

			int particleVertexByteCount = Particle.SIZE_IN_BYTES + Float.BYTES;

			FloatBuffer particleBuffer = ByteBuffer.allocateDirect(particles.size() * particleVertexByteCount * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
			for (Particle particle : particles) {
				particleBuffer.put(particle.position.getX());
				particleBuffer.put(particle.position.getY());
				// particleBuffer.put(particle.color);
				particleBuffer.put(1.0f);

				Vector2f previousPosition = Vector2f.subtract(particle.position, particle.velocity).subtract(particle.velocity);
				particleBuffer.put(previousPosition.getX());
				particleBuffer.put(previousPosition.getY());
				// particleBuffer.put(particle.color);
				particleBuffer.put(1.0f - (1.0f / Particle.PARTICLE_TRAIL_LENGTH));
			}
			particleBuffer.flip();

			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, particleVBO);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, particleBuffer, GL15.GL_STREAM_DRAW);
			GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, particleVertexByteCount, 0);
			GL20.glVertexAttribPointer(1, 1, GL11.GL_FLOAT, false, particleVertexByteCount, 2 * Float.BYTES);
			GL11.glDrawArrays(GL11.GL_LINES, 0, particles.size() * 2);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

			// if (renderGlowingParticles) {
			// frameBuffer1.bindFrameBuffer();
			// } else {
			FrameBuffer.unbindFrameBuffer();
			// }

			GL11.glEnable(GL11.GL_TEXTURE_2D);
			textureBasicShader.bindShader();
			for (int i = 0; i <= particleTrailFBOs.length; i++) {
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, particleTrailFBOs[(i + currentTrailBufferID) % particleTrailFBOs.length].getTextureID());
				GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f / Particle.PARTICLE_TRAIL_LENGTH * i);
				this.renderTexturedQuad();
			}
			currentTrailBufferID = (currentTrailBufferID + 1) % particleTrailFBOs.length;
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
			GL11.glDisable(GL11.GL_TEXTURE_2D);

			GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);

			basicShader.bindShader();
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, particleVBO);
			GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, particleVertexByteCount, 0);
			GL20.glVertexAttribPointer(1, 1, GL11.GL_FLOAT, false, particleVertexByteCount, 2 * Float.BYTES);
			GL11.glDrawArrays(GL11.GL_POINTS, 0, particles.size() * 2);

			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		} else {
			int particleVertexByteCount = Particle.SIZE_IN_BYTES + Float.BYTES;

			FloatBuffer particleBuffer = ByteBuffer.allocateDirect(particles.size() * particleVertexByteCount).order(ByteOrder.nativeOrder()).asFloatBuffer();
			for (Particle particle : particles) {
				particleBuffer.put(particle.position.getX());
				particleBuffer.put(particle.position.getY());
				// particleBuffer.put(particle.color);
				particleBuffer.put(1.0f);
			}
			particleBuffer.flip();

			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, particleVBO);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, particleBuffer, GL15.GL_STREAM_DRAW);
			GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, particleVertexByteCount, 0);
			GL20.glVertexAttribPointer(1, 1, GL11.GL_FLOAT, false, particleVertexByteCount, 2 * Float.BYTES);
			GL11.glDrawArrays(GL11.GL_POINTS, 0, particles.size());
		}

		// FloatBuffer particleBuffer = ByteBuffer.allocateDirect(particles.size() * Particle.SIZE_IN_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		// for (int i = 0; i < particles.size(); i++) {
		// particleBuffer.put(particles.get(i).position.getX());
		// particleBuffer.put(particles.get(i).position.getY());
		// }
		// particleBuffer.flip();
		//
		// GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, particleVBO);
		// GL15.glBufferData(GL15.GL_ARRAY_BUFFER, particleBuffer, GL15.GL_STREAM_DRAW);
		// GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * 4, 0);
		// GL11.glDrawArrays(GL11.GL_POINTS, 0, particles.size());
		// }
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
		GL20.glDisableVertexAttribArray(1);
	}

	private void drawQuadtreeOutline() {
		ArrayList<Float> quadtreeVertices = quadtree.getVertices();
		GL11.glBegin(GL11.GL_LINES);
		for (int i = 0; i < quadtreeVertices.size() / 6; i++) {
			GL11.glColor4f(quadtreeVertices.get(i * 6 + 0) * 0.0f, quadtreeVertices.get(i * 6 + 1) * 1.0f, quadtreeVertices.get(i * 6 + 2) * 0.0f, quadtreeVertices.get(i * 6 + 3)
				* 1.0f);
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
			particles.add(particle);
		}
	}
}
