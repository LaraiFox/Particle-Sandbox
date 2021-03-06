package net.laraifox.particlesandbox.core;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

public class Vector2f {
	public static final int ELEMENT_COUNT = 2;
	public static final int ELEMENT_SIZE = 4;
	public static final int VECTOR_SIZE = 8;

	private float x, y;

	public Vector2f() {
		this.x = 0;
		this.y = 0;
	}

	public Vector2f(float s) {
		this.x = s;
		this.y = s;
	}

	public Vector2f(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vector2f(Vector2f v) {
		this.x = v.x;
		this.y = v.y;
	}

	public Vector2f add(float x, float y) {
		this.x += x;
		this.y += y;
		return this;
	}

	public Vector2f add(Vector2f v) {
		this.x += v.x;
		this.y += v.y;
		return this;
	}

	public Vector2f subtract(float x, float y) {
		this.x -= x;
		this.y -= y;
		return this;
	}

	public Vector2f subtract(Vector2f v) {
		this.x -= v.x;
		this.y -= v.y;
		return this;
	}

	public Vector2f multiply(float x, float y) {
		this.x *= x;
		this.y *= y;
		return this;
	}

	public Vector2f multiply(Vector2f v) {
		this.x *= v.x;
		this.y *= v.y;
		return this;
	}

	public Vector2f scale(float scalar) {
		this.x *= scalar;
		this.y *= scalar;
		return this;
	}

	public float dot(Vector2f v) {
		return this.x * v.x + this.y * v.y;
	}

	public Vector2f cross() {
		float cx = this.y;
		float cy = -this.x + 0.0f;
		this.x = cx;
		this.y = cy;
		return this;
	}

	public Vector2f rotate(float theta) {
		theta = (float) Math.toRadians(theta);
		float sin = (float) Math.sin(theta);
		float cos = (float) Math.cos(theta);
		float rx = (this.x * cos) - (this.y * sin);
		float ry = (this.x * sin) + (this.y * cos);
		this.x = rx;
		this.y = ry;
		return this;
	}

	public Vector2f negate() {
		this.x = -this.x;
		this.y = -this.y;
		return this;
	}

	public Vector2f normalize() {
		float length = length();
		if (length == 0.0f)
			return this;

		this.x /= length;
		this.y /= length;
		return this;
	}

	public Vector2f absolute() {
		this.x = Math.abs(this.x);
		this.y = Math.abs(this.y);
		return this;
	}

	public Vector2f project(Vector2f v) {
		return scale(v, dot(normalize(v)));
	}

	public Vector2f reverse() {
		float x_ = this.y;
		float y_ = this.x;
		this.x = x_;
		this.y = y_;
		return this;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Vector2f) {
			Vector2f _other = (Vector2f) other;

			return (this.x == _other.getX() && this.y == _other.getY());
		}

		return false;
	}

	public float length() {
		return (float) Math.sqrt(x * x + y * y);
	}

	public float lengthSq() {
		return x * x + y * y;
	}

	public float distanceTo(float x, float y) {
		float dx = x - this.x;
		float dy = y - this.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	public float distanceTo(Vector2f v) {
		float dx = v.x - this.x;
		float dy = v.y - this.y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	public float distanceSqTo(float x, float y) {
		float dx = x - this.x;
		float dy = y - this.y;
		return dx * dx + dy * dy;
	}

	public float distanceSqTo(Vector2f v) {
		float dx = v.x - this.x;
		float dy = v.y - this.y;
		return dx * dx + dy * dy;
	}

	public FloatBuffer toFloatBuffer() {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(ELEMENT_COUNT);
		buffer.put(x);
		buffer.put(y);
		return buffer;
	}

	@Override
	public String toString() {
		return "[" + x + ", " + y + "]";
	}

	public org.lwjgl.util.vector.Vector2f toLWJGLVector2f() {
		return new org.lwjgl.util.vector.Vector2f(this.x, this.y);
	}

	public static Vector2f Zero() {
		return new Vector2f(0.0f, 0.0f);
	}

	public static Vector2f One() {
		return new Vector2f(1.0f, 1.0f);
	}

	public static Vector2f PositiveX() {
		return new Vector2f(1.0f, 0.0f);
	}

	public static Vector2f NegativeX() {
		return new Vector2f(-1.0f, 0.0f);
	}

	public static Vector2f PositiveY() {
		return new Vector2f(0.0f, 1.0f);
	}

	public static Vector2f NegativeY() {
		return new Vector2f(0.0f, -1.0f);
	}

	public static Vector2f add(Vector2f u, Vector2f v) {
		float x = u.getX() + v.getX();
		float y = u.getY() + v.getY();
		return new Vector2f(x, y);
	}

	public static Vector2f subtract(Vector2f u, Vector2f v) {
		float x = u.getX() - v.getX();
		float y = u.getY() - v.getY();
		return new Vector2f(x, y);
	}

	public static Vector2f multiply(Vector2f u, Vector2f v) {
		float x = u.getX() * v.getX();
		float y = u.getY() * v.getY();
		return new Vector2f(x, y);
	}

	public static Vector2f scale(Vector2f u, float scalar) {
		float x = u.getX() * scalar;
		float y = u.getY() * scalar;
		return new Vector2f(x, y);
	}

	public static float dot(Vector2f u, Vector2f v) {
		return u.getX() * v.getX() + u.getY() * v.getY();
	}

	public static Vector2f cross(Vector2f u) {
		float x = u.getY();
		float y = -u.getX() + 0.0f;
		return new Vector2f(x, y);
	}

	public static Vector2f rotate(Vector2f u, float theta) {
		theta = (float) Math.toRadians(theta);
		float sin = (float) Math.sin(theta);
		float cos = (float) Math.cos(theta);
		float x = (u.getX() * cos) - (u.getY() * sin);
		float y = (u.getX() * sin) + (u.getY() * cos);
		return new Vector2f(x, y);
	}

	public static Vector2f negate(Vector2f u) {
		float x = -u.getX();
		float y = -u.getY();
		return new Vector2f(x, y);
	}

	public static Vector2f normalize(Vector2f u) {
		float length = (float) Math.sqrt(u.getX() * u.getX() + u.getY() * u.getY());
		if (length == 0.0f)
			return u;

		float x = u.getX() / length;
		float y = u.getY() / length;
		return new Vector2f(x, y);
	}

	public static Vector2f absolute(Vector2f u) {
		float x = Math.abs(u.x);
		float y = Math.abs(u.y);
		return new Vector2f(x, y);
	}

	public static Vector2f project(Vector2f u, Vector2f v) {
		return Vector2f.scale(v, u.dot(normalize(v)));
	}

	public static Vector2f reverse(Vector2f u) {
		return new Vector2f(u.getY(), u.getX());
	}

	public static float length(Vector2f u) {
		return (float) Math.sqrt(u.getX() * u.getX() + u.getY() * u.getY());
	}

	public static float lengthSq(Vector2f u) {
		return u.getX() * u.getX() + u.getY() * u.getY();
	}

	public static float distanceBetween(Vector2f u, Vector2f v) {
		float dx = v.getX() - u.getX();
		float dy = v.getY() - u.getY();
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	public static float distanceSqBetween(Vector2f u, Vector2f v) {
		float dx = v.getX() - u.getX();
		float dy = v.getY() - u.getY();
		return dx * dx + dy * dy;
	}

	public static Vector2f sum(Vector2f... u) {
		float x = 0.0f;
		float y = 0.0f;
		for (int i = 0; i < u.length; i++) {
			x += u[i].getX();
			y += u[i].getY();
		}
		return new Vector2f(x, y);
	}

	public Vector2f get() {
		return new Vector2f(this);
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public Vector2f set(float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	public Vector2f set(Vector2f v) {
		this.x = v.getX();
		this.y = v.getY();
		return this;
	}

	public Vector2f setX(float x) {
		this.x = x;
		return this;
	}

	public Vector2f setY(float y) {
		this.y = y;
		return this;
	}
}
