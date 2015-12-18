__kernel void seam_combine(__global const float * redChannel,
						   __global const float * greenChannel,
						   __global const float * blueChannel,
						   __global int * raster) {
						   
	const int ALPHA_OFFSET = 24;
	const int RED_OFFSET = 16;
	const int GREEN_OFFSET = 8;
	const int BLUE_OFFSET = 0;
	
	int i = get_global_id(0);
	
	int a = 0xff;
	int r = (int) redChannel[i];
	int g = (int) greenChannel[i];
	int b = (int) blueChannel[i];

	int resultColor = (a << ALPHA_OFFSET) |
					  (r << RED_OFFSET) |
					  (g << GREEN_OFFSET) |
					  (b << BLUE_OFFSET);
				  
	raster[i] = resultColor;
}