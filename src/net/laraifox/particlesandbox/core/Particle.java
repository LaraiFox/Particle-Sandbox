package net.laraifox.particlesandbox.core;

import java.util.Random;

import net.laraifox.particlesandbox.collision.Point2DCollider;
import net.laraifox.particlesandbox.interfaces.ICollidable;
import net.laraifox.particlesandbox.interfaces.ICollider;
import net.laraifox.particlesandbox.interfaces.IRenderObject;

import org.lwjgl.util.vector.Vector2f;

public class Particle implements ICollidable, IRenderObject {
	public static final int SIZE_IN_BYTES = 2 * 4;

	private static World world;

	public Vector2f position;
	public Vector2f velocity;

	public Particle(Random random) {
		this.position = new Vector2f(random.nextFloat() * world.getWidth(), random.nextFloat() * world.getHeight());
		// this.position = new Vector2f(random.nextFloat() * world.getWidth() / 5 + world.getWidth() / 5 * 2, random.nextFloat()* world.getHeight() / 5 +
		// world.getHeight() / 5 * 2);
		this.velocity = new Vector2f(0.0f, 0.0f);
	}

	public ICollider getCollider() {
		return new Point2DCollider(position);
	}

	public static void setWorld(World world) {
		Particle.world = world;
	}

	@Override
	public float[] getVerticesData() {
		return new float[] { position.getX(), position.getY() };
	}
}
