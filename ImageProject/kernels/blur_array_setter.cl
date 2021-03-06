__kernel void
array_setter_kernel (__global const int * a,
					__global int * red_result,
					__global int * green_result,
					__global int * blue_result )
{
const int ALPHA_MASK = 0xff000000;
const int ALPHA_OFFSET = 24;
const int RED_MASK = 0x00ff0000;
const int RED_OFFSET = 16;
const int GREEN_MASK = 0x0000ff00;
const int GREEN_OFFSET = 8;
const int BLUE_MASK = 0x000000ff;
const int BLUE_OFFSET = 0;

int i = get_global_id(0);

int pixel = a[i];

int r = (pixel & RED_MASK) >> RED_OFFSET;
int g = (pixel & GREEN_MASK) >> GREEN_OFFSET;
int b = (pixel & BLUE_MASK) >> BLUE_OFFSET;

red_result[i] = r;
green_result[i] = g;
blue_result[i] = b;
}