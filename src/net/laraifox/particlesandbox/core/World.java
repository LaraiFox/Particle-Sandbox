package net.laraifox.particlesandbox.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import net.laraifox.particlesandbox.collision.AABBCollider;
import net.laraifox.particlesandbox.collision.Quadtree;
import net.laraifox.particlesandbox.interfaces.ICollidable;
import net.laraifox.particlesandbox.interfaces.IRenderObject;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

public class World {
	private class PhysicsThread extends Thread {
		// public Object syncStartPhysicsObject;
		// public boolean syncStartPhysicsObjectNotified;
		// private Object syncFinishPhysicsObject;
		// private boolean syncFinishPhysicsObjectNotified;

		private int threadIndex;

		// private boolean running;

		public PhysicsThread(int threadIndex) {
			super("Particle Physics Thread #" + threadIndex);

			// this.syncStartPhysicsObjectNotified = false;
			// this.syncStartPhysicsObject = new Object();
			// this.syncFinishPhysicsObjectNotified = false;
			// this.syncFinishPhysicsObject = new Object();

			this.threadIndex = threadIndex;
			// this.running = true;
		}

		public void run() {
			// while (running) {
			// synchronized (syncStartPhysicsObject) {
			// try {
			// while (!syncStartPhysicsObjectNotified) {
			// syncStartPhysicsObject.wait();
			// }
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			//
			// syncStartPhysicsObjectNotified = false;
			// }

			this.updatePhysics();

			// syncFinishPhysicsObjectNotified = true;
			// synchronized (syncFinishPhysicsObject) {
			// syncFinishPhysicsObject.notifyAll();
			// }
			// }
		}

		private void updatePhysics() {
			if (Mouse.isButtonDown(0) && !Mouse.isButtonDown(1) && !Mouse.isButtonDown(2)) {
				Vector2f mousePosition = Vector2f.add(camera.getPosition(), new Vector2f(Mouse.getX(), Mouse.getY()), null);

				for (int i = threadIndex; i < particles.length; i += physicsThreads.length) {
					Vector2f vecToMouse = Vector2f.sub(mousePosition, particles[i].position, null);

					float distance = vecToMouse.length();
					if (distance != 0.0f) {
						if (distance < 1.0f) {
							distance = 1.0f;
						}

						Vector2f acceleration = (Vector2f) vecToMouse.normalise(null).scale(1.0f / distance);

						particles[i].velocity = Vector2f.add(particles[i].velocity, acceleration, null);
					}
				}
			} else if (Mouse.isButtonDown(1) && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(2)) {
				Vector2f mousePosition = Vector2f.add(camera.getPosition(), new Vector2f(Mouse.getX(), Mouse.getY()), null);

				for (int i = threadIndex; i < particles.length; i += physicsThreads.length) {
					Vector2f vecToMouse = Vector2f.sub(mousePosition, particles[i].position, null);

					float distance = vecToMouse.length();
					if (distance != 0.0f) {
						if (distance < 1.0f) {
							distance = 1.0f;
						}

						Vector2f acceleration = (Vector2f) vecToMouse.normalise(null).scale(-1.0f / distance);

						particles[i].velocity = Vector2f.add(particles[i].velocity, acceleration, null);
					}
				}
			}
		}
	}

	private int physicsThreadCount = 8;
	private PhysicsThread[] physicsThreads;

	private float width, height;

	private final float CAMERA_SPEED = 1.5f;
	private Vector2f cameraStartPosition;
	private Camera camera;

	private Quadtree quadtree;

	private int particleVBO;
	private int particleCount;
	private Particle[] particles;

	ArrayList<Vector2f> averagePositions = new ArrayList<Vector2f>();

	private int wallVBO;
	private ArrayList<Wall> walls;

	private int collidingParticleVBO;
	private ArrayList<ICollidable> collidingParticles;

	private HashMap<Integer, ArrayList<IRenderObject>> renderMap;

	public World(float width, float height, int particleCount, Random random) {
		Particle.setWorld(this);

		this.physicsThreads = new PhysicsThread[physicsThreadCount];

		this.width = width * 1;
		this.height = height * 1;

		this.cameraStartPosition = new Vector2f(this.width / 2.0f - width / 2.0f, this.height / 2.0f - height / 2.0f);
		this.camera = new Camera((Matrix4f) new Matrix4f().setIdentity(), cameraStartPosition);

		float quadtreeDepth = this.width;
		if (this.height > this.width)
			quadtreeDepth = this.height;
		quadtreeDepth = (float) Math.ceil(Math.sqrt((quadtreeDepth / 100.0f)));
		this.quadtree = new Quadtree(new AABBCollider(0, 0, this.width, this.height), 100, (int) quadtreeDepth);

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
		walls.add(new Wall(new Vector2f(0.0f, 0.0f), new Vector2f(this.width, 0.0f)));
		walls.add(new Wall(new Vector2f(this.width, 0.0f), new Vector2f(this.width, this.height)));
		walls.add(new Wall(new Vector2f(this.width, this.height), new Vector2f(0.0f, this.height)));
		walls.add(new Wall(new Vector2f(0.0f, this.height), new Vector2f(0.0f, 0.0f)));

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

		this.collidingParticleVBO = GL15.glGenBuffers();
		this.collidingParticles = new ArrayList<ICollidable>();

		this.setRenderMap(new HashMap<Integer, ArrayList<IRenderObject>>());

		// for (int i = 0; i < physicsThreadCount; i++) {
		// physicsThreads[i] = new PhysicsThread(i);
		// physicsThreads[i].start();
		// }
	}

	// @Override
	// public void finalize() {
	// System.out.println("Starting Thread Shutdown...");
	// for (int i = 0; i < physicsThreads.length; i++) {
	// System.out.println("Shutting down thread #" + i);
	// physicsThreads[i].running = false;
	// physicsThreads[i].syncFinishPhysicsObjectNotified = true;
	// synchronized (physicsThreads[i].syncStartPhysicsObject) {
	// physicsThreads[i].syncStartPhysicsObject.notifyAll();
	// }
	//
	// try {
	// physicsThreads[i].join(10000);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// System.exit(1);
	// }
	// System.out.println("    Shutdown complete!");
	// }
	// }

	public void update(float delta) {
		quadtree.clear();
		// for (int i = 0; i < particleCount; i++) {
		// quadtree.insert(particles[i]);
		// }

		AABBCollider testCollider = new AABBCollider(width / 5 * 2, height / 5 * 2, width / 5, height / 5);

		collidingParticles.clear();
		// for (ICollidable collidable : particles) {
		// collidingParticles.add(collidable);
		// }
		collidingParticles = quadtree.retrieve(testCollider);

		// System.out.println(collidingParticles.size());
		Iterator<ICollidable> collidingParticlesIterator = collidingParticles.iterator();
		while (collidingParticlesIterator.hasNext()) {
			ICollidable collidable = collidingParticlesIterator.next();
			if (!collidable.getCollider().getCollision(testCollider).isColliding()) {
				collidingParticlesIterator.remove();
			}
		}

		// System.out.println(collidingParticles.size());
		// System.out.println();

		// ArrayList<ArrayList<ICollider>> colliderLists = new ArrayList<ArrayList<ICollider>>();
		// for (int i = 0; i < 4; i++) {
		// for (int j = 0; j < 4; j++) {
		// for (int k = 0; k < 4; k++) {
		// for (int l = 0; l < 4; l++) {
		// colliderLists.add(quadtree.getList(new int[] {
		// i, j, k, l
		// }));
		// }
		// }
		// }
		// }
		//
		// averagePositions.clear();
		// for (ArrayList<ICollider> list : colliderLists) {
		// Vector2f averagePosition = new Vector2f(0.0f, 0.0f);
		// for (ICollider collider : list) {
		// averagePosition = Vector2f.add(averagePosition, ((Point2DCollider) collider).getPosition(), null);
		// }
		// averagePosition = (Vector2f) averagePosition.scale(1.0f / (float) list.size());
		//
		// averagePositions.add(averagePosition);
		// }
		//
		// for (int j = 0; j < averagePositions.size(); j++) {
		// Vector2f averagePosition = averagePositions.get(j);
		// float scalar = (float) averagePositions.size() / (float) (particleCount / averagePositions.size());
		//
		// int startIndex = j * (particleCount / averagePositions.size());
		// int endIndex = (j + 1) * (particleCount / averagePositions.size());
		// if (endIndex >= particleCount)
		// endIndex = particleCount;
		//
		// for (int i = startIndex; i < endIndex; i++) {
		// Vector2f vec = Vector2f.sub(averagePosition, particles[i].position, null);
		//
		// float distance = vec.length() * 10.0f;
		// if (distance != 0.0f) {
		// if (distance < 1.0f) {
		// distance = 1.0f;
		// }
		//
		// Vector2f acceleration = (Vector2f) vec.normalise(null).scale((1.0f / distance) * scalar);
		//
		// particles[i].velocity = Vector2f.add(particles[i].velocity, acceleration, null);
		// }
		// }
		// }

		// averagePosition = new Vector2f(0.0f, 0.0f);
		// for (int i = 0; i < particleCount; i++) {
		// averagePosition = Vector2f.add(averagePosition, new Vector2f(particles[i].position), null);
		// }
		// averagePosition = (Vector2f) averagePosition.scale(1.0f / (float) particleCount);

		// for (int i = 0; i < particleCount; i++) {
		// Vector2f vecToMouse = Vector2f.sub(averagePosition, particles[i].position, null);
		//
		// float distance = vecToMouse.length() * 10.0f;
		// if (distance != 0.0f) {
		// if (distance < 1.0f) {
		// distance = 1.0f;
		// }
		//
		// Vector2f acceleration = (Vector2f) vecToMouse.normalise(null).scale(1.0f / distance);
		//
		// particles[i].velocity = Vector2f.add(particles[i].velocity, acceleration, null);
		// }
		// }

		if (Mouse.isButtonDown(2)) {
			camera.translate(-Mouse.getDX(), -Mouse.getDY());
		} else {
			if (Keyboard.isKeyDown(Keyboard.KEY_DOWN) && !Keyboard.isKeyDown(Keyboard.KEY_UP)) {
				camera.translate(0.0f, -CAMERA_SPEED);
			} else if (Keyboard.isKeyDown(Keyboard.KEY_UP) && !Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
				camera.translate(0.0f, CAMERA_SPEED);
			}

			if (Keyboard.isKeyDown(Keyboard.KEY_LEFT) && !Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
				camera.translate(-CAMERA_SPEED, 0.0f);
			} else if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT) && !Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
				camera.translate(CAMERA_SPEED, 0.0f);
			}

			if (Keyboard.isKeyDown(Keyboard.KEY_NUMPAD0) && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
				camera.setPosition(cameraStartPosition);
			}
		}

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

		for (int i = 0; i < physicsThreadCount; i++) {
			physicsThreads[i] = new PhysicsThread(i);
			physicsThreads[i].start();
		}

		// else {
		// if (Mouse.isButtonDown(0)) {
		// Vector2f mousePosition = Vector2f.add(camera.getPosition(), new Vector2f(Mouse.getX(), Mouse.getY()), null);
		//
		// for (int i = 0; i < particleCount; i++) {
		// Vector2f vecToMouse = Vector2f.sub(mousePosition, particles[i].position, null);
		//
		// float distance = vecToMouse.length();
		// if (distance != 0.0f) {
		// if (distance < 1.0f) {
		// distance = 1.0f;
		// }
		//
		// Vector2f acceleration = (Vector2f) vecToMouse.normalise(null).scale(1.0f / distance);
		//
		// particles[i].velocity = Vector2f.add(particles[i].velocity, acceleration, null);
		// }
		// }
		// }
		// }

		try {
			for (int i = 0; i < physicsThreadCount; i++) {
				physicsThreads[i].join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < particleCount; i++) {
			particles[i].position = Vector2f.add(particles[i].position, (Vector2f) new Vector2f(particles[i].velocity).scale(delta), null);
			particles[i].velocity = (Vector2f) particles[i].velocity.scale((float) Math.pow(0.8f, delta));
		}

		// ArrayList<IRenderObject> pointList = new ArrayList<IRenderObject>();
		// pointList.addAll(Arrays.asList(particles));
		// for (ICollidable collidable : collidingParticles) {
		// pointList.add((Particle) collidable);
		// }
	}

	public void render() {
		// HashMap<Integer, ArrayList<IRenderObject>> renderMapInstance = this.getRenderMap();
		// GL11.glTranslatef(-100, -100, 0.0f);
		FloatBuffer viewProjectionBuffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		camera.getViewProjectionMatrix().store(viewProjectionBuffer);
		viewProjectionBuffer.flip();
		GL11.glMultMatrix(viewProjectionBuffer);

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

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, wallVBO);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, wallBuffer, GL15.GL_STREAM_DRAW);
		GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * 4, 0);
		GL11.glDrawArrays(GL11.GL_LINES, 0, wallCount * 2);

		GL20.glDisableVertexAttribArray(0);

		GL11.glPointSize(5.0f);

		GL11.glColor3f(1.0f, 0.0f, 0.0f);

		GL11.glBegin(GL11.GL_POINTS);
		for (Vector2f position : averagePositions) {
			GL11.glVertex2f(position.getX(), position.getY());
		}
		GL11.glEnd();

		// ArrayList<Float> quadtreeVertices = quadtree.getVertices();
		// System.out.println("Vertices = " + (quadtreeVertices.size() / 6));
		//
		// GL11.glBegin(GL11.GL_LINES);
		// for (int i = 0; i < quadtreeVertices.size() / 6; i++) {
		// // System.out.println("    Vertex #" + i + ": Color[" + quadtreeVertices.get(i * 6 + 0) + ", " + quadtreeVertices.get(i * 6 + 1) + ", " +
		// // quadtreeVertices.get(i * 6 + 2)
		// // + ", " + quadtreeVertices.get(i * 6 + 3) + "], Position[" + quadtreeVertices.get(i * 6 + 4) + ", " + quadtreeVertices.get(i * 6 + 5) + "]");
		// GL11.glColor4f(quadtreeVertices.get(i * 6 + 0) * 0.0f, quadtreeVertices.get(i * 6 + 1) * 1.0f, quadtreeVertices.get(i * 6 + 2) * 0.0f,
		// quadtreeVertices.get(i * 6 + 3) * 1.0f);
		//
		// GL11.glVertex2f(quadtreeVertices.get(i * 6 + 4), quadtreeVertices.get(i * 6 + 5));
		// }
		// GL11.glEnd();
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
