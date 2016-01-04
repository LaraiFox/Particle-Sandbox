package net.laraifox.particlesandbox.physicstasks;

import java.util.Random;

import net.laraifox.particlesandbox.core.Particle;
import net.laraifox.particlesandbox.core.Vector2f;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;

public class ParticleMovementTask implements IPhysicsTask {
	private Random random;

	private float worldWidth;
	private float worldHeight;

	private float particleRadius = 0.5f;
	private Vector2f fluidVelocity = new Vector2f(0.0f, 0.0f);
	private float fluidForceVariance = 0.0f;
	private float fluidVelocityVariance = 0.0f;
	private float rho = 0.01f;

	public ParticleMovementTask(float worldWidth, float worldHeight, Random random) {
		this.random = random;

		this.worldWidth = worldWidth;
		this.worldHeight = worldHeight;
		
		this.particleRadius = particleRadius;
		this.fluidVelocity = fluidVelocity;
		this.fluidForceVariance = fluidForceVariance;
		this.fluidVelocityVariance = fluidVelocityVariance;
		this.rho = rho;
	}

	@Override
	public void performTask(Particle particle) {
		particle.position.add(Vector2f.scale(particle.velocity, (float) 1.0f));

		Vector2f relativeVelocity = Vector2f.subtract(fluidVelocity, particle.velocity);

		float particleReferenceArea = (float) (Math.PI * (particleRadius * particleRadius));

		float dragCoefficient = 0.47f;

		float dragForce = 0.5f * rho * relativeVelocity.lengthSq() * dragCoefficient * particleReferenceArea;

		float randomForceVariance = 1.0f - (random.nextFloat() * fluidForceVariance);
		float randomDirectionVariance = (random.nextFloat() - 0.5f) * fluidVelocityVariance * 2.0f;
		Vector2f dragVector = Vector2f.scale(relativeVelocity.normalize(), dragForce * randomForceVariance).rotate(randomDirectionVariance);

		particle.velocity.add(dragVector);

		// particle.velocity.scale(0.99f);

		if (particle.velocity.length() <= Particle.PARTICLE_MIN_SPEED)
			particle.velocity = Vector2f.Zero();

		if (particle.position.getX() <= 0.0f) {
			particle.position.setX(0.1f);
		} else if (particle.position.getX() >= worldWidth) {
			particle.position.setX(worldWidth - 0.1f);
		}
		
		if (particle.position.getY() <= 0.0f) {
			particle.position.setY(0.1f);
		} else if (particle.position.getY() >= worldHeight) {
			particle.position.setY(worldHeight - 0.1f);
		}
	}

}
