package net.laraifox.particlesandbox.collision;

import net.laraifox.particlesandbox.core.Transform2D;
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

	public Collision getCollision(ICollider other) {
		return null;
	}

	public Collision getCollision(ICollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(AABBCollider other) {
		Vector2f thisMin = this.getMin();
		Vector2f thisMax = this.getMax();

		Vector2f otherMin = other.getMin();
		Vector2f otherMax = other.getMax();

		if (thisMax.getX() < otherMin.getX() || thisMax.getY() < otherMin.getY() || thisMin.getX() > otherMax.getX() || thisMin.getY() > otherMax.getY()) {
			return new Collision(false, 0.0f);
		}

		return new Collision(true, 0.0f);
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

	public Vector2f getPosition() {
		return position;
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

	public boolean contains(Vector2f position) {
		float xMin = this.getX();
		float yMin = this.getY();
		float xMax = this.getX() + this.getWidth();
		float yMax = this.getY() + this.getHeight();

		if (position.getX() >= xMin && position.getX() <= xMax && position.getY() >= yMin && position.getY() <= yMax) {
			return true;
		}

		return false;
	}

	public Transform2D getTransform() {
		return new Transform2D(position);
	}

	public Vector2f getMin() {
		return position;
	}

	public Vector2f getMax() {
		return Vector2f.add(position, size, null);
	}
}
