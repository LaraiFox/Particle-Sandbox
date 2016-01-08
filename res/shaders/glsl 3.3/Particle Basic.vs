#version 330

layout (location = 0) in vec2 attribPosition;

uniform mat4 uViewPojectionMatrix;

void main() {
	gl_Position = uViewPojectionMatrix * vec4(attribPosition, 0.0, 1.0);
	gl_FrontColor = gl_Color;
}