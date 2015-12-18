__kernel void scatter(__global const int * source,
					  __global const int * predicate,
					  const int bit,
					  __global const int * predicate_scan,
					  __global const int * not_predicate_scan
					  const int predicate_max, 
					  __global int * result) {
					  
	int i = get_global_id(0);
	
	int current = source[i];
	int value = predicate[i];
	int setter = 0;
	
	if(value == 1) {
		setter = not_predicate_scan[i];
	}
	else {
		setter = predicate_scan[i] + predicate_max;
	}
	
	result[setter] = current;
}