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
}
