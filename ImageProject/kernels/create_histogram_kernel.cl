__kernel void histogram(__global const int * source,
						const int height, const int width,
						const int image_height, const int image_width,
						__global int * result) {
	
	const int k = 16;
	__local int local_histogram[256];
	int paddedWidth = width / 16;
	int paddedHeight = height / 16;
	
	int imageCol = get_global_id(0);
	int imageRow = get_global_id(1);
	int imageIndex = (imageRow * width) + imageCol;
	
	int value;
	int temp;
	if(imageCol < image_width && imageRow < image_height) {
		value = source[imageIndex];
		temp = local_histogram[value] + 1;
	}	
	barrier(CLK_LOCAL_MEM_FENCE);
	if(imageCol < image_width && imageRow < image_height) {
		local_histogram[value] = temp;
	}	
	barrier(CLK_LOCAL_MEM_FENCE);
		
	int tileCol = get_group_id(0);
	int tileRow = get_group_id(1);
	int tile_index = (tileRow * paddedWidth) + tileCol;
	
	int theCol = get_local_id(0);
	int theRow = get_local_id(1);
	int i = (theRow * k) + theCol;
	
	result[(tile_index * 256) + i] = local_histogram[i];
}