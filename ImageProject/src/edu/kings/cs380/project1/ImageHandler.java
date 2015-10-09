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
	 * Blurs the image using sequential computing
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
			for(int h = 0; h < height; h ++) {
				for(int w = 0; w < width; w ++) {
					int index = (h * width) + w;
					int redBlur = 0;
					int blueBlur = 0;
					int greenBlur = 0;
					for(int row = 0; row < 5; row ++) {
						int rowFixer = h - 2;
						for(int col = 0; col < 5; col ++) {
							int colFixer = w - 2;
							if(rowFixer >= 0 && colFixer >= 0){
								int i = (rowFixer * width) + colFixer;
								redBlur += redValues[i] * filter[row][col];
								greenBlur += greenValues[i] * filter[row][col];
								blueBlur += blueValues[i] * filter[row][col];
							}
							colFixer++;
						}
						rowFixer++;
					}
					redBlurred[index] = redBlur;
					greenBlurred[index] = greenBlur;
					blueBlurred[index] = blueBlur;
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
	 * Blurs the image using parallel computing
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
					Sizeof.cl_float * n, ptrArrayA, null);
			cl_mem memRedResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_float * n, ptrRedResult, null);
			cl_mem memGreenResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_float * n, ptrGreenResult, null);
			cl_mem memBlueResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
					Sizeof.cl_float * n, ptrBlueResult, null);
			
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
			CL.clEnqueueReadBuffer(commandQueue, memRedResult, CL.CL_TRUE, 0, n * Sizeof.cl_float, 
					ptrRedResult, 0, null, null);
			
			CL.clEnqueueReadBuffer(commandQueue, memGreenResult, CL.CL_TRUE, 0, n* Sizeof.cl_float,
					ptrGreenResult, 0, null, null);
			
			CL.clEnqueueReadBuffer(commandQueue, memBlueResult, CL.CL_TRUE, 0, n* Sizeof.cl_float,
					ptrBlueResult, 0, null, null);
			long runTime = System.nanoTime() - startTime;
			
			CL.clReleaseKernel(kernel);
			CL.clReleaseProgram(program);
			CL.clReleaseMemObject(memArrayA);
			CL.clReleaseMemObject(memRedResult);
			CL.clReleaseMemObject(memGreenResult);
			CL.clReleaseMemObject(memBlueResult);
			
			
			
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
		int sigma = 2;
		double fixerOne = valueOne;
		double fixerTwo = valueTwo;
		double exponent = (-1 * ((fixerOne * fixerOne) + (fixerTwo * fixerTwo))) / (2 * (theSigma * theSigma));
		double returnValue = Math.exp(exponent);
		return returnValue;
	}
}
