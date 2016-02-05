package net.laraifox.particlesandbox.physicstasks;

import net.laraifox.particlesandbox.collision.Quadtree;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;
import net.laraifox.particlesandbox.objects.Particle;

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
