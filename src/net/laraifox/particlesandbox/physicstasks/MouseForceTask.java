package net.laraifox.particlesandbox.physicstasks;

import net.laraifox.particlesandbox.core.Particle;
import net.laraifox.particlesandbox.core.Vector2f;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;

public class MouseForceTask implements IPhysicsTask {
	private float mouseX;
	private float mouseY;
	private float force;
	private float threshold;

	public MouseForceTask(float mouseX, float mouseY, float force, float threshold) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.force = force;
		this.threshold = threshold;
	}

	@Override
	public void performTask(Particle particle) {
		Vector2f vecToMouse = Vector2f.subtract(new Vector2f(mouseX, mouseY), particle.position);

		float distance = vecToMouse.length();
		if (distance != 0.0f) {
			if (distance < threshold) {
				distance = threshold;
			}

			Vector2f acceleration = vecToMouse.normalize().scale(force / distance);

			particle.velocity = Vector2f.add(particle.velocity, acceleration);
		}
	}
}
