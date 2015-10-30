__kernel void adjust(__global const int * cuf,
					 __global const int * cufeq,
					 __global int * new_histogram) {
					 
	int i = get_global_id(0);