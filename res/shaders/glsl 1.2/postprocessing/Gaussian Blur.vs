#version 120

//combined projection and view matrix
uniform mat4 uViewProjectionMatrix;

//"in" attributes from our SpriteBatch
attribute vec2 positionAttrib;
attribute vec2 texCoordAttrib;

//"out" varyings to our fragment shader
varying vec2 vTexCoord;

void main() {
    vTexCoord = texCoordAttrib;
    gl_Position = uViewProjectionMatrix * vec4(positionAttrib, 0.0, 1.0);
}