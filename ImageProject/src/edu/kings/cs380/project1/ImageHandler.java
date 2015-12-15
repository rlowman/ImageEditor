package edu.kings.cs380.project1;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

/**
 * Handles the image algorithms necessary for the project.
 * 
 * @author Robert Lowman
 * @version 9.8.2015
 */
public class ImageHandler {
	
	/**The frame to draw images to.*/
	private ImagePanel frame;
	
	/**The current image being drawn to the frame.*/
	private BufferedImage currentImage;
	
	/**The current file being handled.*/
	private File currentFile;
	
	/**The id of the selected device.*/
	private cl_device_id selectedDevice;
	
	/**The id of the selected platform.*/
	private cl_platform_id selectedPlatform;
	
	/**
	 * Constructor for the ImageHandler class.
	 * 
	 * @param theFrame the frame to draw the image too
	 * @param theId the id of the selected device
	 * @param thePlatform the id of the selected platform
	 */
	public ImageHandler(ImagePanel theFrame, cl_device_id theId, cl_platform_id thePlatform){
		currentImage = null;
		frame = theFrame;
		currentFile = null;
		selectedDevice = theId;
		selectedPlatform = thePlatform;
	}
	
	/**
	 * Loads an image from a file.
	 *  
	 * @param theFile the file to load to the program
	 * @throws IOException if the file cannot be found
	 * (Modified from http://kings.mrooms2.net/mod/resource/view.php?id=147952)
	 */
	public void loadFile(File theFile) throws IOException {
		BufferedImage ri = ImageIO.read(theFile);
		currentFile = theFile;
		BufferedImage tempImage = new BufferedImage(ri.getWidth(), ri.getHeight(), 
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = tempImage.getGraphics();
		g.drawImage(ri, 0, 0, null);
		frame.setVisible(true);
		frame.setImage(tempImage);
		currentImage = tempImage;
	}
	
	/**
	 * Saves the changes made to the file.
	 * 
	 * @throws IOException if the image cannot be saved
	 * @return true if the file was saved false otherwise
	 * (Modified from http://kings.mrooms2.net/mod/resource/view.php?id=147952)
	 */
	public boolean saveFile() throws IOException {
		boolean returnValue = false;
		if(currentImage != null) {
			ImageIO.write(currentImage, "png", currentFile);
			returnValue = true;
		}
		return returnValue;
	}
	
	/**
	 * Saves the changes made to the file.
	 * 
	 * @param theFile the File to save the image to
	 * @return true if the file was saved, false otherwise
	 * @throws IOException if the image cannot be saved
	 * (Modified from http://kings.mrooms2.net/mod/resource/view.php?id=147952)
	 */
	public boolean saveAsFile(File theFile) {
		boolean returnValue = false;
		try {
			ImageIO.write(currentImage, "png", theFile);
			returnValue = true;
		} catch (IOException e) {
			returnValue = false;
		}
		return returnValue;
	}
	
	/**
	 * Takes an Image and returns it in grayscale.
	 * 
	 * @return true if the image was grayscaled, false otherwise
	 * (Modified from http://kings.mrooms2.net/mod/resource/view.php?id=147952)
	 */
	public boolean grayScale() {
		boolean returnValue = false;
		if(currentImage != null) {
			int width = currentImage.getWidth ();
			int height = currentImage.getHeight ();
			for(int row = 0 ; row < height ; row ++) {
				for (int column = 0; column < width; column ++) {
					Color c = new Color (currentImage.getRGB(column, row));
					int red = c. getRed();
					int green = c.getGreen();
					int blue = c. getBlue();
					int alpha = c.getAlpha();
					int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
					Color newColor = new Color(gray, gray, gray, alpha);
					currentImage.setRGB(column, row, newColor.getRGB());
				}
			}
			returnValue = true;
		}
		return returnValue;
	}
	
	/**
	 * Returns the image in grayscale using parallel programming.
	 * 
	 * @return the given image in grayscale
	 */
	public long grayScaleParallel() {
		long returnValue;
		if(currentImage == null){
			returnValue = -1;
		}
		else {
			//Initialize the context properties	
			cl_context_properties contextProperties = new cl_context_properties();
			contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, selectedPlatform);
					
			//Create a context for the selected device
			cl_context context = CL.clCreateContext(
			contextProperties, 1,
				new cl_device_id[]{selectedDevice},
				null, null, null);		
			
					
			//Create a command queue for the selected device
			cl_command_queue commandQueue = CL.clCreateCommandQueue(context, selectedDevice, 0, null);

			//Get Raster information for array
			WritableRaster sourceRaster = currentImage.getRaster();
			DataBuffer sourceDataBuffer = sourceRaster.getDataBuffer();
			DataBufferInt sourceBytes = (DataBufferInt)sourceDataBuffer;
			int sourceData[] = sourceBytes.getData();
			int n = sourceData.length;
			int[]result = new int[n];
					
			//Create pointers to arrays to give to kernel
			Pointer ptrArrayA = Pointer.to(sourceData);
			Pointer ptrResult = Pointer.to(result);
					
			cl_mem memArrayA = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_float * n, ptrArrayA, null);
			cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_float * n, ptrResult, null);
					
			//Create the program from the source code
			//Create the OpenCL kernel from the program
			String source = readFile("kernels/parallel_grayscale_kernel.cl");
			cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
					
			//Build the program
			CL.clBuildProgram(program, 0, null, null, null, null);
					
			//Create the kernel
			cl_kernel kernel = CL.clCreateKernel(program, "parallel_grayscale", null);
					
			//Set the arguments for the kernel
			CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memArrayA));
			CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memResult));
					
			//Set the work−item dimensions
			long[] globalWorkSize = new long[]{n};
			long[] localWorkSize = new long[]{1};
					
			//Execute the kernel
			long startTime = System.nanoTime();
			CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
					0, null, null);
			long runTime = System.nanoTime() - startTime;
	
					
			//Read the output data
			CL.clEnqueueReadBuffer(commandQueue, memResult, CL.CL_TRUE, 0, n * Sizeof.cl_float, 
					ptrResult, 0, null, null);
			
			//Write the results to the BufferedImage named result
			DataBufferInt resultDataBuffer = new DataBufferInt(result, result.length);
			Raster resultRaster = Raster.createRaster(currentImage.getSampleModel(), resultDataBuffer, new Point(0, 0));
			currentImage.setData(resultRaster);
					
			//Clean-up
			CL.clReleaseKernel(kernel);
			CL.clReleaseProgram(program);
			CL.clReleaseMemObject(memArrayA);
			CL.clReleaseMemObject(memResult);
			CL.clReleaseCommandQueue(commandQueue);
			CL.clReleaseContext(context);
			returnValue = runTime;
		}
		return returnValue;
	}
	
	/**
	 * Blurs the image using sequential computing.
	 * 
	 * @return the time in nanoseconds
	 */
	public long sequentialBlur() {
		long returnValue = -1;
		if(currentImage != null) {
			long startTime = System.nanoTime();
			int height = currentImage.getHeight();
			int width = currentImage.getWidth();
			int[] redValues = new int[height*width];
			int[] greenValues = new int[height*width];
			int[] blueValues = new int[height*width];
			int[] redBlurred = new int[height*width];
			int[] greenBlurred = new int[height*width];
			int[] blueBlurred = new int[height * width];
			int pixelCount = 0;
			for(int row = 0; row < height; row++) {
				for (int column = 0; column < width; column ++) {
					Color c = new Color(currentImage.getRGB(column, row));
					int red = c. getRed();
					int green = c.getGreen();
					int blue = c. getBlue();
					redValues[pixelCount] = red;
					greenValues[pixelCount] = green;
					blueValues[pixelCount] = blue;
					pixelCount++;
				}
			}
			double[][] filter = createBlurFilter(5, 2);
			for(int row = 0; row < height; row ++) {
				for(int col = 0; col < width; col ++) {
					int index = (row * width) + col;
					double redBlur = 0;
					double blueBlur = 0;
					double greenBlur = 0;
					for(int filterRow = 0; filterRow < 5; filterRow ++) {
						for(int filterCol = 0; filterCol < 5; filterCol ++) {
							int r = Math.abs(row - filterRow - 2);
							int c = Math.abs(col - filterCol - 2);
							r = Math.max(0, Math.min(r, height - 1));
							c = Math.max(0, Math.min(c, width - 1));
							int i = (r * width) + c;
							redBlur += redValues[i] * filter[filterRow][filterCol];
							greenBlur += greenValues[i] * filter[filterRow][filterCol];
							blueBlur += blueValues[i] * filter[filterRow][filterCol];
						}
					}
					redBlurred[index] = (int)redBlur;
					greenBlurred[index] = (int)greenBlur;
					blueBlurred[index] = (int)blueBlur;
				}
			}
			for(int theHeight = 0; theHeight < height; theHeight ++) {
				for(int theWidth = 0; theWidth < width; theWidth ++) {
					int index = (theHeight * width) + theWidth;

					int ALPHA_OFFSET = 24;
				    int RED_OFFSET = 16;
					int GREEN_OFFSET = 8;
					int BLUE_OFFSET = 0;	
					int resultColor = (0xff << ALPHA_OFFSET) |
									  (redBlurred[index] << RED_OFFSET) |
									  (greenBlurred[index] << GREEN_OFFSET) |
									  (blueBlurred[index] << BLUE_OFFSET);
					
					currentImage.setRGB(theWidth, theHeight, resultColor);
				}
			}
			returnValue = System.nanoTime() - startTime;
		}
		return returnValue;
	}
	
	/**
	 * Blurs the image using parallel computing.
	 * 
	 * @return the time in nanoseconds
	 */
	public long parallelBlur() {
		long returnValue = -1;
		if(currentImage != null) {
			//Initialize the context properties	
			cl_context_properties contextProperties = new cl_context_properties();
			contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, selectedPlatform);
					
			//Create a context for the selected device
			cl_context context = CL.clCreateContext(
			contextProperties, 1,
				new cl_device_id[]{selectedDevice},
				null, null, null);		
			
					
			//Create a command queue for the selected device
			cl_command_queue commandQueue = CL.clCreateCommandQueue(context, selectedDevice, 0, null);

			//Get Raster information for array
			WritableRaster sourceRaster = currentImage.getRaster();
			DataBuffer sourceDataBuffer = sourceRaster.getDataBuffer();
			DataBufferInt sourceBytes = (DataBufferInt)sourceDataBuffer;
			int sourceData[] = sourceBytes.getData();
			int n = sourceData.length;
			int[]redResult = new int[n];
			int[]greenResult = new int[n];
			int[]blueResult = new int[n];
			
			Pointer ptrArrayA = Pointer.to(sourceData);
			Pointer ptrRedResult = Pointer.to(redResult);
			Pointer ptrGreenResult = Pointer.to(greenResult);
			Pointer ptrBlueResult = Pointer.to(blueResult);
			
			cl_mem memArrayA = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_int * n, ptrArrayA, null);
			cl_mem memRedResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			cl_mem memGreenResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			cl_mem memBlueResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			
			String source = readFile("kernels/blur_array_setter.cl");
			cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
					
			CL.clBuildProgram(program, 0, null, null, null, null);
					
			cl_kernel kernel = CL.clCreateKernel(program, "array_setter_kernel", null);
					
			CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memArrayA));
			CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memRedResult));
			CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memGreenResult));
			CL.clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(memBlueResult));
			
			long[] globalWorkSize = new long[]{n};
			long[] localWorkSize = new long[]{1};
			
			//Execute the kernel
			long startTime = System.nanoTime();
			CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
					0, null, null);
				
			//Read the output data
			CL.clEnqueueReadBuffer(commandQueue, memRedResult, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
					ptrRedResult, 0, null, null);
			
			CL.clEnqueueReadBuffer(commandQueue, memGreenResult, CL.CL_TRUE, 0, n* Sizeof.cl_int,
					ptrGreenResult, 0, null, null);
			
			CL.clEnqueueReadBuffer(commandQueue, memBlueResult, CL.CL_TRUE, 0, n* Sizeof.cl_int,
					ptrBlueResult, 0, null, null);
			long runTime = System.nanoTime() - startTime;
			
			//Start of second kernel
			double[][] filter = createBlurFilter(5,2);
			float[] paramFilter = new float[25];
			int count = 0;
			for(int i = 0; i < filter.length; i ++) {
				for(int m = 0; m < filter[i].length; m ++) {
					paramFilter[count] = (float)filter[i][m];
					count ++;
				}
			}
			
			int[]redBlurred = new int[n];
			int[]blueBlurred = new int[n];
			int[]greenBlurred = new int[n];
			
			int[]height = new int[]{currentImage.getHeight()};
			int[]width = new int[]{currentImage.getWidth()};
			
			Pointer ptrFilter = Pointer.to(paramFilter);
			Pointer ptrRedBlurred = Pointer.to(redBlurred);
			Pointer ptrGreenBlurred = Pointer.to(greenBlurred);
			Pointer ptrBlueBlurred = Pointer.to(blueBlurred);
					
			cl_mem ptrFilterArray = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_float * n, ptrFilter, null);
			cl_mem memRedChannel = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL. CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_int * n, ptrRedResult, null);
			cl_mem memGreenChannel = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL. CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_int * n, ptrGreenResult, null);
			cl_mem memBlueChannel = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL. CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_int * n, ptrBlueResult, null);
			cl_mem memRedBlurredResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			cl_mem memGreenBlurredResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			cl_mem memBlueBlurredResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			
			String theSource = readFile("kernels/parallel_blur_kernel.cl");
			cl_program theProgram = CL.clCreateProgramWithSource(context, 1, new String[]{ theSource }, null, null);
					
			CL.clBuildProgram(theProgram, 0, null, null, null, null);
					
			cl_kernel blurKernel = CL.clCreateKernel(theProgram, "blur_kernel", null);
					
			CL.clSetKernelArg(blurKernel, 0, Sizeof.cl_mem, Pointer.to(ptrFilterArray));
			CL.clSetKernelArg(blurKernel, 1, Sizeof.cl_mem, Pointer.to(memRedChannel));
			CL.clSetKernelArg(blurKernel, 2, Sizeof.cl_mem, Pointer.to(memGreenChannel));
			CL.clSetKernelArg(blurKernel, 3, Sizeof.cl_mem, Pointer.to(memBlueChannel));
			CL.clSetKernelArg(blurKernel, 4, Sizeof.cl_mem, Pointer.to(memRedBlurredResult));
			CL.clSetKernelArg(blurKernel, 5, Sizeof.cl_mem, Pointer.to(memGreenBlurredResult));
			CL.clSetKernelArg(blurKernel, 6, Sizeof.cl_mem, Pointer.to(memBlueBlurredResult));
			CL.clSetKernelArg(blurKernel, 7, Sizeof.cl_int, Pointer.to(width));
			CL.clSetKernelArg(blurKernel, 8, Sizeof.cl_int, Pointer.to(height));
			
			long secondStartTime = System.nanoTime();
			CL.clEnqueueNDRangeKernel(commandQueue, blurKernel, 1, null, globalWorkSize, localWorkSize,
					0, null, null);
				
			//Read the output data
			CL.clEnqueueReadBuffer(commandQueue, memRedBlurredResult, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
					ptrRedBlurred, 0, null, null);
			
			CL.clEnqueueReadBuffer(commandQueue, memGreenBlurredResult, CL.CL_TRUE, 0, n* Sizeof.cl_int,
					ptrGreenBlurred, 0, null, null);
			
			CL.clEnqueueReadBuffer(commandQueue, memBlueBlurredResult, CL.CL_TRUE, 0, n* Sizeof.cl_int,
					ptrBlueBlurred, 0, null, null);
			runTime += System.nanoTime() - secondStartTime;
			
			int newRaster[] = new int[n];
			
			Pointer ptrRaster = Pointer.to(newRaster);
			
			cl_mem ptrRasterArray = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			
			cl_mem memRedBlurredChannel = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_int * n, ptrRedBlurred, null);
			
			cl_mem memGreenBlurredChannel = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_int * n, ptrGreenBlurred, null);
			
			cl_mem memBlueBlurredChannel = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_int * n, ptrBlueBlurred, null);
			
			String combineSource = readFile("kernels/array_combine_kernel.cl");
			cl_program combineProgram = CL.clCreateProgramWithSource(context, 1, new String[]{ combineSource }, null, null);
					
			CL.clBuildProgram(combineProgram, 0, null, null, null, null);
					
			cl_kernel combineKernel = CL.clCreateKernel(combineProgram, "combine_kernel", null);
					
			CL.clSetKernelArg(combineKernel, 0, Sizeof.cl_mem, Pointer.to(ptrRasterArray));
			CL.clSetKernelArg(combineKernel, 1, Sizeof.cl_mem, Pointer.to(memRedBlurredChannel));
			CL.clSetKernelArg(combineKernel, 2, Sizeof.cl_mem, Pointer.to(memGreenBlurredChannel));
			CL.clSetKernelArg(combineKernel, 3, Sizeof.cl_mem, Pointer.to(memBlueBlurredChannel));
			
			long thirdStartTime = System.nanoTime();
			CL.clEnqueueNDRangeKernel(commandQueue, combineKernel, 1, null, globalWorkSize, localWorkSize,
					0, null, null);
				
			//Read the output data
			CL.clEnqueueReadBuffer(commandQueue, ptrRasterArray, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
					ptrRaster, 0, null, null);
			runTime += System.nanoTime() - thirdStartTime;
			
			DataBufferInt resultDataBuffer = new DataBufferInt(newRaster, newRaster.length);
			Raster resultRaster = Raster.createRaster(currentImage.getSampleModel(), resultDataBuffer, new Point(0, 0));
			currentImage.setData(resultRaster);
			
			//Clean-up
			CL.clReleaseKernel(kernel);
			CL.clReleaseProgram(program);
			CL.clReleaseMemObject(memArrayA);
			CL.clReleaseMemObject(memRedResult);
			CL.clReleaseMemObject(memGreenResult);
			CL.clReleaseMemObject(memBlueResult);
			CL.clReleaseKernel(combineKernel);
			CL.clReleaseProgram(combineProgram);
			CL.clReleaseMemObject(memRedBlurredChannel);
			CL.clReleaseMemObject(memGreenBlurredChannel);
			CL.clReleaseMemObject(memBlueBlurredChannel);
			CL.clReleaseMemObject(ptrRasterArray);
			CL.clReleaseCommandQueue(commandQueue);
			CL.clReleaseContext(context);
			CL.clReleaseKernel(blurKernel);
			CL.clReleaseProgram(theProgram);
			CL.clReleaseMemObject(ptrFilterArray);
			CL.clReleaseMemObject(memRedChannel);
			CL.clReleaseMemObject(memGreenChannel);
			CL.clReleaseMemObject(memBlueChannel);
			CL.clReleaseMemObject(memRedBlurredResult);
			CL.clReleaseMemObject(memGreenBlurredResult);
			CL.clReleaseMemObject(memBlueBlurredResult);
			returnValue = runTime;
		}
		return returnValue;
	}

	/**
	 * Gets the selected device.
	 * 
	 * @return the id of the selected device
	 */
	public cl_device_id getSelectedDevice() {
		return selectedDevice;
	}

	/**
	 * Sets the selected device.
	 * 
	 * @param selectedDevice the selected device
	 */
	public void setSelectedDevice(cl_device_id selectedDevice) {
		this.selectedDevice = selectedDevice;
	}

	/**
	 * Sets the selected information.
	 * 
	 * @param thePlatform the new selected platform
	 */
	public void setSelectedPlatform(cl_platform_id thePlatform) {
		selectedPlatform = thePlatform;
	}
	
	/**
	 * Gets the currently selected platform.
	 * 
	 * @return the selected platform
	 */
	public cl_platform_id getSelectedPlatform() {
		return selectedPlatform;
	}

	/**
	 * Closes the current image.
	 */
	public void close() {
		frame.setImage(null);
		currentImage = null;
		currentFile = null;
	}

	/**
	 * Reads and returns the string of a file.
	 * 
	 * @param filename the name of the file to read
	 * @return the String of the file
	 */
	private static String readFile(String filename){
		String result = "";
		File file = new File(filename);
		try {
			Scanner scanner = new Scanner(file);
			while(scanner.hasNextLine()) {
				result += scanner.nextLine() + "\n";
			}
			scanner.close();
			return result;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Creates the filter for the blur function 
	 * 
	 * @param theBlurWidth
	 * @param sigma
	 * @return
	 */
	private double[][] createBlurFilter(int theBlurWidth, int sigma) {
		int rowBlur = (-1 * (theBlurWidth)) / 2;
		int colBlur = (-1 * (theBlurWidth)) / 2;
		double[][] filter = new double[theBlurWidth][theBlurWidth];
		double normalizeValue = 0;
		for(int row = 0; row < filter.length; row ++) {
			for(int col = 0; col < filter[row].length; col ++) {
				double temp = gaussinFunction(rowBlur, colBlur, sigma);
				filter[row][col] = temp;
				normalizeValue += temp;
				colBlur++;
			}
			colBlur = (-1 * (theBlurWidth)) / 2;
			rowBlur ++;
		}
		for(int row = 0; row < filter.length; row ++) {
			for(int col = 0; col < filter[row].length; col ++) {
				filter[row][col] = filter[row][col] / normalizeValue;
			}
		}
		return filter;
	}
	
	private double gaussinFunction(int valueOne, int valueTwo, int theSigma) {
		double fixerOne = valueOne;
		double fixerTwo = valueTwo;
		double exponent = (-1 * ((fixerOne * fixerOne) + (fixerTwo * fixerTwo))) / (2 * (theSigma * theSigma));
		double returnValue = Math.exp(exponent);
		return returnValue;
	}

	public double sequentialEqualization() {
		double returnValue = -1;
		int ALPHA_MASK = 0xff000000;
		int ALPHA_OFFSET = 24;
		int RED_MASK = 0x00ff0000;
		int RED_OFFSET = 16;
		if(currentImage != null){
			long startTime = System.nanoTime();
			int width = currentImage.getWidth();
			int height = currentImage.getHeight();
			int sourceData[] = new int[width * height];
			int alphaData[] = new int[width * height];
			for(int row = 0; row < height; row ++) {
				for(int col = 0; col < width; col++) {
					int index = (row * width) + col;
					int pixel = currentImage.getRGB(col, row);
					int alpha = (pixel & ALPHA_MASK) >> ALPHA_OFFSET;
					int r = (pixel & RED_MASK) >> RED_OFFSET;
					sourceData[index] = r;
					alphaData[index] = alpha;
				}
			}
			int[] histogram = new int[256];
			for(int count = 0; count <= 255; count ++) {
				histogram[count] = 0;
			}
			for(int i = 0; i < sourceData.length; i++) {
				int colorValue = sourceData[i];
				histogram[colorValue] ++;
			}
			int[] cuf = new int[256];
			int ramp = 0;
			for(int k = 0; k <= 255; k ++) {
				ramp += histogram[k];
				cuf[k] = ramp;
			}
			int[] feq = new int[256];
			int feqValue = sourceData.length / 255;
			Random rand = new Random();
			int addition = rand.nextInt(256);
			for(int j = 0; j <= 255; j ++) {
				if(j == addition) {
					feq[j] = feqValue + 1;
				}
				else {
					feq[j] = feqValue;
				}
			}
			int[] cufeq = new int[256];
			int in = 0;
			for(int h = 0; h <= 255; h++) {
				in += feq[h];
				cufeq[h] = in;
			}
			int[] newHistogram = new int[256];
			for(int index = 0; index <= 255; index ++) {
				int ramped = cuf[index];
				int closestIndex = 0;
				int best = Math.abs(ramped - cufeq[0]);
				for(int i = 1; i < cufeq.length; i ++) {
					int temp = Math.abs(ramped - cufeq[i]);
					if(temp < best) {
						closestIndex = i; 
						best = temp;
					}
				}
				newHistogram[index] = closestIndex;
			}
			int[] modifiedRaster = new int[sourceData.length];
			for(int c = 0; c < modifiedRaster.length; c ++) {
				int checker = sourceData[c];
				int equalValue = newHistogram[checker];
				Color newColor = new Color(equalValue, equalValue, equalValue, 0xff);
				modifiedRaster[c] = newColor.getRGB();
			}
			returnValue = System.nanoTime() - startTime;;
			DataBufferInt resultDataBuffer = new DataBufferInt(modifiedRaster, modifiedRaster.length);
			Raster resultRaster = Raster.createRaster(currentImage.getSampleModel(), resultDataBuffer, new Point(0, 0));
			currentImage.setData(resultRaster);
		}
		return returnValue;
	}

	public double parallelEqualization() {
		
		//Initialize the context properties	
		cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, selectedPlatform);
				
		//Create a context for the selected device
		cl_context context = CL.clCreateContext(
		contextProperties, 1,
			new cl_device_id[]{selectedDevice},
			null, null, null);		
		
				
		//Create a command queue for the selected device
		cl_command_queue commandQueue = CL.clCreateCommandQueue(context, selectedDevice, 0, null);

		//Get Raster information for array
		WritableRaster sourceRaster = currentImage.getRaster();
		DataBuffer sourceDataBuffer = sourceRaster.getDataBuffer();
		DataBufferInt sourceBytes = (DataBufferInt)sourceDataBuffer;
		int sourceData[] = sourceBytes.getData();
		int n = sourceData.length;
		int height = currentImage.getHeight();
		int width = currentImage.getWidth();
		int k = 16;
		int[] result = new int[n];
		int[] heightArray = new int[]{height};
		int[] widthArray = new int[]{width};
		
		int globalHeight = height;
		int globalWidth = width;
		while(globalHeight % k != 0) {
			globalHeight++;
		}
		while(globalWidth % k != 0) {
			globalWidth ++;
		}
		
		int[] globalHeightArray = new int[]{globalHeight};
		int[] globalWidthArray = new int[]{globalWidth};
		
		Pointer ptrArrayA = Pointer.to(sourceData);
		Pointer ptrResult = Pointer.to(result);
		
		cl_mem memArrayA = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * n, ptrArrayA, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * n, null, null);
		
		String source = readFile("kernels/array_split.cl");
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
				
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		cl_kernel kernel = CL.clCreateKernel(program, "split", null);
				
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memArrayA));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memResult));
		
		long[] globalWorkSize = new long[]{n};
		long[] localWorkSize = new long[]{1};
		
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memResult, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
				ptrResult, 0, null, null);
		
		int[] collection = new int[globalHeight * globalWidth];
		Pointer ptrCollection = Pointer.to(collection);
		
		cl_mem memResultInput = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * n, ptrResult, null);
		cl_mem memCollectionHistogram = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * collection.length, null, null);
		
		source = readFile("kernels/create_histogram_kernel.cl");
		program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
				
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		kernel = CL.clCreateKernel(program, "histogram", null);
				
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memResultInput));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_int, Pointer.to(heightArray));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(widthArray));
		CL.clSetKernelArg(kernel, 3, Sizeof.cl_int * (k*k), null);
		CL.clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(memCollectionHistogram));
		
		globalWorkSize = new long[]{globalWidth * globalHeight};
		localWorkSize = new long[]{k * k};
		
		//Execute the kernel
		long startTime = System.nanoTime();
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memCollectionHistogram, CL.CL_TRUE, 0, collection.length * Sizeof.cl_int, 
				ptrCollection, 0, null, null);
		long returnValue = System.nanoTime() - startTime;
		
		int[] resultHistogram = new int[256];
		
		Pointer ptrResultHistogram = Pointer.to(resultHistogram);
		
		cl_mem memResultHistogram = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * 256, null, null);
		
		source = readFile("kernels/collection_combine.cl");
		program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
				
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		kernel = CL.clCreateKernel(program, "collection_combine_kernel", null);
				
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memCollectionHistogram));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_int, Pointer.to(globalHeightArray));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(globalWidthArray));
		CL.clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(memResultHistogram));
		
		globalWorkSize = new long[]{n};
		localWorkSize = new long[]{1};
		
		//Execute the kernel
		long startTime2 = System.nanoTime();
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memResultHistogram, CL.CL_TRUE, 0, 256 * Sizeof.cl_int, 
				ptrResultHistogram, 0, null, null);
		returnValue += System.nanoTime() - startTime2;
		
		
		int[] cuf = blellochScan(resultHistogram, context, commandQueue);

		int feqValue = n/256;
		
		int[] feq = parallelArraySet(feqValue, 256, context, commandQueue);
		Random rand = new Random();
		int addition = rand.nextInt(256);
		feq[addition]++;
		
		int[] cufeq = blellochScan(feq, context, commandQueue);
		
		int[] newHistogram = new int[256];
		
		Pointer ptrCuf = Pointer.to(cuf);
		Pointer ptrCufeq = Pointer.to(cufeq);
		Pointer ptrNewHistogram = Pointer.to(newHistogram);
		
		cl_mem memCuf = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * 256, ptrCuf, null);
		cl_mem memCufeq = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * 256, ptrCufeq, null);
		cl_mem memNewHistogram = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * 256, null, null);
		
		source = readFile("kernels/adjust_histogram.cl");
		program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
				
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		kernel = CL.clCreateKernel(program, "adjust", null);
				
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memCuf));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memCufeq));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memNewHistogram));
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memNewHistogram, CL.CL_TRUE, 0, 256 * Sizeof.cl_int, 
				ptrNewHistogram, 0, null, null);
		
		int[] newRaster = new int[n];
		
		Pointer ptrNewRaster = Pointer.to(newRaster);
		
		cl_mem memNewRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * n, null, null);
		
		source = readFile("kernels/create_raster_kernel.cl");
		program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
		
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		kernel = CL.clCreateKernel(program, "create_raster", null);
		
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memResult));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memNewHistogram));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memNewRaster));
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memNewRaster, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
				ptrNewRaster, 0, null, null);	
		
		int[] finalRaster = new int[n];
		Pointer ptrFinalRaster = Pointer.to(finalRaster);
		
		cl_mem memNewRasterInput = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * n, ptrNewRaster, null);
		cl_mem memFinalRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * n, null, null);
		
		source = readFile("kernels/array_color_adjust.cl");
		program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
		
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		kernel = CL.clCreateKernel(program, "color_adjust", null);
		
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memNewRasterInput));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memFinalRaster));
		

		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memFinalRaster, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
				ptrFinalRaster, 0, null, null);
		
		DataBufferInt resultDataBuffer = new DataBufferInt(finalRaster, finalRaster.length);
		Raster resultRaster = Raster.createRaster(currentImage.getSampleModel(), resultDataBuffer, new Point(0, 0));
		currentImage.setData(resultRaster);
		
		CL.clReleaseKernel(kernel);
		CL.clReleaseMemObject(memArrayA);
		CL.clReleaseMemObject(memResult);
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memResultInput);
		CL.clReleaseMemObject(memCollectionHistogram);
		CL.clReleaseContext(context);
		CL.clReleaseCommandQueue(commandQueue);
		CL.clReleaseMemObject(memCuf);
		CL.clReleaseMemObject(memCufeq);
		CL.clReleaseMemObject(memNewHistogram);
		CL.clReleaseMemObject(memNewRaster);
		return returnValue;
	}
	
	public double UnoptimizedParallelEqualization() {
		
		//Initialize the context properties	
		cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, selectedPlatform);
				
		//Create a context for the selected device
		cl_context context = CL.clCreateContext(
		contextProperties, 1,
			new cl_device_id[]{selectedDevice},
			null, null, null);		
		
				
		//Create a command queue for the selected device
		cl_command_queue commandQueue = CL.clCreateCommandQueue(context, selectedDevice, 0, null);

		//Get Raster information for array
		WritableRaster sourceRaster = currentImage.getRaster();
		DataBuffer sourceDataBuffer = sourceRaster.getDataBuffer();
		DataBufferInt sourceBytes = (DataBufferInt)sourceDataBuffer;
		int sourceData[] = sourceBytes.getData();
		int n = sourceData.length;
		int[] result = new int[n];
		
		Pointer ptrArrayA = Pointer.to(sourceData);
		Pointer ptrResult = Pointer.to(result);
		
		cl_mem memArrayA = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * n, ptrArrayA, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * n, null, null);
		
		String source = readFile("kernels/array_split.cl");
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
				
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		cl_kernel kernel = CL.clCreateKernel(program, "split", null);
				
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memArrayA));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memResult));
		
		long[] globalWorkSize = new long[]{n};
		long[] localWorkSize = new long[]{1};
		
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memResult, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
				ptrResult, 0, null, null);
		
		int[] resultHistogram = new int[256];
		Pointer ptrCollection = Pointer.to(resultHistogram);
		
		cl_mem memResultInput = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * n, ptrResult, null);
		cl_mem memCollectionHistogram = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * resultHistogram.length, null, null);
		
		source = readFile("kernels/create_histogram_kernel_slow.cl");
		program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
				
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		kernel = CL.clCreateKernel(program, "histogram_slow", null);
				
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memResultInput));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memCollectionHistogram));
		
		globalWorkSize = new long[]{n};
		localWorkSize = new long[]{1};
		
		long startTime = System.nanoTime();
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memCollectionHistogram, CL.CL_TRUE, 0, 256 * Sizeof.cl_int, 
				ptrCollection, 0, null, null);	
		long returnValue = System.nanoTime() - startTime;
		
		int[] cuf = blellochScan(resultHistogram, context, commandQueue);

		int feqValue = n/256;
		
		int[] feq = parallelArraySet(feqValue, 256, context, commandQueue);
		Random rand = new Random();
		int addition = rand.nextInt(256);
		feq[addition]++;
		
		int[] cufeq = blellochScan(feq, context, commandQueue);
		
		int[] newHistogram = new int[256];
		
		Pointer ptrCuf = Pointer.to(cuf);
		Pointer ptrCufeq = Pointer.to(cufeq);
		Pointer ptrNewHistogram = Pointer.to(newHistogram);
		
		cl_mem memCuf = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * 256, ptrCuf, null);
		cl_mem memCufeq = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * 256, ptrCufeq, null);
		cl_mem memNewHistogram = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * 256, null, null);
		
		source = readFile("kernels/adjust_histogram.cl");
		program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
				
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		kernel = CL.clCreateKernel(program, "adjust", null);
				
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memCuf));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memCufeq));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memNewHistogram));
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memNewHistogram, CL.CL_TRUE, 0, 256 * Sizeof.cl_int, 
				ptrNewHistogram, 0, null, null);
		
		int[] newRaster = new int[n];
		
		Pointer ptrNewRaster = Pointer.to(newRaster);
		
		cl_mem memNewRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * n, null, null);
		
		source = readFile("kernels/create_raster_kernel.cl");
		program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
		
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		kernel = CL.clCreateKernel(program, "create_raster", null);
		
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memResult));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memNewHistogram));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memNewRaster));
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memNewRaster, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
				ptrNewRaster, 0, null, null);
		
		
		int[] finalRaster = new int[n];
		Pointer ptrFinalRaster = Pointer.to(finalRaster);
		
		cl_mem memNewRasterInput = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * n, ptrNewRaster, null);
		cl_mem memFinalRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * n, null, null);
		
		source = readFile("kernels/array_color_adjust.cl");
		program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
		
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		kernel = CL.clCreateKernel(program, "color_adjust", null);
		
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memNewRasterInput));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memFinalRaster));
		

		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memFinalRaster, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
				ptrFinalRaster, 0, null, null);
		
		DataBufferInt resultDataBuffer = new DataBufferInt(finalRaster, finalRaster.length);
		Raster resultRaster = Raster.createRaster(currentImage.getSampleModel(), resultDataBuffer, new Point(0, 0));
		currentImage.setData(resultRaster);
		
		CL.clReleaseKernel(kernel);
		CL.clReleaseMemObject(memArrayA);
		CL.clReleaseMemObject(memResult);
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memResultInput);
		CL.clReleaseMemObject(memCollectionHistogram);
		CL.clReleaseContext(context);
		CL.clReleaseCommandQueue(commandQueue);
		CL.clReleaseMemObject(memCuf);
		CL.clReleaseMemObject(memCufeq);
		CL.clReleaseMemObject(memNewHistogram);
		CL.clReleaseMemObject(memNewRaster);
		return returnValue;
	}
	
	private int[] parallelArraySet(int setValue, int size, cl_context context, cl_command_queue commandQueue) {
		int[] source = new int[size];
		int[] returnValue = new int[size];
		int[] sizeArray = new int[]{setValue};
		Pointer ptrArray = Pointer.to(source);
				
		cl_mem memArray = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * size, null, null);

		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String sourceFile = readFile("kernels/array_set.cl");
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ sourceFile }, null, null);
				
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, "set", null);
				
		//Set the arguments for the kernel
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_int, Pointer.to(sizeArray));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memArray));

				
		//Set the work−item dimensions
		long[] globalWorkSize = new long[]{size};
		long[] localWorkSize = new long[]{1};
		
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
				
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memArray, CL.CL_TRUE, 0, size * Sizeof.cl_int, 
				ptrArray, 0, null, null);
		for(int i = 0; i < source.length; i ++) {
			returnValue[i] = source[i];
		}
		
		CL.clReleaseMemObject(memArray);
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
		return returnValue;
	}
	
	protected int[] blellochScan(int[] theArray, cl_context context, cl_command_queue commandQueue){
		int size = theArray.length;
		int[] source = new int[size];
		int[] sizeArray = new int[]{size};
		int[] returnValue = new int[size];
		Pointer ptrArray = Pointer.to(theArray);
				
		cl_mem memArray = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * size, ptrArray, null);

		cl_mem memResultArray = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * size, null, null);
				
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String sourceFile = readFile("kernels/blelloch_scan_kernel.cl");
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ sourceFile }, null, null);
				
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		//Create the kernel
		cl_kernel theKernel = CL.clCreateKernel(program, "scan2", null);
				
		//Set the arguments for the kernel
		CL.clSetKernelArg(theKernel, 0, Sizeof.cl_mem, Pointer.to(memArray));
		CL.clSetKernelArg(theKernel, 1, Sizeof.cl_int, Pointer.to(sizeArray));
				
		//Set the work−item dimensions
		long[] globalWorkSize = new long[]{256};
		long[] localWorkSize = new long[]{256};
		
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, theKernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
				
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memArray, CL.CL_TRUE, 0, size * Sizeof.cl_int, 
				ptrArray, 0, null, null);
		for(int i = 0; i < source.length; i ++) {
			returnValue[i] = theArray[i];
		}
		CL.clReleaseMemObject(memArray);
		CL.clReleaseMemObject(memResultArray);
		CL.clReleaseKernel(theKernel);
		CL.clReleaseProgram(program);
		return returnValue;
	}
	
//	private int[] parrallelSort(int[] array, cl_context context, cl_command_queue commandQueue) {
//		int size = array.length;
//		int[] returnValue = new int[size];
//		Pointer ptrReturn = Pointer.to(returnValue);
//		Pointer ptrArray = Pointer.to(array);
//		
//		cl_mem memArray = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
//				Sizeof.cl_int * size, ptrArray, null);
//
//		cl_mem memResultArray = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
//				Sizeof.cl_int * size, null, null);
//				
//		//Create the program from the source code
//		//Create the OpenCL kernel from the program
//		String sourceFile = readFile("kernels/predicate_kernel.cl");
//		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ sourceFile }, null, null);
//				
//		//Build the program
//		CL.clBuildProgram(program, 0, null, null, null, null);
//				
//		//Create the kernel
//		cl_kernel theKernel = CL.clCreateKernel(program, "predicate", null);
//		
//		for(int i = 0; i < 31; i ++) {
//			int[] bitArray = new int[]{i}; 
//			
//			int[] predicate = new int[size];
//			
//			cl_mem memPredicate = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
//					Sizeof.cl_int * size, null, null);
//			
//			//Set the arguments for the kernel
//			CL.clSetKernelArg(theKernel, 0, Sizeof.cl_mem, Pointer.to(memArray));
//			CL.clSetKernelArg(theKernel, 1, Sizeof.cl_int, Pointer.to(bitArray));
//			CL.clSetKernelArg(theKernel, 2, Sizeof.cl_mem, Pointer.to(memPredicate));
//					
//			//Set the work−item dimensions
//			long[] globalWorkSize = new long[]{256};
//			long[] localWorkSize = new long[]{256};
//			
//			//Execute the kernel
//			CL.clEnqueueNDRangeKernel(commandQueue, theKernel, 1, null, globalWorkSize, localWorkSize,
//					0, null, null);
//					
//			//Read the output data
//			CL.clEnqueueReadBuffer(commandQueue, memArray, CL.CL_TRUE, 0, size * Sizeof.cl_int, 
//					ptrArray, 0, null, null);
//		}
//		
//		CL.clReleaseMemObject(memArray);
//		CL.clReleaseMemObject(memResultArray);
//		CL.clReleaseKernel(theKernel);
//		CL.clReleaseProgram(program);
//		return returnValue;
//	}
//	
	public double redEyeRemoval() throws IOException {
		double returnValue = -1;
		if(currentImage != null) {
			//Initialize the context properties	
			cl_context_properties contextProperties = new cl_context_properties();
			contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, selectedPlatform);
					
			//Create a context for the selected device
			cl_context context = CL.clCreateContext(
			contextProperties, 1,
				new cl_device_id[]{selectedDevice},
				null, null, null);		
			
					
			//Create a command queue for the selected device
			cl_command_queue commandQueue = CL.clCreateCommandQueue(context, selectedDevice, 0, null);

			//Get Raster information for array
			WritableRaster sourceRaster = currentImage.getRaster();
			DataBuffer sourceDataBuffer = sourceRaster.getDataBuffer();
			DataBufferInt sourceBytes = (DataBufferInt)sourceDataBuffer;
			int sourceData[] = sourceBytes.getData();
			int n = sourceData.length;
			int height = currentImage.getHeight();
			int width = currentImage.getWidth();
			File file = new File ("red_eye_effect_template_5.png");
			BufferedImage ri = ImageIO.read(file);
			BufferedImage template = new BufferedImage (ri.getWidth(), ri.getHeight (), BufferedImage.TYPE_INT_ARGB );
			Graphics g = template.getGraphics();
			g.drawImage (ri , 0, 0, null);
			WritableRaster sourceRasterTemplate = template.getRaster();
			DataBuffer sourceDataBufferTemplate = sourceRasterTemplate.getDataBuffer();
			DataBufferInt sourceBytesTemplate = (DataBufferInt)sourceDataBufferTemplate;
			int[] sourceDataTemplate = sourceBytesTemplate.getData();
			int templateHeight = template.getHeight();
			int templateWidth = template.getWidth();
			
			int[]redResult = new int[n];
			int[]greenResult = new int[n];
			int[]blueResult = new int[n];
			
			Pointer ptrArrayA = Pointer.to(sourceData);
			Pointer ptrRedResult = Pointer.to(redResult);
			Pointer ptrGreenResult = Pointer.to(greenResult);
			Pointer ptrBlueResult = Pointer.to(blueResult);
			
			cl_mem memArrayA = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_int * n, ptrArrayA, null);
			cl_mem memRedResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			cl_mem memGreenResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			cl_mem memBlueResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			
			String source = readFile("kernels/blur_array_setter.cl");
			cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
					
			CL.clBuildProgram(program, 0, null, null, null, null);
					
			cl_kernel kernel = CL.clCreateKernel(program, "array_setter_kernel", null);
					
			CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memArrayA));
			CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memRedResult));
			CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memGreenResult));
			CL.clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(memBlueResult));
			
			long[] globalWorkSize = new long[]{n};
			long[] localWorkSize = new long[]{1};
			
			//Execute the kernel
			long startTime = System.nanoTime();
			CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
					0, null, null);
				
			//Read the output data
			CL.clEnqueueReadBuffer(commandQueue, memRedResult, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
					ptrRedResult, 0, null, null);
			
			CL.clEnqueueReadBuffer(commandQueue, memGreenResult, CL.CL_TRUE, 0, n* Sizeof.cl_int,
					ptrGreenResult, 0, null, null);
			
			CL.clEnqueueReadBuffer(commandQueue, memBlueResult, CL.CL_TRUE, 0, n* Sizeof.cl_int,
					ptrBlueResult, 0, null, null);
			long runTime = System.nanoTime() - startTime;
			
			int s = sourceDataTemplate.length;
			int[] redTemplate = new int[s];
			int[] greenTemplate = new int[s];
			int[] blueTemplate = new int[s];
			
			Pointer ptrRedTemplate = Pointer.to(redTemplate);
			Pointer ptrGreenTemplate = Pointer.to(greenTemplate);
			Pointer ptrBlueTemplate = Pointer.to(blueTemplate);
			
			Pointer ptrTemplate = Pointer.to(sourceDataTemplate);
			
			cl_mem memTemplateValues = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_int * s, ptrTemplate, null);
			cl_mem memRedResultTemplate = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * s, null, null);
			cl_mem memGreenResultTemplate = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * s, null, null);
			cl_mem memBlueResultTemplate = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * s, null, null);
			
			CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memTemplateValues));
			CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memRedResultTemplate));
			CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memGreenResultTemplate));
			CL.clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(memBlueResultTemplate));
			
			globalWorkSize = new long[]{s};
			
			//Execute the kernel
			long startTime2 = System.nanoTime();
			CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
					0, null, null);
				
			//Read the output data
			CL.clEnqueueReadBuffer(commandQueue, memRedResultTemplate, CL.CL_TRUE, 0, s * Sizeof.cl_int, 
					ptrRedTemplate, 0, null, null);
			
			CL.clEnqueueReadBuffer(commandQueue, memGreenResult, CL.CL_TRUE, 0, s * Sizeof.cl_int,
					ptrGreenTemplate, 0, null, null);
			
			CL.clEnqueueReadBuffer(commandQueue, memBlueResult, CL.CL_TRUE, 0, s * Sizeof.cl_int,
					ptrBlueTemplate, 0, null, null);
			runTime += System.nanoTime() - startTime2;
			
			float[] redC = new float[n];
			float[] greenC = new float[n];
			float[] blueC = new float[n];
			
			Pointer ptrRedC = Pointer.to(redC);
			Pointer ptrGreenC = Pointer.to(greenC);
			Pointer ptrBlueC = Pointer.to(blueC);
			
			cl_mem memRedCResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			cl_mem memGreenCResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);
			cl_mem memBlueCResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);

			source = readFile("kernels/red_eye_kernel.cl");
			program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
					
			CL.clBuildProgram(program, 0, null, null, null, null);
					
			kernel = CL.clCreateKernel(program, "red_eye", null);
					
			CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memRedResultTemplate));
			CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memRedResult));
			CL.clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{height} ));
			CL.clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{width}));
			CL.clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{templateHeight}));
			CL.clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{templateWidth}));
			CL.clSetKernelArg(kernel, 6, Sizeof.cl_mem, Pointer.to(memRedCResult));
			
			globalWorkSize = new long[]{n};
			
			//Execute the kernel
			long startTime3 = System.nanoTime();
			CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
					0, null, null);
				
			//Read the output data
			CL.clEnqueueReadBuffer(commandQueue, memRedCResult, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
					ptrRedC, 0, null, null);
			runTime += System.nanoTime() - startTime3;
			
			CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memGreenResultTemplate));
			CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memGreenResult));
			CL.clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{height} ));
			CL.clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{width}));
			CL.clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{templateHeight}));
			CL.clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{templateWidth}));
			CL.clSetKernelArg(kernel, 6, Sizeof.cl_mem, Pointer.to(memGreenCResult));
			
			globalWorkSize = new long[]{n};
			
			//Execute the kernel
			long startTime4 = System.nanoTime();
			CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
					0, null, null);
				
			//Read the output data
			CL.clEnqueueReadBuffer(commandQueue, memGreenCResult, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
					ptrGreenC, 0, null, null);
			runTime += System.nanoTime() - startTime4;
			
			CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memBlueResultTemplate));
			CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memBlueResult));
			CL.clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{height} ));
			CL.clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{width}));
			CL.clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{templateHeight}));
			CL.clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{templateWidth}));
			CL.clSetKernelArg(kernel, 6, Sizeof.cl_mem, Pointer.to(memBlueCResult));
			
			//Execute the kernel
			long startTime5 = System.nanoTime();
			CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
					0, null, null);
				
			//Read the output data
			CL.clEnqueueReadBuffer(commandQueue, memGreenCResult, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
					ptrBlueC, 0, null, null);
			runTime += System.nanoTime() - startTime5;
			
			float[] finalC = new float[n];
			for(int i = 0; 0 < n; i ++) {
				float value = (redC[i] * blueC[i] * greenC[i]) * 100;
				finalC[i] = value;
			}
			
			float largest = 0;
			float position = 0;
			int count = 0;
			for(float temp : finalC) {
				if(temp >= largest) {
					position = count;
					largest = temp;
				}
				count ++;
			}
			
			//Change Pixels Here
			returnValue = runTime;
		}
		return returnValue;
	}

	public double seamlessImageClone(BufferedImage tempImage) {
		double returnValue = -1;
		if(currentImage != null) {
			//Initialize the context properties	
			cl_context_properties contextProperties = new cl_context_properties();
			contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, selectedPlatform);
					
			//Create a context for the selected device
			cl_context context = CL.clCreateContext(
			contextProperties, 1,
				new cl_device_id[]{selectedDevice},
				null, null, null);		
			
					
			//Create a command queue for the selected device
			cl_command_queue commandQueue = CL.clCreateCommandQueue(context, selectedDevice, 0, null);

			//Get Raster information for array
			WritableRaster sourceRaster = tempImage.getRaster();
			DataBuffer sourceDataBuffer = sourceRaster.getDataBuffer();
			DataBufferInt sourceBytes = (DataBufferInt)sourceDataBuffer;
			int sourceData[] = sourceBytes.getData();
			int n = sourceData.length;
			int[] maskData = new int[n];
			
			Pointer ptrSource = Pointer.to(sourceData);
			Pointer ptrMaskResult = Pointer.to(maskData);
			

			cl_mem memSourceValues = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_int * n, ptrSource, null);
			cl_mem memMaskResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
					Sizeof.cl_int * n, null, null);

			String source = readFile("kernels/create_mask_kernel.cl");
			cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
					
			CL.clBuildProgram(program, 0, null, null, null, null);
					
			cl_kernel kernel = CL.clCreateKernel(program, "create_mask", null);
					
			CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memSourceValues));
			CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memMaskResult));
			
			long[] globalWorkSize = new long[]{n};
			long[] localWorkSize = new long[]{1};
			
			//Execute the kernel
			long startTime = System.nanoTime();
			CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
					0, null, null);
				
			//Read the output data
			CL.clEnqueueReadBuffer(commandQueue, memMaskResult, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
					ptrMaskResult, 0, null, null);
			
			long runTime = System.nanoTime() - startTime;
			
			DataBufferInt resultDataBuffer = new DataBufferInt(maskData, maskData.length);
			Raster resultRaster = Raster.createRaster(currentImage.getSampleModel(), resultDataBuffer, new Point(0, 0));
			currentImage.setData(resultRaster);
			returnValue = runTime;
		}
		return returnValue;
	}
	
//	private float[] normalizedCrossCorrelationSequential(int[] templateValues, int[] imageValues,
//			int height, int width, int templateHeight, int templateWidth) throws IOException {
//		int count = 0;
//		float[] returnValue = new float[imageValues.length];
//		for(int row = 0; row < width; row ++) {
//			for(int col = 0; col < height; col ++) {
//				int index = (row * width) + col;
//				count += imageValues[index];
//			}
//		}
//		float t = count / imageValues.length;
//		for(int row = 0; row < height; row ++) {
//			for(int col = 0; col < width; col ++) {
//				int index = (row * width) + col;
//				float numerator = 0;
//				double denominator = 0;
//				float denominatorOne = 0;
//				float denominatorTwo = 0;
//			    int valueCount = 0;
//				for(int x = col - (width / 2); x < col + (width / 2) - 1; x ++) {
//					for(int y = row - (height / 2); y < row + (height / 2) - 1; y ++) {
//						if((x >= 0 && x < width) && (y >= 0 && y < height)) {
//							valueCount += imageValues[index];
//						}
//					}
//				}
//				float f = valueCount / templateValues.length;
//			    float firstPart = 0;
//			    float secondPart = 0;
//			    int rowCount = 0;
//			    int colCount = 0;
//				for(int x = row - (templateWidth/2); x < row + (templateWidth/2); x ++) {
//					for(int y = height - (templateHeight/2); y < height + (templateHeight/2); y ++) {
//						int templateIndex = (rowCount * templateWidth) + colCount;
//						int pictureIndex = (y * width) + x; 
//						if((x >= 0 && x < width) && (y >=0 && y < height)) {
//							firstPart = (templateValues[templateIndex] - t);
//							secondPart = (imageValues[pictureIndex] - f);
//							numerator += firstPart * secondPart;
//							denominatorOne += Math.pow(firstPart, 2);
//							denominatorTwo += Math.pow(secondPart, 2);
//						}
//						colCount ++;
//					}
//					rowCount ++;
//					colCount = 0;
//				}
//				denominator = (Math.sqrt(denominatorOne) * Math.sqrt(denominatorTwo));
//				returnValue[index] = (float) (numerator / denominator);
//			}
//		}
//		return returnValue;
//	}
	
}
