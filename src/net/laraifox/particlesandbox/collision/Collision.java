package net.laraifox.particlesandbox.collision;

import org.lwjgl.util.vector.Vector2f;

public class Collision {
	private boolean colliding;
	private float collisionPoint;
	private Vector2f minTranslation;

	public Collision(boolean colliding, float collisionPoint, Vector2f minTranslation) {
		this.colliding = colliding;
		this.collisionPoint = collisionPoint;
		this.minTranslation = minTranslation;
	}

	public boolean isColliding() {
		return colliding;
	}

	public float getCollisionPoint() {
		return collisionPoint;
	}

	public Vector2f getMinTranslation() {
		return minTranslation;
	}

}
