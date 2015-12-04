__kernel void red_eye(__global const int * templateValues, 
					  __global const int * imageValues,
				      const int height, const int width, 
				      const int templateHeight, 
				      const int templateWidth,
				      __global int * result) {
	int row = get_global_id(0);
	
	int col = get_global_id(1); 
	
	int index = (row * width) + col;
	float numerator = 0;
	float denominator = 0;
	float denominatorOne = 0;
	float denominatorTwo = 0;
    int valueCount = 0;
    for(int x = col - (width / 2); x < col + (width / 2) - 1; x ++) {
		for(int y = row - (height / 2); y < row + (height / 2) - 1; y ++) {
			if((x >= 0 && x < width) && (y >= 0 && y < height)) {
				valueCount += imageValues[index];
			}
		}
	}
	float f = valueCount / templateValues.length;
	float firstPart = 0;
    float secondPart = 0;
	int rowCount = 0;
	int colCount = 0;
	for(int x = row - (templateWidth/2); x < row + (templateWidth/2); x ++) {
		for(int y = height - (templateHeight/2); y < height + (templateHeight/2); y ++) {
			int templateIndex = (rowCount * templateWidth) + colCount;
			int pictureIndex = (y * width) + x; 
			if((x >= 0 && x < width) && (y >=0 && y < height)) {
				firstPart = (templateValues[templateIndex] - t);
				secondPart = (imageValues[pictureIndex] - f);
				numerator += firstPart * secondPart;
				denominatorOne += firstPart * firstPart;
				denominatorTwo += secondPart * secondPart;
			}
			colCount ++;
		}
		rowCount ++;
		colCount = 0;
	}
	denominator = (Math.sqrt(denominatorOne) * Math.sqrt(denominatorTwo));
	result[index] = (numerator / denominator);			      
}