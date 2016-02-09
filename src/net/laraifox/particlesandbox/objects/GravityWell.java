package net.laraifox.particlesandbox.objects;

import net.laraifox.particlesandbox.core.Vector2f;

public class GravityWell {
	public Vector2f position;
	public Vector2f velocity;
	public float strength, threshold, limit;
	public float veloctiyScalar;

	public GravityWell(Vector2f position, Vector2f velocity, float strength, float threshold, float limit, float veloctiyScalar) {
		this.position = position;
		this.velocity = velocity;
		this.strength = strength;
		this.threshold = threshold;
		this.limit = limit;
		this.veloctiyScalar = veloctiyScalar;
	}
}
