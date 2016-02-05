package net.laraifox.particlesandbox.interfaces;

import net.laraifox.particlesandbox.core.World;
import net.laraifox.particlesandbox.objects.Particle;

public interface IPhysicsTask {
	public void performTask(Particle particle);
}
