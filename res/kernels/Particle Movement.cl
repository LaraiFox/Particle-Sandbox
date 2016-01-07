kernel void main(global float2 *position, global float2 *velocity, global const unsigned int *PARTICLE_COUNT, global const float *FRAME_DELTA) {
	const unsigned int ID = get_global_id(0);
	
	const float PARTICLE_RADIUS = 0.5f;
	const float RHO = 0.01f;
	const float DRAG_COEFFICIENT = 0.47f;
	const float PARTICLE_MIN_SPEED = 0.000663f;
<<<<<<< HEAD
	const float PARTICLE_REFERENCE_AREA = (float) (M_PI * (PARTICLE_RADIUS * PARTICLE_RADIUS));
=======
	const float PARTICLE_REFERENCE_AREA = (float) (MATH_PI * (PARTICLE_RADIUS * PARTICLE_RADIUS));
	
	//universalGravity(ID, PARTICLE_COUNT, position, velocity);
>>>>>>> 9efd1c90acefde055b23dcb4dcd1c6bb67cce766
	
	float2 currentVelocity = velocity[ID];
	float velocityLength = length(currentVelocity);
	
	float2 relativeVelocity = -currentVelocity;

	float dragForce = 0.5f * RHO * velocityLength * velocityLength * DRAG_COEFFICIENT * PARTICLE_REFERENCE_AREA;
	
	currentVelocity += normalize(relativeVelocity) * dragForce;
	
	velocity[ID] = currentVelocity;
	
	position[ID] += currentVelocity;
	
	// position[ID] += velocity[ID];

	// if (length(currentVelocity) <= PARTICLE_MIN_SPEED) {
		// velocity[ID] = (float2) (0, 0);
	// } else {
		// velocity[ID] = currentVelocity;
	// }
}