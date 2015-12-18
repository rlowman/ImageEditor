__kernel void initial_guess(__global const int * mask,
					__global const int * red_source,
					__global const int * green_source,
					__global const int * blue_source,
					__global const int * red_target,
					__global const int * green_target,
					__global const int * blue_target,
					__global float * red_result,
					__global float * green_result,
					__global float * blue_result) {
	
	const int ALPHA_MASK = 0xff000000;
	const int ALPHA_OFFSET = 24;
	const int RED_MASK = 0x00ff0000;
	const int RED_OFFSET = 16;
	const int GREEN_MASK = 0x0000ff00;
	const int GREEN_OFFSET = 8;

	int i = get_global_id(0);

	int pixel = mask[i];

	int r = (pixel & RED_MASK) >> RED_OFFSET;
	int g = (pixel & GREEN_MASK) >> GREEN_OFFSET;
	
	float red_guess = (float) red_target[i];
	float green_guess = (float) green_target[i];
	float blue_guess = (float) blue_target[i];
	
	if(r > 0) {
		if(g > 0) {
			red_guess = (float) red_source[i];
			green_guess = (float) green_source[i];
			blue_guess = (float) blue_source[i];
		}
	}
	red_result[i] = red_guess;
	green_result[i] = green_guess;
	blue_result[i] = blue_guess;
}