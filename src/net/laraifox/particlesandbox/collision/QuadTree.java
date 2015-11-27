package net.laraifox.particlesandbox.collision;

import java.util.ArrayList;
import java.util.Iterator;

import net.laraifox.particlesandbox.interfaces.ICollider;

public class QuadTree {
	/** The default maximum number of objects that can be present in a node before the node will attempt to subdivide. */
	public static final int DEFAULT_MAX_OBJECTS = 8;
	/** The default maximum number of subdivisions that the {@link QuadTree} can have before nodes will stop subdividing. */
	public static final int DEFAULT_MAX_LEVELS = 8;

	private int level;
	private AABBCollider bounds;
	private ArrayList<ICollider> objects;
	private QuadTree[] nodes;
	private boolean hasSplit;

	private int maxObjects, maxLevels;

	public QuadTree(AABBCollider bounds) {
		this(0, bounds, DEFAULT_MAX_OBJECTS, DEFAULT_MAX_LEVELS);
	}

	public QuadTree(AABBCollider bounds, int maxObjects, int maxLevels) {
		this(0, bounds, maxObjects, maxLevels);
	}

	private QuadTree(int level, AABBCollider bounds, int maxObjects, int maxLevels) {
		this.level = level;
		this.bounds = bounds;
		this.objects = new ArrayList<ICollider>();
		this.nodes = new QuadTree[4];
		this.hasSplit = false;

		int x = (int) bounds.getX();
		int y = (int) bounds.getY();
		int halfWidth = (int) (bounds.getWidth() / 2.0);
		int halfHeight = (int) (bounds.getHeight() / 2.0);
		int childLevel = level + 1;

		nodes[0] = new QuadTree(childLevel, new AABBCollider(x, y, halfWidth, halfHeight), maxObjects, maxLevels);
		nodes[1] = new QuadTree(childLevel, new AABBCollider(x + halfWidth, y, halfWidth, halfHeight), maxObjects, maxLevels);
		nodes[2] = new QuadTree(childLevel, new AABBCollider(x + halfWidth, y + halfHeight, halfWidth, halfHeight), maxObjects, maxLevels);
		nodes[3] = new QuadTree(childLevel, new AABBCollider(x, y + halfHeight, halfWidth, halfHeight), maxObjects, maxLevels);

		this.maxObjects = maxObjects;
		this.maxLevels = maxLevels;
	}

	public void clear() {
		objects.clear();

		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i] != null) {
				nodes[i].clear();
				nodes[i] = null;
			}
		}
	}

	public void insert(ICollider collider) {
		if (hasSplit) {
			int index = getIndex(collider);
			if (index != -1) {
				nodes[index].insert(collider);
				return;
			}
		}

		objects.add(collider);

		if (!hasSplit && objects.size() > maxObjects && level < maxLevels) {
			hasSplit = true;

			Iterator<ICollider> iterator = objects.iterator();
			while (iterator.hasNext()) {
				ICollider currentCollider = iterator.next();
				int index = getIndex(currentCollider);
				if (index != -1) {
					nodes[index].insert(currentCollider);
					iterator.remove();
				}
			}
		}
	}

	/**
	 * 
	 * @param result
	 *            -
	 * @param rectangle
	 * @return
	 */
	public ArrayList<ICollider> retrieve(ICollider collider) {
		ArrayList<ICollider> result = new ArrayList<ICollider>();

		return retrieve(result, collider);
	}

	private ArrayList<ICollider> retrieve(ArrayList<ICollider> result, ICollider collider) {
		int index = getIndex(collider);
		if (index != -1 && hasSplit) {
			nodes[index].retrieve(result, collider);
		}

		result.addAll(objects);

		return result;
	}

	// /**
	// * Initializes the four child nodes contained within the current node.
	// */
	// private void split() {
	// int x = (int) bounds.getX();
	// int y = (int) bounds.getY();
	// int halfWidth = (int) (bounds.getWidth() / 2.0);
	// int halfHeight = (int) (bounds.getHeight() / 2.0);
	// int childLevel = level + 1;
	//
	// nodes[0] = new QuadTree(childLevel, new AABBCollider(x, y, halfWidth, halfHeight), maxObjects, maxLevels);
	// nodes[1] = new QuadTree(childLevel, new AABBCollider(x + halfWidth, y, halfWidth, halfHeight), maxObjects, maxLevels);
	// nodes[2] = new QuadTree(childLevel, new AABBCollider(x + halfWidth, y + halfHeight, halfWidth, halfHeight), maxObjects, maxLevels);
	// nodes[3] = new QuadTree(childLevel, new AABBCollider(x, y + halfHeight, halfWidth, halfHeight), maxObjects, maxLevels);
	// }

	/**
	 * Returns the index of the child {@link QuadTree} which fully contains the given rectangle. If no child fully contains the rectangle then a value of -1 is
	 * returned.
	 * 
	 * @param rectangle
	 *            - the rectangle which this function finds the owner of.
	 * @return the index of the child {@link QuadTree}.
	 */
	private int getIndex(ICollider collider) {
		int childIndex = -1;
		for (int i = 0; i < nodes.length; i++) {
			if (collider.collides(nodes[i].bounds).isColliding()) {
				if (childIndex != -1) {
					return -1;
				}

				childIndex = i;
			}
		}

		return childIndex;

		// double xCenter = bounds.getX() + (bounds.getWidth() / 2.0);
		// double yCenter = bounds.getY() + (bounds.getHeight() / 2.0);
		//
		// if (collider.getX() + collider.getWidth() < xCenter) {
		// if (collider.getY() + collider.getHeight() < yCenter) {
		// return 0;
		// } else if (collider.getY() > yCenter) {
		// return 3;
		// }
		// } else if (collider.getX() > xCenter) {
		// if (collider.getY() + collider.getHeight() < yCenter) {
		// return 1;
		// } else if (collider.getY() > yCenter) {
		// return 2;
		// }
		// }
		//
		// return -1;
	}
}
