__kernel void color_adjust(__global const int * source,
						   __global int * new_raster) {

	const int ALPHA_OFFSET = 24;
	const int RED_OFFSET = 16;
	const int GREEN_OFFSET = 8;
	const int BLUE_OFFSET = 0;
	
	int i = get_global_id(0);
						 
	int gray = source[i];					 
	
	int resultColor = (0xff << ALPHA_OFFSET) |
					  (gray << RED_OFFSET) |
					  (gray << GREEN_OFFSET) |
					  (gray << BLUE_OFFSET);
					  
	new_raster[i] = resultColor;
}