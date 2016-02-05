package net.laraifox.particlesandbox.physicstasks;

import java.util.ArrayList;

import net.laraifox.particlesandbox.core.GameManager;
import net.laraifox.particlesandbox.interfaces.IPhysicsTask;
import net.laraifox.particlesandbox.objects.Particle;

public class PhysicsThread extends Thread {
	// public Object syncStartPhysicsObject;
	// public boolean syncStartPhysicsObjectNotified;
	// private Object syncFinishPhysicsObject;
	// private boolean syncFinishPhysicsObjectNotified;

	private int threadIndex;
	private ArrayList<Particle> particles;
	private ArrayList<IPhysicsTask> physicsTasks;

	// private boolean running;

	public PhysicsThread(int threadIndex, ArrayList<Particle> particles, ArrayList<IPhysicsTask> physicsTasks) {
		super("Particle Physics Thread #" + threadIndex);

		// this.syncStartPhysicsObjectNotified = false;
		// this.syncStartPhysicsObject = new Object();
		// this.syncFinishPhysicsObjectNotified = false;
		// this.syncFinishPhysicsObject = new Object();

		this.threadIndex = threadIndex;
		this.particles = particles;
		this.physicsTasks = physicsTasks;
		// this.running = true;
	}

	public void run() {
		// while (running) {
		// synchronized (syncStartPhysicsObject) {
		// try {
		// while (!syncStartPhysicsObjectNotified) {
		// syncStartPhysicsObject.wait();
		// }
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		//
		// syncStartPhysicsObjectNotified = false;
		// }

		for (int i = threadIndex; i < particles.size(); i += GameManager.THREAD_COUNT) {
			for (IPhysicsTask task : physicsTasks) {
				task.performTask(particles.get(i));
			}
		}

		// syncFinishPhysicsObjectNotified = true;
		// synchronized (syncFinishPhysicsObject) {
		// syncFinishPhysicsObject.notifyAll();
		// }
		// }
	}
}
