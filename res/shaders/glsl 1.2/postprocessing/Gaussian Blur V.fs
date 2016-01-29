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

    int blur = 1;
    float width = 2.0 * blur + 1; 

    float vstep = 0.00078125;
	
	// for (int i = -blur; i <= blur; i++) {
		// sum += texture2D(u_texture, vec2(tc.x, tc.y + (i * vstep))) * gaussian(i, 0, blur);
	// }

    sum += texture2D(u_texture, vec2(tc.x, tc.y - 6.0*blur*vstep)) * 0.0014594594;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 5.0*blur*vstep)) * 0.0048648648;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 4.0*blur*vstep)) * 0.0162162162;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 3.0*blur*vstep)) * 0.0540540541;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 2.0*blur*vstep)) * 0.0716216216;
    sum += texture2D(u_texture, vec2(tc.x, tc.y - 1.0*blur*vstep)) * 0.1245945946;

    sum += texture2D(u_texture, vec2(tc.x, tc.y)) * 0.2270270270;

    sum += texture2D(u_texture, vec2(tc.x, tc.y + 1.0*blur*vstep)) * 0.1245945946;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 2.0*blur*vstep)) * 0.0716216216;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 3.0*blur*vstep)) * 0.0540540541;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 4.0*blur*vstep)) * 0.0162162162;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 5.0*blur*vstep)) * 0.0048648648;
    sum += texture2D(u_texture, vec2(tc.x, tc.y + 6.0*blur*vstep)) * 0.0014594594;

    gl_FragColor = vec4(sum.rgb * 3.0, 1.0);
}