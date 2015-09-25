__kernel void
parallel_grayscale (__global const int * a,
					__global int * result )
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

int alpha = (pixel & ALPHA_MASK) >> ALPHA_OFFSET;
int r = (pixel & RED_MASK) >> RED_OFFSET;
int g = (pixel & GREEN_MASK) >> GREEN_OFFSET;
int b = (pixel & BLUE_MASK) >> BLUE_OFFSET;

int gray = (int)(r * .299 + g * .587 + b * .114);

int resultColor = (alpha << ALPHA_OFFSET) |
				  (gray << RED_OFFSET) |
				  (gray << GREEN_OFFSET) |
				  (gray << BLUE_OFFSET);
				  
result[i] = resultColor;
}