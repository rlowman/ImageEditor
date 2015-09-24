package edu.kings.cs380.project1;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

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
 * Handles the image algorithms necessary for the project
 * 
 * @author Robert Lowman
 * @date 9.8.2015
 */
public class ImageHandler {
	
	/**
	 * Takes an Image and returns it in grayscale.
	 * 
	 * @param theImage the Image to turn to grayscale
	 * @return the grayscaled image
	 * (Modified from http://kings.mrooms2.net/mod/resource/view.php?id=147952)
	 */
	public BufferedImage grayScale(BufferedImage theImage) {
		BufferedImage returnImage = theImage;
		int width = theImage.getWidth ();
		int height = theImage.getHeight ();
		for(int row = 0 ; row < height ; row ++) {
			for (int column = 0; column < width; column ++) {
				Color c = new Color (theImage.getRGB(column, row));
				int red = c. getRed();
				int green = c.getGreen();
				int blue = c. getBlue();
				int alpha = c.getAlpha();
				int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
				Color newColor = new Color(gray, gray, gray, alpha);
				returnImage.setRGB(column, row, newColor.getRGB());
			}
		}
		return returnImage;
	}
	
	/**
	 * Returns the image in grayscale using parallel programming
	 * 
	 * @param theImage the image to turn to grayscale
	 * @return the given image in grayscale
	 */
	public BufferedImage grayScaleParallel(BufferedImage theImage) {
		BufferedImage returnImage = theImage;
		//Enable exceptions
		CL.setExceptionsEnabled(true);
				
		//Selecting the device
		final int platformIndex = 0;
		final long deviceType = CL.CL_DEVICE_TYPE_ALL;
		final int deviceIndex = 0;
						
		//Get number of platforms
		int[] numberOfPlatformsArray = new int[1];
		CL.clGetPlatformIDs(0, null, numberOfPlatformsArray);
		int numberOfPlatforms = numberOfPlatformsArray[0];
				
		//Obtain a platform ID
		cl_platform_id[] platforms = new cl_platform_id[numberOfPlatforms];
		CL.clGetPlatformIDs(platforms.length, platforms, null);
		cl_platform_id platform = platforms[platformIndex];
				
		//Initialize the context properties
		cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, platform);
				
		//Obtain the number of devices for the platform
		int numberOfDevicesArray[] = new int[1];
		CL.clGetDeviceIDs(platform, deviceType, 0,
					null, numberOfDevicesArray);
		int numberOfDevices = numberOfDevicesArray[0];
				
		//Get a device ID
		cl_device_id[] devices = new cl_device_id[numberOfDevices];
		CL.clGetDeviceIDs(platform, deviceType, numberOfDevices, devices,
				null);
		cl_device_id device = devices[deviceIndex];
				
		//Create a context for the selected device
		cl_context context = CL.clCreateContext(
				contextProperties, 1,
				new cl_device_id[]{device},
				null, null, null);
				
		//Create a command queve for the selected device
		cl_command_queue commandQueue = CL.clCreateCommandQueue(context, device, 0, null);
				
		//Create Arrays to give to kernel
		int width = theImage.getWidth();
		int height = theImage.getHeight();
		
		WritableRaster sourceRaster = theImage.getRaster();
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
		String source = readFile("kernels/hello_kernel");
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ source }, null, null);
				
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
				
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, "hello_kernel", null);
				
		//Set the arguments for the kernel
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memArrayA));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memResult));
				
		//Set the work−item dimensions
		long[] globalWorkSize = new long[]{n};
		long[] localWorkSize = new long[]{1};
				
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize,
				0, null, null);
				
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memResult, CL.CL_TRUE, 0, n* Sizeof.cl_float, 
				ptrResult, 0, null, null);
		
		//Write the results to the BufferedImage named result
		DataBufferInt resultDataBuffer = new DataBufferInt(result, result.length);
		Raster resultRaster = Raster.createRaster(theImage.getSampleModel(), resultDataBuffer, new Point(0, 0));
		returnImage.setData(resultRaster);
				
		//Clean-up
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memArrayA);
		CL.clReleaseMemObject(memResult);
		CL.clReleaseCommandQueue(commandQueue);
		CL.clReleaseContext(context);
		
		//Return the modified image
		return returnImage;
	}
	
	/**
	 * Reads and returns the string of a file
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
}
