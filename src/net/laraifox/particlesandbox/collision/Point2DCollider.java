package net.laraifox.particlesandbox.collision;

import net.laraifox.particlesandbox.core.Transform2D;
import net.laraifox.particlesandbox.interfaces.ICollider;

import org.lwjgl.util.vector.Vector2f;

public class Point2DCollider implements ICollider {
	private Vector2f position;

	public Point2DCollider(Vector2f position) {
		this.position = position;
	}

	public Collision getCollision(ICollider other) {
		if (other instanceof AABBCollider) {
			return this.getCollision((AABBCollider) other);
		} else if (other instanceof CircleCollider) {
			return this.getCollision((CircleCollider) other);
		} else if (other instanceof Line2DCollider) {
			return this.getCollision((Line2DCollider) other);
		} else if (other instanceof Point2DCollider) {
			return this.getCollision((Point2DCollider) other);
		}

		return null;
	}

	public Collision getCollision(ICollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		if (other instanceof AABBCollider) {
			return this.getCollision((AABBCollider) other, thisVelocity, otherVelocity);
		} else if (other instanceof CircleCollider) {
			return this.getCollision((CircleCollider) other, thisVelocity, otherVelocity);
		} else if (other instanceof Line2DCollider) {
			return this.getCollision((Line2DCollider) other, thisVelocity, otherVelocity);
		} else if (other instanceof Point2DCollider) {
			return this.getCollision((Point2DCollider) other, thisVelocity, otherVelocity);
		}

		return null;
	}

	public Collision getCollision(AABBCollider other) {
		float xMin = other.getX();
		float yMin = other.getY();
		float xMax = other.getX() + other.getWidth();
		float yMax = other.getY() + other.getHeight();

		if (position.getX() >= xMin && position.getX() <= xMax && position.getY() >= yMin && position.getY() <= yMax) {
			return new Collision(true, 0.0f);
		}

		return new Collision(false, 0.0f);
	}

	public Collision getCollision(AABBCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		Vector2f combinedVelocity = Vector2f.add(thisVelocity, otherVelocity, null);

		return new Line2DCollider(position, Vector2f.add(position, combinedVelocity, null)).getCollision(other);
	}

	public Collision getCollision(CircleCollider other) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(CircleCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		Vector2f combinedVelocity = Vector2f.add(thisVelocity, otherVelocity, null);

		return new Line2DCollider(position, Vector2f.add(position, combinedVelocity, null)).getCollision(other);
	}

	public Collision getCollision(Line2DCollider other) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(Line2DCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		Vector2f combinedVelocity = Vector2f.add(thisVelocity, otherVelocity, null);

		return new Line2DCollider(position, Vector2f.add(position, combinedVelocity, null)).getCollision(other);
	}

	public Collision getCollision(Point2DCollider other) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collision getCollision(Point2DCollider other, Vector2f thisVelocity, Vector2f otherVelocity) {
		Vector2f combinedVelocity = Vector2f.add(thisVelocity, otherVelocity, null);

		return new Line2DCollider(position, Vector2f.add(position, combinedVelocity, null)).getCollision(other);
	}

	public Vector2f getPosition() {
		return position;
	}

	public Transform2D getTransform() {
		return new Transform2D(position);
	}

	public Vector2f getMin() {
		return position;
	}

	public Vector2f getMax() {
		return position;
	}
}
