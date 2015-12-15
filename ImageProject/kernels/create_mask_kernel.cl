__kernel void create_mask(__global const int * source,
						  __global int * mask) {
	
	const int ALPHA_MASK = 0xff000000;
	const int ALPHA_OFFSET = 24;
	const int RED_MASK = 0x00ff0000;
	const int RED_OFFSET = 16;
	const int GREEN_MASK = 0x0000ff00;
	const int GREEN_OFFSET = 8;
	const int BLUE_MASK = 0x000000ff;
	const int BLUE_OFFSET = 0;

	int i = get_global_id(0);

	int pixel = source[i];

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

	if( a== 0) {
		mask[i] = black;
	}
	else {
		mask[i] = white;
	}
}