package net.laraifox.particlesandbox.interfaces;

import net.laraifox.particlesandbox.collision.AABBCollider;
import net.laraifox.particlesandbox.collision.CircleCollider;
import net.laraifox.particlesandbox.collision.Collision;
import net.laraifox.particlesandbox.collision.Line2DCollider;
import net.laraifox.particlesandbox.collision.Point2DCollider;

import org.lwjgl.util.vector.Vector2f;

public interface ICollider {
	public Collision collides(ICollider other);

	public Collision collides(ICollider other, Vector2f thisVelocity, Vector2f otherVelocity);

	public Collision collides(AABBCollider other);

	public Collision collides(AABBCollider other, Vector2f thisVelocity, Vector2f otherVelocity);

	public Collision collides(CircleCollider other);

	public Collision collides(CircleCollider other, Vector2f thisVelocity, Vector2f otherVelocity);

	public Collision collides(Line2DCollider other);

	public Collision collides(Line2DCollider other, Vector2f thisVelocity, Vector2f otherVelocity);

	public Collision collides(Point2DCollider other);

	public Collision collides(Point2DCollider other, Vector2f thisVelocity, Vector2f otherVelocity);
}
