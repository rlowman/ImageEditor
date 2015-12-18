__kernel void
blur_kernel (__global const float * filter,
			 __global const int * red_channel,
	 		 __global const int * green_channel,
			 __global const int * blue_channel,
			 __global int * red_blurred,
	 		 __global int * green_blurred,
			 __global int * blue_blurred, 
			 const int blurWidth,
			 const int width,
			 const int height)
{
	
	int index = get_global_id(0);
	int row = index / width;
	int col = index % width;
	
	float redBlur = 0;
	float blueBlur = 0;
	float greenBlur = 0;
	int space = blurWidth / 2;
	for(int filterRow = -1 * space; filterRow <= space; filterRow ++) {
		for(int filterCol = -1 * space; filterCol <= space; filterCol ++) {
			int filterIndex = ((filterRow + space) * blurWidth) + (filterCol + space);
			int r = row - filterRow;
			int c = col - filterCol;
			r = max(0, min(r, height));
			c = max(0, min(c, width));
			int i = (r * width) + c;
			redBlur += red_channel[i] * filter[filterIndex];
			greenBlur += green_channel[i] * filter[filterIndex];
			blueBlur += blue_channel[i] * filter[filterIndex];
		}
	}
	red_blurred[index] = (int)redBlur;
	green_blurred[index] = (int)greenBlur;
	blue_blurred[index] = (int)blueBlur;
}
