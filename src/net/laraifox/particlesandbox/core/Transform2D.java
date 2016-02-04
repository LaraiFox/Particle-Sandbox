package net.laraifox.particlesandbox.core;

public class Transform2D {
	private Vector2f translation;
	private float rotation;
	private Vector2f scale;

	public Transform2D() {
		this(Vector2f.Zero(), 0.0f, Vector2f.One());
	}

	public Transform2D(Vector2f translation) {
		this(translation, 0.0f, Vector2f.One());
	}

	public Transform2D(Vector2f translation, float rotation) {
		this(translation, rotation, Vector2f.One());
	}

	public Transform2D(Vector2f translation, float rotation, Vector2f scale) {
		this.translation = translation;
		this.rotation = rotation;
		this.scale = scale;
	}

	public Transform2D(Transform2D transform) {
		this.translation = transform.translation;
		this.rotation = transform.rotation;
	}

	public Vector2f getTranslation() {
		return translation;
	}

	public float getRotation() {
		return rotation;
	}

	public Vector2f getScale() {
		return scale;
	}
}
