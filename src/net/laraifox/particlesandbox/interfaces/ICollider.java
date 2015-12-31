package net.laraifox.particlesandbox.interfaces;

import net.laraifox.particlesandbox.collision.AABBCollider;
import net.laraifox.particlesandbox.collision.CircleCollider;
import net.laraifox.particlesandbox.collision.Collision;
import net.laraifox.particlesandbox.collision.Line2DCollider;
import net.laraifox.particlesandbox.collision.Point2DCollider;
import net.laraifox.particlesandbox.core.Transform2D;

import org.lwjgl.util.vector.Vector2f;

public interface ICollider {
	public Collision getCollision(ICollider other);

	public Collision getCollision(ICollider other, Vector2f thisVelocity, Vector2f otherVelocity);

	public Collision getCollision(AABBCollider other);

	public Collision getCollision(AABBCollider other, Vector2f thisVelocity, Vector2f otherVelocity);

	public Collision getCollision(CircleCollider other);

	public Collision getCollision(CircleCollider other, Vector2f thisVelocity, Vector2f otherVelocity);

	public Collision getCollision(Line2DCollider other);

	public Collision getCollision(Line2DCollider other, Vector2f thisVelocity, Vector2f otherVelocity);

	public Collision getCollision(Point2DCollider other);

	public Collision getCollision(Point2DCollider other, Vector2f thisVelocity, Vector2f otherVelocity);

	public Transform2D getTransform();

	public Vector2f getMin();

	public Vector2f getMax();
}
