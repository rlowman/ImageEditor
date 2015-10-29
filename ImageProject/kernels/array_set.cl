__kernel void set(const int setValue,
				  __global int * result) {

	int i = get_global_id(0);
	
	result[i] = setValue;
}