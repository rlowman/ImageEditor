__kernel void collection_combine_kernel(__global const int * collection,
										const int height, const int width,
										__global int * result) {
	
	int i = get_global_id(0);
	
	int value = 0;
	 
	for(int count = 0; count < height / 16; count ++) {
		int index = (count * (width/ 16)) + i;
		value += collection[index];
	}								
	
	result[i] = value;	
}	 