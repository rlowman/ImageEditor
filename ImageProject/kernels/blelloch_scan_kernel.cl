__kernel void scan2(__global int * source,
				   const int size) {

	int modified = 8;
	
	int i = get_local_id(0);
	
	for(int d = 0; d < modified - 1; d ++) {
		if(i % (1 << (d+1)) == 0) {
			source[i + (1 << (d + 1)) - 1] = source[i + (1 << d) - 1] + source[i + (1 << (d + 1)) - 1];
		}
		barrier(CLK_GLOBAL_MEM_FENCE);
	}
	
	int temp = source[size - 1];
	source[size - 1] = 0;
	for(int d = modified - 1; d >= 0; d --) {
		if(i % (1 << (d+1)) == 0) {
			int t = source[i + (1 << d) - 1];
			source[i + (1 << d) - 1] = source[i + (1<<(d+1)) - 1];
			source[i + (1 << (d + 1)) - 1] = t + source[i + (1<<(d+1)) - 1]; 
		}
		barrier(CLK_GLOBAL_MEM_FENCE);
	}
	source[size - 1] = temp; 
}