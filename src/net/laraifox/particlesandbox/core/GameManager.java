package net.laraifox.particlesandbox.core;

import java.io.IOException;
import java.util.Random;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.GL11;

public class GameManager {
	public static final int THREAD_COUNT = 8;

	private final int PARTICLE_COUNT = (int) (1000 * 0.0);

	private Random random;

	private World world;

	public GameManager(float width, float height) {
		this.random = new Random();

		try {
			this.world = new World(width, height, PARTICLE_COUNT, random);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void update(float delta) {
		InputHandler.update();

		world.update(delta);
	}

	public void render() {
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		GL11.glLoadIdentity();

		world.render();
	}
}
