__kernel void histogram(__global const int * source,
						const int height, const int width,
						__local int * tile, const int K,
						__global int * result) {
	
	__local int local_histogram[256];
	const int SIZE = 256;
	int col = get_local_id(0);
	int row = get_local_id(1);
	
	int source_corner_col = get_global_id(0) * K;					
	int source_corner_row = get_global_id(1) * K;
	int index = (row * K) + col;
	int the_row = (source_corner_row + row);
	int the_col = (source_corner_col + col);
	
	if(the_row <= width | the_col <= height) {
		int source_index = (source_corner_row + row) * SIZE + (source_corner_col + col);
		tile[index] = source[source_index];
	}
	barrier(CLK_LOCAL_MEM_FENCE);
	
		
		int value = tile[index];
		local_histogram[value] ++;
		barrier(CLK_LOCAL_MEM_FENCE);
	}
	
	const int modified_height = height / K;
	const int modified_width = width / K;
	const int modified_row = get_global_id(1) / K;
	const int modified_col = get_global_id(0) / K; 
	
	int tile_index = (modified_row * modified_width) + modified_col;
	if(row == (K - 1) && col == (K - 1)) {
		for(int i = 0; i < 256; i ++) {
			result[(tile_index * 256) + i] = local_histogram[i];	
		}
	}
}