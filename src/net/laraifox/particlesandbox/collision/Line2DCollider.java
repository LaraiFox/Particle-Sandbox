package net.laraifox.particlesandbox.collision;

import net.laraifox.particlesandbox.core.Transform2D;
import net.laraifox.particlesandbox.core.Vector2f;
import net.laraifox.particlesandbox.interfaces.ICollider;

public class Line2DCollider implements ICollider {
	private Transform2D transform;
	private Vector2f size;

	public Line2DCollider(Vector2f position, Vector2f size) {
		this.transform = new Transform2D(position);
		this.size = new Vector2f(size);
	}

	public Collision getCollision(ICollider other) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(ICollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(AABBCollider other) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(AABBCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(CircleCollider other) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(CircleCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(Line2DCollider other) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(Line2DCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(Point2DCollider other) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(Point2DCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		// TODO Auto-generated method stub
		return null;
	}

	public Transform2D getTransform() {
		// TODO Auto-generated method stub
		return null;
	}

	public Vector2f getMin() {
		// TODO Auto-generated method stub
		return null;
	}

	public Vector2f getMax() {
		// TODO Auto-generated method stub
		return null;
	}
}
