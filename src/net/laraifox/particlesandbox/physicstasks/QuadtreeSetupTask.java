package net.laraifox.particlesandbox.physicstasks;

import net.laraifox.particlesandbox.collision.Quadtree;
import net.laraifox.particlesandbox.core.Particle;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;

public class QuadtreeSetupTask implements IPhysicsTask {
	private Quadtree quadtree;

	public QuadtreeSetupTask(Quadtree quadtree) {
		this.quadtree = quadtree;
	}

	@Override
	public void performTask(Particle particle) {
		quadtree.insert(particle);
	}
}
