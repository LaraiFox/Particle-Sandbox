#version 120

attribute vec2 attribPosition;

varying vec2 vTexCoord;

void main() {
    vTexCoord = gl_MultiTexCoord0.st;
	
	gl_FrontColor = gl_Color;
	gl_Position = vec4(attribPosition, 0.0, 1.0);
}