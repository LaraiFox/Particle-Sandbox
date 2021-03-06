package net.laraifox.particlesandbox.objects;

import java.awt.geom.Line2D;

import net.laraifox.particlesandbox.collision.AABBCollider;
import net.laraifox.particlesandbox.core.Vector2f;
import net.laraifox.particlesandbox.interfaces.ICollidable;
import net.laraifox.particlesandbox.interfaces.ICollider;

public class Wall implements ICollidable {
	public static final int SIZE_IN_BYTES = 4 * 4;

	private Vector2f start, end;

	public Wall(Vector2f start, Vector2f end) {
		this.start = start;
		this.end = end;
	}

	public ICollider getCollider() {
		return new AABBCollider(start, Vector2f.subtract(end, start));
	}

	public Vector2f getNormal() {
		return Vector2f.subtract(end, start).normalize().cross();
	}

	public Vector2f getStart() {
		return start;
	}

	public Vector2f getEnd() {
		return end;
	}

	public Line2D.Float getLine2D() {
		return new Line2D.Float(start.getX(), start.getY(), end.getX(), end.getY());
	}

}
