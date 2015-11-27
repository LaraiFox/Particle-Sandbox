package net.laraifox.particlesandbox.collision;

import net.laraifox.particlesandbox.interfaces.ICollider;

import org.lwjgl.util.vector.Vector2f;

public class Point2DCollider implements ICollider {
	private Vector2f position;

	public Point2DCollider(Vector2f position) {
		this.position = position;
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

}
