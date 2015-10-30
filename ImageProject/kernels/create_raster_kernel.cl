__kernel void create_raster(__global const int * source,
							__global const int * histogram,
							__global int * raster,
							
	int i = get_global_id(0);
	
	int pixel = source[i];
	
	raster[i] = histogram[pixel];
}