package net.laraifox.particlesandbox.core;

import java.util.Random;

import net.laraifox.particlesandbox.collision.Line2DCollider;
import net.laraifox.particlesandbox.interfaces.ICollidable;
import net.laraifox.particlesandbox.interfaces.ICollider;

import org.lwjgl.util.vector.Vector2f;

public class Particle implements ICollidable {
	public static final int SIZE_IN_BYTES = 2 * 4;

	private static World world;

	public Vector2f position;
	public Vector2f velocity;

	public Particle(Random random) {
		this.position = new Vector2f(random.nextFloat() * world.getWidth(), random.nextFloat() * world.getHeight());
		this.velocity = new Vector2f(0.0f, 0.0f);
	}

	@Override
	public ICollider getCollider() {
		return new Line2DCollider(position, Vector2f.add(position, velocity, null));
	}

	public static void setWorld(World world) {
		Particle.world = world;
	}

}
