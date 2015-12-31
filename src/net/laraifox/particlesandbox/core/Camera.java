package net.laraifox.particlesandbox.core;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

public class Camera {
	private Matrix4f projectionMatrix;
	private Vector2f position;

	public Camera(Matrix4f projectionMatrix, Vector2f position) {
		this.projectionMatrix = projectionMatrix;
		this.position = position;
	}

	public void translate(float x, float y) {
		position.x += x;
		position.y += y;
	}

	public Matrix4f getViewProjectionMatrix() {
		Matrix4f viewMatrix = new Matrix4f();
		viewMatrix.setIdentity();
		viewMatrix.translate(position.negate(null));
		
		return Matrix4f.mul(viewMatrix, projectionMatrix, null);
	}

	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

	public void setProjectionMatrix(Matrix4f projectionMatrix) {
		this.projectionMatrix = projectionMatrix;
	}

	public Vector2f getPosition() {
		return position;
	}

	public void setPosition(Vector2f position) {
		this.position = position;
	}
}
