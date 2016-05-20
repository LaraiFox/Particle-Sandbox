package net.laraifox.particlesandbox.tasks;

import net.laraifox.particlesandbox.collision.Quadtree;
import net.laraifox.particlesandbox.objects.Particle;

public class QuadtreeSetupTask implements IProcessingTask {
	private Particle[] particles;
	private Quadtree quadtree;

	public QuadtreeSetupTask(Particle[] particles, Quadtree quadtree) {
		this.particles = particles;
		this.quadtree = quadtree;
	}

	@Override
	public void process(int threadIndex, int threadCount) {
		for (int i = threadIndex; i < particles.length; i += threadCount) {
			quadtree.insert(particles[i]);
		}
	}
}
