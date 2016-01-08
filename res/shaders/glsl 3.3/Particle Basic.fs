#version 330

layout (location = 0) out glFragColor;

void main() {
	glFragColor = gl_Color;
}