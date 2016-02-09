#version 120

attribute vec2 attribPosition;
attribute vec4 attribColor;

uniform mat4 uViewPojectionMatrix;

varying vec4 vColor;

void main() {
	gl_Position = uViewPojectionMatrix * vec4(attribPosition, 0.0, 1.0);
	vColor = gl_Color * attribColor.r;
}