__kernel void parallel_sepia(__global const int * a,
					__global int * result ) {
	
	const int SEPIA_DEPTH = 20;
	const int SEPIA_INTENSITY = 30;
	
	const int ALPHA_MASK = 0xff000000;
	const int ALPHA_OFFSET = 24;
	const int RED_MASK = 0x00ff0000;
	const int RED_OFFSET = 16;
	const int GREEN_MASK = 0x0000ff00;
	const int GREEN_OFFSET = 8;
	const int BLUE_MASK = 0x000000ff;
	const int BLUE_OFFSET = 0;
	
	int i = get_global_id(0);
	
	int pixel = a[i];
	
	int alpha = (pixel & ALPHA_MASK) >> ALPHA_OFFSET;
	int r = (pixel & RED_MASK) >> RED_OFFSET;
	int g = (pixel & GREEN_MASK) >> GREEN_OFFSET;
	int b = (pixel & BLUE_MASK) >> BLUE_OFFSET;
	
	int average = (int)(r * .299 + g * .587 + b * .114);
	
	int red = average + (SEPIA_DEPTH * 2);
	int blue = average - SEPIA_INTENSITY;
	int green = average + SEPIA_DEPTH;
	red = max(0, min(red, 255));
	green = max(0, min(green, 255));
	blue = max(0, min(blue, 255));
	
	int resultColor = (alpha << ALPHA_OFFSET) |
					  (red << RED_OFFSET) |
					  (green << GREEN_OFFSET) |
					  (blue << BLUE_OFFSET);
					  
	result[i] = resultColor;
}