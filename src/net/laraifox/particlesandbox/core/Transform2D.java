package net.laraifox.particlesandbox.core;

public class Transform2D {
	private Vector2f translation;
	private float rotation;

	public Transform2D() {
		this(new Vector2f(), 0.0f);
	}

	public Transform2D(Vector2f translation) {
		this(translation, 0.0f);
	}

	public Transform2D(Vector2f translation, float rotation) {
		this.translation = translation;
		this.rotation = rotation;
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
}
