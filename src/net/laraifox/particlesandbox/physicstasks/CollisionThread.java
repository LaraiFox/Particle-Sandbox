package net.laraifox.particlesandbox.physicstasks;

import java.awt.geom.Line2D;
import java.util.ArrayList;

import net.laraifox.particlesandbox.collision.Quadtree;
import net.laraifox.particlesandbox.core.GameManager;
import net.laraifox.particlesandbox.core.Particle;
import net.laraifox.particlesandbox.core.Vector2f;
import net.laraifox.particlesandbox.core.Wall;
import net.laraifox.particlesandbox.interfaces.ICollidable;

public class CollisionThread extends Thread {
	// public Object syncStartPhysicsObject;
	// public boolean syncStartPhysicsObjectNotified;
	// private Object syncFinishPhysicsObject;
	// private boolean syncFinishPhysicsObjectNotified;

	private int threadIndex;
	private ArrayList<Wall> walls;
	private Quadtree quadtree;

	// private boolean running;

	public CollisionThread(int threadIndex, ArrayList<Wall> walls, Quadtree quadtree) {
		super("Particle Collision Thread #" + threadIndex);

		// this.syncStartPhysicsObjectNotified = false;
		// this.syncStartPhysicsObject = new Object();
		// this.syncFinishPhysicsObjectNotified = false;
		// this.syncFinishPhysicsObject = new Object();

		this.threadIndex = threadIndex;
		this.walls = walls;
		this.quadtree = quadtree;
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

		ArrayList<ICollidable> possibleCollidables = new ArrayList<ICollidable>();
		ArrayList<Particle> collidingParticles = new ArrayList<Particle>();

		for (int i = threadIndex; i < walls.size(); i += GameManager.THREAD_COUNT) {
			Wall wall = walls.get(i);
			Line2D.Float wallLine = wall.getLine2D();

			possibleCollidables = quadtree.retrieve(wall);

			for (ICollidable collidable : possibleCollidables) {
				Particle particle = (Particle) collidable;

				if (particle.getVelocityLine2D().intersectsLine(wallLine)) {
					Vector2f v = particle.velocity;
					Vector2f n = new Vector2f(wall.getNormal().normalize());
					n.setX(Math.abs(n.getX()));
					n.setY(Math.abs(n.getY()));

					float dot = (float) Vector2f.dot(v, n);

					Vector2f r = Vector2f.subtract(v, Vector2f.scale(n, 2.0f * dot)).scale(0.5f);

					particle.velocity.set(r);
					
					collidingParticles.add(particle);
				}
			}

		}

		// syncFinishPhysicsObjectNotified = true;
		// synchronized (syncFinishPhysicsObject) {
		// syncFinishPhysicsObject.notifyAll();
		// }
		// }
	}
}
