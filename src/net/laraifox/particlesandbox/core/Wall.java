package net.laraifox.particlesandbox.core;

import net.laraifox.particlesandbox.interfaces.ICollidable;
import net.laraifox.particlesandbox.interfaces.ICollider;

import org.lwjgl.util.vector.Vector2f;

public class Wall implements ICollidable {
	public static final int SIZE_IN_BYTES = 4 * 4;

	private Vector2f start, end;

	public Wall(Vector2f start, Vector2f end) {
		this.start = start;
		this.end = end;
	}

	@Override
	public ICollider getCollider() {
		return null;
	}

	public Vector2f getNormal() {
		return null;
	}

	public Vector2f getStart() {
		return start;
	}

	public Vector2f getEnd() {
		return end;
	}

}
