package net.laraifox.particlesandbox.core;

import java.util.ArrayList;

import net.laraifox.particlesandbox.interfaces.IPhysicsTask;

public class PhysicsThread extends Thread {
	// public Object syncStartPhysicsObject;
	// public boolean syncStartPhysicsObjectNotified;
	// private Object syncFinishPhysicsObject;
	// private boolean syncFinishPhysicsObjectNotified;

	private int threadIndex;
	private Particle[] particles;
	private ArrayList<IPhysicsTask> physicsTasks;

	// private boolean running;

	public PhysicsThread(int threadIndex, Particle[] particles, ArrayList<IPhysicsTask> physicsTasks) {
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

		for (int i = threadIndex; i < particles.length; i += GameManager.THREAD_COUNT) {
			for (IPhysicsTask task : physicsTasks) {
				task.performTask(particles[i]);
			}
		}

		// syncFinishPhysicsObjectNotified = true;
		// synchronized (syncFinishPhysicsObject) {
		// syncFinishPhysicsObject.notifyAll();
		// }
		// }
	}
}
