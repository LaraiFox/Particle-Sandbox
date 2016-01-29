#version 120

attribute vec2 positionAttrib;

varying vec2 vTexCoord;

void main() {
    vTexCoord = gl_MultiTexCoord0.st;
    gl_Position = vec4(positionAttrib, 0.0, 1.0);
}