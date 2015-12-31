package net.laraifox.particlesandbox.collision;

public class Collision {
	private boolean colliding;
	private float collisionPoint;

	public Collision(boolean colliding, float collisionPoint) {
		this.colliding = colliding;
		this.collisionPoint = collisionPoint;
	}

	public boolean isColliding() {
		return colliding;
	}

	public float getCollisionPoint() {
		return collisionPoint;
	}
}
