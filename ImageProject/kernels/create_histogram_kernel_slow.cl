__kernel void histogram_slow(__global const int * source,
						__global int * result) {
						
	int i = get_global_id(0);

	int value = source[i];
	
	atomic_add( &(result[value]), 1);
}