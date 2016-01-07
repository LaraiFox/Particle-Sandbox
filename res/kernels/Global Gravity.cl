kernel void main(global float2 *position, global float2 *velocity, global const int *PARTICLE_COUNT, global const float *FRAME_DELTA) {
	const unsigned int ID = get_global_id(0);

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