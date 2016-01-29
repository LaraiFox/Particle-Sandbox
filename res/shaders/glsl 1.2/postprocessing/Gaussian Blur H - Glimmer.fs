#version 120

#define M_PI 3.141592653589793238462643383279

varying vec2 vTexCoord;

uniform sampler2D u_texture;

float gaussian(float x, float b, float c) {
	float top = (x - b) * (x - b);
	float bot = (c * c) * 2.0;

	return (1.0 / (c * sqrt(2.0 * M_PI))) * exp(-(top / bot));
}

void main() {
    vec4 sum = vec4(0.0);

    vec2 tc = vTexCoord;

    int blur = 9; 
    float width = 2.0 * blur + 1; 

    float hstep = 0.00078125;
	
	for (int i = -blur; i <= blur; i++) {
		sum += texture2D(u_texture, vec2(tc.x + (i * hstep), tc.y)) * gaussian(i, 0, blur);
	}

    // sum += texture2D(u_texture, vec2(tc.x - 4.0*blur*hstep, tc.y)) * 0.0162162162;
    // sum += texture2D(u_texture, vec2(tc.x - 3.0*blur*hstep, tc.y)) * 0.0540540541;
    // sum += texture2D(u_texture, vec2(tc.x - 2.0*blur*hstep, tc.y)) * 0.1216216216;
    // sum += texture2D(u_texture, vec2(tc.x - 1.0*blur*hstep, tc.y)) * 0.1945945946;

    // sum += texture2D(u_texture, vec2(tc.x, tc.y)) * 0.2270270270;

    // sum += texture2D(u_texture, vec2(tc.x + 1.0*blur*hstep, tc.y)) * 0.1945945946;
    // sum += texture2D(u_texture, vec2(tc.x + 2.0*blur*hstep, tc.y)) * 0.1216216216;
    // sum += texture2D(u_texture, vec2(tc.x + 3.0*blur*hstep, tc.y)) * 0.0540540541;
    // sum += texture2D(u_texture, vec2(tc.x + 4.0*blur*hstep, tc.y)) * 0.0162162162;

    gl_FragColor = vec4(sum * 3.0);
}