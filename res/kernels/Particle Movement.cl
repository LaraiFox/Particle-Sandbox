void universalGravity(const unsigned int ID, const global unsigned int *PARTICLE_COUNT, global float2 *position, global float2 *velocity) {
	const float GRAVITATIONAL_CONSTANT = 0.00006673f;
	const float PARTICLE_MASS = 100.0f;
	const float PARTICLE_COLLISION_RADIUS = 5.0f;
	
	float2 iPosition = position[ID];
	
	for (unsigned int j = ID + 1; j < PARTICLE_COUNT[0]; j++) {
		float2 jPosition = position[j];
	
		float particleDistanceRadius = length(iPosition - jPosition);
		particleDistanceRadius = max(particleDistanceRadius, (PARTICLE_COLLISION_RADIUS));

		float force = (GRAVITATIONAL_CONSTANT * PARTICLE_MASS * PARTICLE_MASS) / (particleDistanceRadius * particleDistanceRadius);

		float2 direction = normalize(jPosition - iPosition);

		velocity[ID] += (direction * (force / PARTICLE_MASS));
		velocity[j] += (direction * (-force / PARTICLE_MASS));
	}
}

kernel void main(global float2 *position, global float2 *velocity, const global unsigned int *PARTICLE_COUNT, const global float *FRAME_DELTA) {
	const unsigned int ID = get_global_id(0);
	const unsigned int SIZE = get_global_size(0);
	
	const float MATH_PI = 3.1415926535;
	const float PARTICLE_RADIUS = 0.5f;
	const float RHO = 0.01f;
	const float DRAG_COEFFICIENT = 0.47f;
	const float PARTICLE_MIN_SPEED = 0.000663f;
	const float PARTICLE_REFERENCE_AREA = (float) (MATH_PI * (PARTICLE_RADIUS * PARTICLE_RADIUS));
	
	// universalGravity(ID, PARTICLE_COUNT, position, velocity);
	
	float2 currentVelocity = velocity[ID];
	float velocityLength = length(currentVelocity);
	
	position[ID] = position[ID] + currentVelocity;
	
	float2 relativeVelocity = -currentVelocity;

	float dragForce = 0.5f * RHO * velocityLength * velocityLength * DRAG_COEFFICIENT * PARTICLE_REFERENCE_AREA;
	
	float2 dragVector = normalize(relativeVelocity) * dragForce;
	
	// velocity[ID] = currentVelocity + dragVector;
	
	// position[ID] += velocity[ID];

	// if (length(currentVelocity + dragVector) <= PARTICLE_MIN_SPEED) {
		// velocity[ID] = (float2) (0, 0);
	// } else {
		velocity[ID] = currentVelocity + dragVector;
	// }
}