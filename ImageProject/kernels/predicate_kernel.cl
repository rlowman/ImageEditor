__kernel void predicate(__global const int * source,
						const int bit,
						__global int * result) {
	int i = get_globabl_id(0);
	
	int current = source[i];
	
	int temp = current << bit;
	
	int setter = 0;
	
	if(temp & 1) {
		setter = 1;
	}
	
	result[i] = setter;
}