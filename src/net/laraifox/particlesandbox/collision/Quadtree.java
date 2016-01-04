package net.laraifox.particlesandbox.collision;

import java.util.ArrayList;

import net.laraifox.particlesandbox.core.Vector2f;
import net.laraifox.particlesandbox.interfaces.ICollidable;
import net.laraifox.particlesandbox.interfaces.ICollider;

public class Quadtree {
	/** The default maximum number of objects that can be present in a node before the node will attempt to subdivide. */
	public static final int DEFAULT_MAX_OBJECTS = 8;
	/** The default maximum number of subdivisions that the {@link Quadtree} can have before nodes will stop subdividing. */
	public static final int DEFAULT_MAX_LEVELS = 8;

	private int currentDepth;
	private AABBCollider bounds;
	private Vector2f center;
	private ArrayList<ICollidable> objects;
	private Quadtree[] childNodes;
	private boolean hasSplit;

	private int maxObjectCount, maxDepth;

	public Quadtree(AABBCollider bounds) {
		this(0, bounds, DEFAULT_MAX_OBJECTS, DEFAULT_MAX_LEVELS);
	}

	public Quadtree(AABBCollider bounds, int maxObjects, int maxLevels) {
		this(0, bounds, maxObjects, maxLevels);
	}

	private Quadtree(int level, AABBCollider bounds, int maxObjects, int maxLevels) {
		this.currentDepth = level;
		this.bounds = bounds;
		this.center = new Vector2f(bounds.getX() + bounds.getWidth() / 2.0f, bounds.getY() + bounds.getHeight() / 2.0f);
		this.objects = new ArrayList<ICollidable>();
		this.hasSplit = false;

		this.maxObjectCount = maxObjects;
		this.maxDepth = maxLevels;
	}

	public void clear() {
		objects.clear();
		hasSplit = false;
		childNodes = null;
	}

	public boolean insert(ICollidable collidable) {
		ICollider collider = collidable.getCollider();
		if (collider.getCollision(bounds).isColliding()) {
			return this.insert(collidable, collider, collider.getMin(), collider.getMax()) != null;
		}

		return false;
	}

	private Quadtree insert(ICollidable collidable, ICollider collider, Vector2f colliderMin, Vector2f colliderMax) {
		if (childNodes != null) {
			int index = (colliderMax.getX() < this.center.getX() ? 0 : colliderMin.getX() > this.center.getX() ? 1 : -10)
				+ (colliderMax.getY() < this.center.getY() ? 0 : colliderMin.getY() > this.center.getY() ? 2 : -10);

			if (index < 0 || index > 3) {
				objects.add(collidable);
			} else {
				return childNodes[index].insert(collidable, collider, colliderMin, colliderMax);
			}

			return this;
		}

		if (!hasSplit && this.currentDepth < maxDepth && objects.size() + 1 > maxObjectCount) {
			this.split();

			ArrayList<ICollidable> tempList = new ArrayList<ICollidable>(objects);
			objects.clear();
			for (ICollidable object : tempList) {
				ICollider objectCollider = object.getCollider();
				this.insert(object, objectCollider, objectCollider.getMin(), objectCollider.getMax());
			}

			this.insert(collidable, collider, colliderMin, colliderMax);
		} else {
			objects.add(collidable);
		}

		return this;

		// int index = getIndex(collider);
		// if (index != -1) {
		// nodes[index].insert(collider);
		// return;
		// }
		// }
		//
		// objects.add(collider);
		//
		// if (objects.size() > maxObjects && level < maxLevels) {
		// if (nodes[0] == null) {
		// split();
		// }
		//
		// Iterator<ICollider> iterator = objects.iterator();
		// while (iterator.hasNext()) {
		// ICollider currentObject = iterator.next();
		// int index = getIndex(currentObject);
		// if (index != -1) {
		// nodes[index].insert(currentObject);
		// iterator.remove();
		// }
		// }
		// }
	}

	private void split() {
		float x = bounds.getX();
		float y = bounds.getY();
		float width = bounds.getWidth();
		float height = bounds.getHeight();
		int childLevel = currentDepth + 1;

		float halfWidth = width / 2.0f;
		float halfHeight = height / 2.0f;

		this.hasSplit = true;
		this.childNodes = new Quadtree[4];
		childNodes[0] = new Quadtree(childLevel, new AABBCollider(x, y, halfWidth, halfHeight), maxObjectCount, maxDepth);
		childNodes[1] = new Quadtree(childLevel, new AABBCollider(x + halfWidth, y, halfWidth, halfHeight), maxObjectCount, maxDepth);
		childNodes[2] = new Quadtree(childLevel, new AABBCollider(x, y + halfHeight, halfWidth, halfHeight), maxObjectCount, maxDepth);
		childNodes[3] = new Quadtree(childLevel, new AABBCollider(x + halfWidth, y + halfHeight, halfWidth, halfHeight), maxObjectCount, maxDepth);
	}

	/**
	 * 
	 * @param result
	 *            -
	 * @param rectangle
	 * @return
	 */
	public ArrayList<ICollidable> retrieve(ICollidable collidable) {
		return retrieve(collidable.getCollider());
	}

	public ArrayList<ICollidable> retrieve(ICollider collider) {
		ArrayList<ICollidable> result = new ArrayList<ICollidable>();

		return retrieve(result, collider);
	}

	private ArrayList<ICollidable> retrieve(ArrayList<ICollidable> result, ICollider collider) {
		if (collider.getCollision(bounds).isColliding()) {
			if (childNodes != null) {
				for (Quadtree child : childNodes) {
					child.retrieve(result, collider);
				}
			}

			result.addAll(objects);
		}

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

	public ArrayList<ICollidable> getList(int[] indices) {
		return this.getList(0, indices);
	}

	private ArrayList<ICollidable> getList(int index, int[] indices) {
		if (index >= indices.length) {
			return this.objects;
		} else if (indices[index] >= 0 && indices[index] < 4 && childNodes[indices[index]] != null) {
			return childNodes[indices[index]].getList(index + 1, indices);
		}

		return null;
	}

	public ArrayList<Float> getVertices() {
		ArrayList<Float> result = new ArrayList<Float>();

		Vector2f min = bounds.getMin();
		Vector2f max = bounds.getMax();

		addColor(result, 1.0f);
		result.add(min.getX());
		result.add(min.getY());

		addColor(result, 1.0f);
		result.add(max.getX());
		result.add(min.getY());

		addColor(result, 1.0f);
		result.add(max.getX());
		result.add(min.getY());

		addColor(result, 1.0f);
		result.add(max.getX());
		result.add(max.getY());

		addColor(result, 1.0f);
		result.add(max.getX());
		result.add(max.getY());

		addColor(result, 1.0f);
		result.add(min.getX());
		result.add(max.getY());

		addColor(result, 1.0f);
		result.add(min.getX());
		result.add(max.getY());

		addColor(result, 1.0f);
		result.add(min.getX());
		result.add(min.getY());

		this.getVertices(result);

		return result;
	}

	private ArrayList<Float> getVertices(ArrayList<Float> result) {
		if (childNodes != null) {
			float halfWidth = bounds.getWidth() / 2.0f;
			float halfHeight = bounds.getHeight() / 2.0f;

			float depthColor = (float) Math.pow(1.0f - ((float) currentDepth) / (float) maxDepth, 1.0);

			addColor(result, depthColor);
			result.add(center.getX() - halfWidth);
			result.add(center.getY());

			addColor(result, depthColor);
			result.add(center.getX() + halfWidth);
			result.add(center.getY());

			addColor(result, depthColor);
			result.add(center.getX());
			result.add(center.getY() - halfHeight);

			addColor(result, depthColor);
			result.add(center.getX());
			result.add(center.getY() + halfHeight);

			for (Quadtree child : childNodes) {
				child.getVertices(result);
			}
		}

		return result;
	}

	private ArrayList<Float> addColor(ArrayList<Float> result, float color) {
		result.add(1.0f);
		result.add(1.0f);
		result.add(1.0f);
		result.add(color);

		return result;
	}

	public void test() {
		System.out.println("Objects In Quadtree = " + testChild());
	}

	private int testChild() {
		if (childNodes != null) {
			return childNodes[0].testChild() + childNodes[1].testChild() + childNodes[2].testChild() + childNodes[3].testChild() + objects.size();
		}

		return objects.size();
	}
}
