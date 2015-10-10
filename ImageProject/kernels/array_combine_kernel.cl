__kernel void
combine_kernel (__global int * new_raster,
				__global const int * red_blurred,
				__global const int * green_blurred,
				__global const int * blue_blurred)
{
	const int ALPHA_OFFSET = 24;
	const int RED_OFFSET = 16;
	const int GREEN_OFFSET = 8;
	const int BLUE_OFFSET = 0;
	
	int i = get_global_id(0);
	
	int a = 0xff;
	int r = red_blurred[i];
	int g = green_blurred[i];
	int b = blue_blurred[i];

	int resultColor = (a << ALPHA_OFFSET) |
					  (r << RED_OFFSET) |
					  (g << GREEN_OFFSET) |
					  (b << BLUE_OFFSET);
				  
	new_raster[i] = resultColor;
}