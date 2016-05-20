#version 120

#define M_PI 3.141592653589793238462643383279

varying vec2 vTexCoord;

uniform sampler2D u_texture;

void main() {
    vec4 sum = vec4(0.0);

    vec2 tc = vTexCoord;

    int blur = 1; 
    float width = 2.0 * blur + 1; 

    float hstep = 0.00078125;

    // sum += texture2D(u_texture, vec2(tc.x - 6.0*blur*hstep, tc.y)) * 0.0014594594;
    // sum += texture2D(u_texture, vec2(tc.x - 5.0*blur*hstep, tc.y)) * 0.0048648648;
    // sum += texture2D(u_texture, vec2(tc.x - 4.0*blur*hstep, tc.y)) * 0.0162162162;
    // sum += texture2D(u_texture, vec2(tc.x - 3.0*blur*hstep, tc.y)) * 0.0540540541;
    sum += texture2D(u_texture, vec2(tc.x - 2.0*blur*hstep, tc.y)) * 0.0716216216;
    sum += texture2D(u_texture, vec2(tc.x - 1.0*blur*hstep, tc.y)) * 0.1245945946;

    sum += texture2D(u_texture, vec2(tc.x, tc.y)) * 0.2270270270;

    sum += texture2D(u_texture, vec2(tc.x + 1.0*blur*hstep, tc.y)) * 0.1245945946;
    sum += texture2D(u_texture, vec2(tc.x + 2.0*blur*hstep, tc.y)) * 0.0716216216;
    // sum += texture2D(u_texture, vec2(tc.x + 3.0*blur*hstep, tc.y)) * 0.0540540541;
    // sum += texture2D(u_texture, vec2(tc.x + 4.0*blur*hstep, tc.y)) * 0.0162162162;
    // sum += texture2D(u_texture, vec2(tc.x + 5.0*blur*hstep, tc.y)) * 0.0048648648;
    // sum += texture2D(u_texture, vec2(tc.x + 6.0*blur*hstep, tc.y)) * 0.0014594594;

    gl_FragColor = vec4(sum.rgb * 3.0, 1.0);
}