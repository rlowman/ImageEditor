__kernel void compute(__global const int * mask,
					  __global const int * guess,
					  __global const int * target,
					  __global const int * source,
					  const int height, const int width,
					  __global float * result) {
	
	const int RED_MASK = 0x00ff0000;
	const int RED_OFFSET = 16;
	const int GREEN_MASK = 0x0000ff00;
	const int GREEN_OFFSET = 8;
	
	int i = get_global_id(0);
	
	int col = i % width;
	int row = i / width;
	
	int pixel = mask[i];
	
	int red = (pixel & RED_MASK) >> RED_OFFSET;
	int green = (pixel & GREEN_MASK) >> GREEN_OFFSET;
	
	float value = guess[i];
	if(red > 0 && green > 0) {
		float a = 0; 
		float b = 0;
		float c = 0;
		float d = 4;
		
		float s = source[i];
		
		if(col > 0) {
			int left = mask[i-1];
			int r = (left & RED_MASK) >> RED_OFFSET;
			int g = (left & GREEN_MASK) >> GREEN_OFFSET;
			if(r == 255) { 
				if(g == 255) {
					a += guess[i-1];
				}
				else {
					b += target[i-1];
				}
			}
			c += s - source[i-1];
		}
		else {
			d--;
		}
		
		if(col < width - 1) {
			int right = mask[i+1];
			int r = (right & RED_MASK) >> RED_OFFSET;
			int g = (right & GREEN_MASK) >> GREEN_OFFSET;
			if(r == 255) { 
				if(g == 255) {
					a += guess[i+1];
				}
				else {
					b += target[i+1];
				}
			}
			c += s - source[i+1]; 
		}
		else {
			d--;
		}
		
		if(row > 0) {
			int above = mask[i-width];
			int r = (above & RED_MASK) >> RED_OFFSET;
			int g = (above & GREEN_MASK) >> GREEN_OFFSET;
			if(r == 255) { 
				if(g == 255) {
					a += guess[i-width];
				}
				else {
					b += target[i-width];
				}
			}
			c += s - source[i-width];
		}
		else {
			d--;
		}
		
		if(row < height - 1) {
			int below = mask[i+width];
			int r = (below & RED_MASK) >> RED_OFFSET;
			int g = (below & GREEN_MASK) >> GREEN_OFFSET;
			if(r == 255) { 
				if(g == 255) {
					a += guess[i+width];
					c += s - source[i-1];
				}
				else {
					b += target[i+width];
				}
			}
			c += s - source[i+ width];
		}
		else {
			d--;
		}
		
		value = (a+b+c)/d;
	}
	result[i] = value;
	
}