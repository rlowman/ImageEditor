__kernel void create_mask(__global const int * source,
						  const int height, const int width,
						  __global int * mask) {
	
	const int ALPHA_MASK = 0xff000000;
	const int ALPHA_OFFSET = 24;
	const int RED_MASK = 0x00ff0000;
	const int RED_OFFSET = 16;
	const int GREEN_MASK = 0x0000ff00;
	const int GREEN_OFFSET = 8;
	const int BLUE_MASK = 0x000000ff;
	const int BLUE_OFFSET = 0;

	int index = get_global_id(0);
	
	int col = index % width;
	int row = index / width;

	int pixel = source[index];

	int a = (pixel & ALPHA_MASK) >> ALPHA_OFFSET;
	
	int transparent	= (0 << ALPHA_OFFSET);				  
	int black = (0xff << ALPHA_OFFSET) |
				(0 << RED_OFFSET) |
		 	    (0 << GREEN_OFFSET) |
				(0 << BLUE_OFFSET);
			
	int white = (0xff << ALPHA_OFFSET) |
				(255 << RED_OFFSET) |
		 	    (255 << GREEN_OFFSET) |
				(255 << BLUE_OFFSET);
				
	int red = (0xff << ALPHA_OFFSET) |
			  (255 << RED_OFFSET) |
		 	  (0 << GREEN_OFFSET) |
			  (0 << BLUE_OFFSET);

	if( a == 0) {
		mask[index] = black;
	}
	else {
		mask[index] = white;
		int left = 999;
		int right = 999;
		int above = 999;
		int below = 999;
		if(col > 0) {
			left = (source[index-1] & ALPHA_MASK) >> ALPHA_OFFSET;
		}
		if(col < width - 1) {
			right = (source[index+1] & ALPHA_MASK) >> ALPHA_OFFSET;
		}
		if(row > 0) {
			above = (source[index - width] & ALPHA_MASK) >> ALPHA_OFFSET;
		}
		if(row < height - 1) {
			below = (source[index + width] & ALPHA_MASK) >> ALPHA_OFFSET;
		}
		if(left == 0 || right == 0 || above == 0 || below == 0) {
			mask[index] = red;
		}
	}
}