package net.laraifox.particlesandbox.physicstasks;

import java.awt.geom.Line2D;
import java.util.ArrayList;

import net.laraifox.particlesandbox.core.Particle;
import net.laraifox.particlesandbox.core.Vector2f;
import net.laraifox.particlesandbox.core.Wall;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;

public class EnvironmentCollisionTask implements IPhysicsTask {
	private ArrayList<Wall> walls;

	public EnvironmentCollisionTask(ArrayList<Wall> walls) {
		this.walls = walls;
	}

	@Override
	public void performTask(Particle particle) {
		Line2D.Float particleLine = particle.getVelocityLine2D();

		for (Wall wall : walls) {
			if (particleLine.intersectsLine(wall.getLine2D())) {
				Vector2f v = particle.velocity;
				Vector2f n = new Vector2f(wall.getNormal().normalize());
				n.setX(Math.abs(n.getX()));
				n.setY(Math.abs(n.getY()));

				float dot = (float) Vector2f.dot(v, n);

				Vector2f r = Vector2f.subtract(v, Vector2f.scale(n, 2.0f * dot)).scale(0.5f);

				particle.velocity.set(r);
			}
		}
	}
}
