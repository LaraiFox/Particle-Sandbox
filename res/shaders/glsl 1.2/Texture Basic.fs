#version 120

uniform sampler2D diffuse;

varying vec2 vTexCoord;

void main() {
	gl_FragColor = gl_Color * texture2D(diffuse, vTexCoord);
}