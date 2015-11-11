__kernel void histogram(__global const int * source,
						const int height, const int width,
						__global int * result) {
	
	int index = get_global_id(0);					
	__local int final[10];
	__local int coarse[10];
	int i = get_local_id(0);
	final[i] = source[index]/10;
	coarse[i] = final[i]/10;
	barrier(CLK_LOCAL_MEM_FENCE);
	
	int value = tile[row][col];
	local_histogram[value] ++;
	barrier(CLK_LOCAL_MEM_FENCE);
	
	const int modified_height = height / 2;
	const int modified_width = width / 2;
	const int modified_row = get_global_id(1) / 2;
	const int modified_col = get_global_id(0) / 2; 
	
	int tile_index = (modified_row * modified_width) + modified_col;
	if((row == 1 && col == 1) {
		for(int i = 0; i < 256; i ++) {
			result[(tile_index * 256) + i)] = local_histogram[i];	
		}
	}
}