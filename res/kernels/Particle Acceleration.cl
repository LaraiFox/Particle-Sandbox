#define ZERO_PRECISION 0.000001f
#define MAX_LIMIT 1000000000000000.0f

#define MOUSE_FORCE_POS_X 0
#define MOUSE_FORCE_POS_Y 1
#define MOUSE_FORCE_STRENGTH 2
#define MOUSE_FORCE_THRESHHOLD 3
#define MOUSE_FORCE_LIMIT 4

#define GRAVITY_WELL_COUNT 0
#define GRAVITY_WELL_POS_X 1
#define GRAVITY_WELL_POS_Y 2
#define GRAVITY_WELL_STRENGTH 3
#define GRAVITY_WELL_THRESHHOLD 4
#define GRAVITY_WELL_LIMIT 5
#define GRAVITY_WELL_COMPONENT_COUNT 5

float checkRange(float value, float minLimit, float maxLimit) {
	return step(minLimit, value) * (1.0f - step(maxLimit, value));
}

kernel void main(global float2 *position, global float2 *velocity, global float *mouseForce, global float *gravityWell) {
	const uint ID = get_global_id(0);
	
	float2 deltaVelocity = (float2) (0.0f, 0.0f);
	
		/***   MOUSE FORCE CALCULATIONS   ***/
	// if (mouseForce[MOUSE_FORCE_STRENGTH] != 0.0f) {
	float2 vecToMouse = (float2) (mouseForce[MOUSE_FORCE_POS_X], mouseForce[MOUSE_FORCE_POS_Y]) - position[ID];
	
	float distanceToMouse = length(vecToMouse);
	// if (mouseForce[MOUSE_FORCE_LIMIT] != 0.0f && distanceToMouse > mouseForce[MOUSE_FORCE_LIMIT]) {
		// distanceToMouse = 0.0f;
	// }
	
	distanceToMouse *= (1.0f - checkRange(distanceToMouse, ZERO_PRECISION, mouseForce[MOUSE_FORCE_LIMIT]) * checkRange(mouseForce[MOUSE_FORCE_LIMIT], ZERO_PRECISION, MAX_LIMIT));
	
	float distanceBelowThreshold = checkRange(distanceToMouse, mouseForce[MOUSE_FORCE_THRESHHOLD], MAX_LIMIT);
	distanceToMouse = distanceToMouse * distanceBelowThreshold + mouseForce[MOUSE_FORCE_THRESHHOLD] * (1.0f - distanceBelowThreshold);

	float distanceIsZero = 1.0f - checkRange(distanceToMouse, -ZERO_PRECISION, ZERO_PRECISION);
	float divisor = (1.0f - distanceIsZero + distanceToMouse * distanceIsZero);
	deltaVelocity += vecToMouse / divisor * mouseForce[MOUSE_FORCE_STRENGTH] / divisor;
	
	// if (distanceToMouse != 0.0f) {
		// if (mouseForce[MOUSE_FORCE_THRESHHOLD] != 0.0f && distanceToMouse < mouseForce[MOUSE_FORCE_THRESHHOLD]) {
			// distanceToMouse = mouseForce[MOUSE_FORCE_THRESHHOLD];
		// }
		
		// deltaVelocity += normalize(vecToMouse) * (mouseForce[MOUSE_FORCE_STRENGTH] / distanceToMouse);
	// }
	// }
	
	for (int i = 0; i < gravityWell[GRAVITY_WELL_COUNT]; i++) {
		int currentIndex = i * GRAVITY_WELL_COMPONENT_COUNT;
		
		if (gravityWell[currentIndex + GRAVITY_WELL_STRENGTH] != 0.0f) {
			float2 vecToGravityWell = (float2) (gravityWell[currentIndex + GRAVITY_WELL_POS_X], gravityWell[currentIndex + GRAVITY_WELL_POS_Y]) - position[ID];
			
			float distanceToGravityWell = length(vecToGravityWell);
			if (gravityWell[currentIndex + GRAVITY_WELL_LIMIT] != 0.0f && distanceToGravityWell > gravityWell[currentIndex + GRAVITY_WELL_LIMIT]) {
				distanceToGravityWell = 0.0f;
			}
			
			if (distanceToGravityWell != 0.0f) {
				if (gravityWell[currentIndex + GRAVITY_WELL_THRESHHOLD] != 0.0f && distanceToGravityWell < gravityWell[currentIndex + GRAVITY_WELL_THRESHHOLD]) {
					distanceToGravityWell = gravityWell[currentIndex + GRAVITY_WELL_THRESHHOLD];
				}
				
				deltaVelocity += normalize(vecToGravityWell) * (gravityWell[currentIndex + GRAVITY_WELL_STRENGTH] / distanceToGravityWell);
			}
		}
	}
	
	velocity[ID] += deltaVelocity;
}