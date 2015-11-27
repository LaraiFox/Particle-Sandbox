package net.laraifox.particlesandbox.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;

import net.laraifox.particlesandbox.collision.AABBCollider;
import net.laraifox.particlesandbox.collision.QuadTree;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;

public class World {
	private float width, height;

	private QuadTree quadtree;

	private int particleVBO;
	private int particleCount;
	private Particle[] particles;

	Vector2f averagePosition = new Vector2f(0.0f, 0.0f);

	private int wallVBO;
	private ArrayList<Wall> walls;

	public World(float width, float height, int particleCount, Random random) {
		Particle.setWorld(this);

		this.width = width;
		this.height = height;

//		this.quadtree = new QuadTree(new AABBCollider(0, 0, width, height));

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
		walls.add(new Wall(new Vector2f(0.0f, 0.0f), new Vector2f(width, 0.0f)));
		walls.add(new Wall(new Vector2f(width, 0.0f), new Vector2f(width, height)));
		walls.add(new Wall(new Vector2f(width, height), new Vector2f(0.0f, height)));
		walls.add(new Wall(new Vector2f(0.0f, height), new Vector2f(0.0f, 0.0f)));

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}

	public void update(float delta) {
//		for (int i = 0; i < particleCount; i++) {
//			quadtree.insert(particles[i].getCollider());
//		}

		averagePosition = new Vector2f(0.0f, 0.0f);
		for (int i = 0; i < particleCount; i++) {
			averagePosition = Vector2f.add(averagePosition, new Vector2f(particles[i].position), null);
		}
		averagePosition = (Vector2f) averagePosition.scale(1.0f / (float) particleCount);

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

		if (Mouse.isButtonDown(0)) {
			Vector2f mousePosition = new Vector2f(Mouse.getX(), Mouse.getY());

			for (int i = 0; i < particleCount; i++) {
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
		}

		for (int i = 0; i < particleCount; i++) {
			particles[i].position = Vector2f.add(particles[i].position, particles[i].velocity, null);
			particles[i].velocity = (Vector2f) particles[i].velocity.scale(0.99f);
		}
	}

	public void render() {
		GL11.glDisable(GL11.GL_TEXTURE_2D);

		GL11.glPointSize(1.0f);
		GL11.glLineWidth(5.0f);

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
		GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * 2, 0);
		GL11.glDrawArrays(GL11.GL_LINES, 0, wallCount);

		GL20.glDisableVertexAttribArray(0);

		GL11.glPointSize(5.0f);

		GL11.glColor3f(1.0f, 0.0f, 0.0f);

		GL11.glBegin(GL11.GL_POINTS);
		GL11.glVertex2f(averagePosition.getX(), averagePosition.getY());
		GL11.glEnd();
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}
}
