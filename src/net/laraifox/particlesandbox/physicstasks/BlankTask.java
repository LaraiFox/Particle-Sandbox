package net.laraifox.particlesandbox.physicstasks;

import net.laraifox.particlesandbox.core.Particle;
import net.laraifox.particlesandbox.core.World;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;

public class BlankTask implements IPhysicsTask {
	@Override
	public void performTask(Particle particle) {
		return;
	}
}
