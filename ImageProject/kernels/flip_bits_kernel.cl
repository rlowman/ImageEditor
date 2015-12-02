__kernel void flip(__global const int * source,
				   __globabl int * result) {
	int i = get_global_id(0);
	
	int value = source[i];
	
	int setter = 0;
	
	if(value == 0) {
		setter = 1;
	}
	
	result[i] = setter;
}
	