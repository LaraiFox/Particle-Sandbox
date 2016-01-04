package net.laraifox.particlesandbox.core;

import org.lwjgl.util.vector.Matrix4f;

public class Camera {
	private Matrix4f projectionMatrix;
	private Vector2f position;

	public Camera(Matrix4f projectionMatrix, Vector2f position) {
		this.projectionMatrix = projectionMatrix;
		this.setPosition(position);
	}

	public void translate(float x, float y) {
		position.add(x, y);
	}

	public Matrix4f getViewProjectionMatrix() {
		Matrix4f viewMatrix = new Matrix4f();
		viewMatrix.setIdentity();
		viewMatrix.translate(Vector2f.negate(position).toLWJGLVector2f());

		return Matrix4f.mul(projectionMatrix, viewMatrix, null);
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
		this.position = new Vector2f(position.getX(), position.getY());
	}

	public void setX(float x) {
		this.position.setX(x);
	}

	public void setY(float y) {
		this.position.setY(y);
	}
}
