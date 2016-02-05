package net.laraifox.particlesandbox.physicstasks;

import net.laraifox.particlesandbox.core.World;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;
import net.laraifox.particlesandbox.objects.Particle;

public class BlankTask implements IPhysicsTask {
	@Override
	public void performTask(Particle particle) {
		return;
	}
}
