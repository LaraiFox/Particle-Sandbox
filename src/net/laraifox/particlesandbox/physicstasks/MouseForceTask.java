package net.laraifox.particlesandbox.physicstasks;

import net.laraifox.particlesandbox.core.Particle;
import net.laraifox.particlesandbox.core.Vector2f;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;

public class MouseForceTask implements IPhysicsTask {
	private float mouseX;
	private float mouseY;
	private float force;

	public MouseForceTask(float mouseX, float mouseY, float force) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.force = force;
	}

	@Override
	public void performTask(Particle particle) {
		Vector2f vecToMouse = Vector2f.subtract(new Vector2f(mouseX, mouseY), particle.position);

		float distance = vecToMouse.length();
		if (distance != 0.0f) {
			if (distance < 10.0f) {
				distance = 10.0f;
			}

			Vector2f acceleration = vecToMouse.normalize().scale(force / distance);

			particle.velocity = Vector2f.add(particle.velocity, acceleration);
		}
	}
}
