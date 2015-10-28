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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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

	public long sequentialEqualization() {
		long returnValue = -1;
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
			returnValue = System.nanoTime() - startTime;
			System.out.println(width * height);
			System.out.println(sourceData.length);
			DataBufferInt resultDataBuffer = new DataBufferInt(modifiedRaster, modifiedRaster.length);
			Raster resultRaster = Raster.createRaster(currentImage.getSampleModel(), resultDataBuffer, new Point(0, 0));
			currentImage.setData(resultRaster);
		}
		return returnValue;
	}

	public long parallelEqualization() {
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
		
		Pointer ptrArrayA = Pointer.to(sourceData);
		Pointer ptrResult = Pointer.to(result);
		
		cl_mem memArrayA = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * n, ptrArrayA, null);
		cl_mem memresult = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * n, null, null);
		
		String source = readFile("kernels/gray_values_kernel.cl");
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
				
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		cl_kernel kernel = CL.clCreateKernel(program, "gray_values", null);
				
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memArrayA));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memresult));
		
		long[] globalWorkSize = new long[]{n};
		long[] localWorkSize = new long[]{1};
		
		//Execute the kernel
		long startTime = System.nanoTime();
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
			
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memresult, CL.CL_TRUE, 0, n * Sizeof.cl_int, 
				ptrResult, 0, null, null);
		long returnValue = System.nanoTime() - startTime;
		
		long startTime2 = System.nanoTime();
		int[] emptyHistogram = parallelArraySet(0, n, context, commandQueue);
		
		return returnValue;
	}
	
	private int[] parallelSumScan(int[] array) {
		int[] returnValue = new int[array.length];
		
		Pointer ptrArray = Pointer.to(returnValue);
		
		return returnValue;
	}
	
	private int[] parallelArraySet(int setValue, int size, cl_context context, cl_command_queue commandQueue) {
		int[] returnValue = new int[size];
		int[] sizeArray = new int[]{setValue};
		Pointer ptrArray = Pointer.to(returnValue);
				
		cl_mem memArray = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE,
				Sizeof.cl_int * size, ptrArray, null);

				
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String sourceFile = readFile("kernels/array_set.cl");
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ sourceFile }, null, null);
				
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, "set", null);
				
		//Set the arguments for the kernel
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memArray));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_int, Pointer.to(sizeArray));
				
		//Set the work−item dimensions
		long[] globalWorkSize = new long[]{size};
		long[] localWorkSize = new long[]{1};
		
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
				
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memArray, CL.CL_TRUE, 0, size * Sizeof.cl_int, 
				ptrArray, 0, null, null);
		
		return returnValue;
	}
}
