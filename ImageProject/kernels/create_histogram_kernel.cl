__kernel void histogram(__global const int * source,
						__global int * result) {
						
	int i = get_global_id(0);

	int value = source[i];
	
	result[value]++;
}