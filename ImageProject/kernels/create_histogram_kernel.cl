__kernel void histogram(__global const int * source,
						const int height, const int width,
						__local int * tile,
						__global int * result) {
	
	const int K = 16;
	__local int local_histogram[256];
	const int SIZE = 256;
	int col = get_local_id(0);
	int row = get_local_id(1);
	
	int source_corner_col = get_global_id(0) * K;					
	int source_corner_row = get_global_id(1) * K;
	int index = (row * K) + col;
	int the_row = (source_corner_row + row);
	int the_col = (source_corner_col + col);
	
	int source_index = (the_row * SIZE) + the_col;
	tile[index] = source[source_index];
	barrier(CLK_LOCAL_MEM_FENCE);
	
	if(the_row < height & the_col < width) {  
		int value = tile[index];
		local_histogram[value] ++;
	}	
	barrier(CLK_LOCAL_MEM_FENCE);
	
	int tile_index = get_group_id(0);
	
	result[(tile_index * 256) + index] = local_histogram[index];
}