package net.laraifox.particlesandbox.core;

import java.util.Random;

public class ParticleEmitter {
	private World world;
	private Random random;

	private Transform2D transform;
	private float minSpeed, maxSpeed;
	private float spawnrate;
	private float deltaTime;

	public ParticleEmitter(Transform2D transform, float spawnrate, float minSpeed, float maxSpeed, World world, Random random) {
		this.world = world;
		this.random = random;

		this.transform = transform;
		this.minSpeed = minSpeed;
		this.maxSpeed = maxSpeed;
		this.spawnrate = spawnrate;
		this.deltaTime = 0.0f;
	}

	public void update(float delta) {
		this.deltaTime += delta;

		if (deltaTime >= spawnrate) {
			this.deltaTime -= spawnrate;

			Vector2f offset = new Vector2f(transform.getScale().getX() * (random.nextFloat() * 2.0f - 1.0f), transform.getScale().getY() * (random.nextFloat() * 2.0f - 1.0f));
			Vector2f position = Vector2f.add(transform.getTranslation(), offset);

			float speed = minSpeed + (maxSpeed - minSpeed) * random.nextFloat();
			Vector2f velocity = Vector2f.rotate(Vector2f.PositiveX(), transform.getRotation()).scale(speed);

			world.addParticle(new Particle(position, velocity));
		}
	}
}
