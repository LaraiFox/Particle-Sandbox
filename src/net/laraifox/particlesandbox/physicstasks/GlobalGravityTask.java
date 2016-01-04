package net.laraifox.particlesandbox.physicstasks;

import net.laraifox.particlesandbox.core.Particle;
import net.laraifox.particlesandbox.core.Vector2f;
import net.laraifox.particlesandbox.core.World;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;

public class GlobalGravityTask implements IPhysicsTask {
	private Particle[] particles;

	public GlobalGravityTask(Particle[] particles) {
		this.particles = particles;
	}

	@Override
	public void performTask(Particle particle) {
		for (Particle otherParticle : particles) {
			if (otherParticle != particle) {
				float particleDistanceRadius = particle.position.distanceTo(otherParticle.position) / 2.0f;
				if (particleDistanceRadius < (World.PARTICLE_RADIUS * 2.0f))
					particleDistanceRadius = World.PARTICLE_RADIUS * 2.0f;

				float force = (World.GRAVITATIONAL_CONSTANT * World.PARTICLE_MASS * World.PARTICLE_MASS) / (particleDistanceRadius * particleDistanceRadius);

				Vector2f direction = Vector2f.subtract(otherParticle.position, particle.position).normalize();

				particle.velocity.add(Vector2f.scale(direction, force / World.PARTICLE_MASS));
				otherParticle.velocity.add(Vector2f.scale(direction, -force / World.PARTICLE_MASS));
			}
		}
	}
}
