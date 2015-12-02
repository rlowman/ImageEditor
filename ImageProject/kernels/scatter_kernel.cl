__kernel void scatter(__global const int * source,
					  const int bit,
					  __global const int * predicate,
					  __global const int * predicate_scan,
					  __global const int * not_predicate_scan) {
					  
	int i = get_global_id(0);
	
	int current = source[i];
	
	int temp = current >> bit;
	
	int setter = 0;
	
	if(temp & 1) {
		predicate_scan
	}
	else {
	
	}
}