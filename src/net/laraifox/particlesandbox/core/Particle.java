package net.laraifox.particlesandbox.core;

import java.awt.geom.Line2D;
import java.util.Random;

import net.laraifox.particlesandbox.collision.Point2DCollider;
import net.laraifox.particlesandbox.interfaces.ICollidable;
import net.laraifox.particlesandbox.interfaces.ICollider;
import net.laraifox.particlesandbox.interfaces.IRenderObject;

public class Particle implements ICollidable, IRenderObject {
	public static final int SIZE_IN_BYTES = 2 * 4;

	public static final float PARTICLE_MASS = 100.0f;
	public static final float PARTICLE_MIN_SPEED = 0.000663f;
	public static final float PARTICLE_RADIUS = 2.0f;

	private static World world;

	public Vector2f position;
	public Vector2f velocity;

	private Line2D.Float line;

	public Particle(Vector2f position, Vector2f velocity) {
		this.position = position;
		this.velocity = velocity;

		this.line = new Line2D.Float(position.getX(), position.getY(), position.getX() + velocity.getX(), position.getY() + velocity.getY());
	}

	public Particle(float width, float height, Random random) {
		this.position = new Vector2f((random.nextFloat() * 2.0f - 1.0f) * width, (random.nextFloat() * 2.0f - 1.0f) * height);
		this.velocity = new Vector2f((random.nextFloat() - 0.5f) * 0.0f, (random.nextFloat() - 0.5f) * 0.0f);

		this.line = new Line2D.Float(position.getX(), position.getY(), position.getX() + velocity.getX(), position.getY() + velocity.getY());
	}

	public void update() {
		line.x1 = position.getX();
		line.y1 = position.getY();
		line.x2 = position.getX() + velocity.getX();
		line.y2 = position.getY() + velocity.getY();
	}

	public ICollider getCollider() {
		return new Point2DCollider(position);
	}

	public static void setWorld(World world) {
		Particle.world = world;
	}

	@Override
	public float[] getVerticesData() {
		return new float[] {
				position.getX(), position.getY()
		};
	}

	public Line2D.Float getVelocityLine2D() {
		return line;
	}
}
