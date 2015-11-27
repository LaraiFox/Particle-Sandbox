package net.laraifox.particlesandbox.collision;

import net.laraifox.particlesandbox.interfaces.ICollider;

import org.lwjgl.util.vector.Vector2f;

public class AABBCollider implements ICollider {
	private Vector2f position;
	private Vector2f size;

	public AABBCollider(Vector2f position, Vector2f size) {
		this.position = position;
		this.size = size;
	}

	public AABBCollider(float x, float y, float width, float height) {
		this.position = new Vector2f(x, y);
		this.size = new Vector2f(width, height);
	}

	@Override
	public Collision collides(ICollider other) {
		// AUTO: Auto-generated method stub
		return null;
	}

	@Override
	public Collision collides(ICollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		// AUTO: Auto-generated method stub
		return null;
	}

	@Override
	public Collision collides(AABBCollider other) {
		// AUTO: Auto-generated method stub
		return null;
	}

	@Override
	public Collision collides(AABBCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		// AUTO: Auto-generated method stub
		return null;
	}

	@Override
	public Collision collides(CircleCollider other) {
		// AUTO: Auto-generated method stub
		return null;
	}

	@Override
	public Collision collides(CircleCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		// AUTO: Auto-generated method stub
		return null;
	}

	@Override
	public Collision collides(Line2DCollider other) {
		// AUTO: Auto-generated method stub
		return null;
	}

	@Override
	public Collision collides(Line2DCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		// AUTO: Auto-generated method stub
		return null;
	}

	@Override
	public Collision collides(Point2DCollider other) {
		// AUTO: Auto-generated method stub
		return null;
	}

	@Override
	public Collision collides(Point2DCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		// AUTO: Auto-generated method stub
		return null;
	}

	public float getX() {
		return position.getX();
	}

	public float getY() {
		return position.getY();
	}

	public float getWidth() {
		return size.getX();
	}

	public float getHeight() {
		return size.getY();
	}

}
