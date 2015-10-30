__kernel void split(__global const int * sourceData,
					__global int * result) {
	
	const int RED_MASK = 0x00ff0000;
    const int RED_OFFSET = 16;
	
	int i = get_global_id(0);
	
	int pixel = sourceData[i];
	
	int r = (pixel & RED_MASK) >> RED_OFFSET;

	result[i] = r;
}