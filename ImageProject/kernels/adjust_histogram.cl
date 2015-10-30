__kernel void adjust(__global const int * cuf,
					 __global const int * cufeq,
					 __global int * new_histogram) {
					 
	int i = get_global_id(0);
	
	int compare = cuf[i];
	
	int bestIndex = 0;
	int best = abs(compare - cufeq[0]);
	
	for(int index = 1; index < 256; index ++) {
		int test = abs(compare - cufeq[index]);
		
		if(test <= best) {
			bestIndex = index;
			best = test;
		}
	}
	
	new_histogram[i] = bestIndex;
}