__kernel void scan(__global int * source,
				   const int size,
				   __global int * result) {

	int modified = 8;
	
	int i = get_local_id(0);
	
	__local int arrayCopy[256];
	arrayCopy[i] = source[i];
	barrier(CLK.LOCAL_MEM_FENCE);
	
	for(int d = 0; d < modified - 1; d ++) {
		arrayCopy[i + (1 << (d + 1)) - 1] = arrayCopy[i + (1 << d) - 1] + arrayCopy[i + (1 << (d + 1)) - 1];
	}
	
	barrier(CLK.LOCAL_MEM_FENCE);
	
	source[size - 1] = arrayCopy[size - 1];
	for(int d = modified - 1; d >= 0; d --) {
		int t = arrayCopy[i + (1 << d) - 1];
		arrayCopy[i + (1 << d) - 1] = arrayCopy[i + (1<<(d+1)) - 1];
		arrayCopy[i + (1 << (d + 1)) - 1] = t + arrayCopy[i + (1<<(d+1)) - 1]; 
	} 
	
	barrier(CLK.LOCAL_MEM_FENCE);
	
	result[i] = arrayCopy[i];
}