__kernel void
blur_kernel (__global const float * filter,
			 __global const int * red_channel,
	 		 __global const int * green_channel,
			 __global const int * blue_channel,
			 __global int * red_blurred,
	 		 __global int * green_blurred,
			 __global int * blue_blurred, 
			 const int width,
			 const int height)
{
	
	int index = get_global_id(0);
	int row = index / width;
	int col = index % width;
	
	float redBlur = 0;
	float blueBlur = 0;
	float greenBlur = 0;
	for(int filterRow = 0; filterRow < 5; filterRow ++) {
		for(int filterCol = 0; filterCol < 5; filterCol ++) {
			int filterIndex = (filterRow * 5) + filterCol;
			int r = row - filterRow - 2;
			int c = col - filterCol - 2;
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
