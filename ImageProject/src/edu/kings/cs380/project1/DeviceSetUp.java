package edu.kings.cs380.project1;

import java.util.HashMap;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;
/**
 * Handles OpenCL code.
 * 
 * @author Robert Lowman
 * @version 9.22.2015
 *
 */
public class DeviceSetUp {
	
	/**
	 * Gets all of the platform IDs of the current machine.
	 * 
	 * @return ArrayList of available platforms
	 */
	public static cl_platform_id[] getPlatformIDs() {
		CL.setExceptionsEnabled(true);
		//Get number of platforms
		int [] numberOfPlatformsArray = new int[1];
		CL.clGetPlatformIDs(0, null, numberOfPlatformsArray);
		int numPlatforms = numberOfPlatformsArray [0];
		
		//Get platform IDs
		cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
		CL.clGetPlatformIDs(platforms.length, platforms, null);
		return platforms;	
	}
	
	/**
	 * Gets the devices associated with the given platform.
	 * 
	 * @param theId the ID of the platform to get devices of
	 * @return HashMap of device Id mapped to their String value
	 */
	public static HashMap<cl_device_id, cl_platform_id> getDevices(cl_platform_id theId) {
		HashMap<cl_device_id, cl_platform_id> returnValue = new HashMap<cl_device_id, cl_platform_id>();
		//Get number of devices
		int numberOfDevicesArray[] = new int[1];
		CL.clGetDeviceIDs(theId, CL.CL_DEVICE_TYPE_ALL,0, null, numberOfDevicesArray );
		int numberOfDevices = numberOfDevicesArray [0];
		
		//Get the list of devices from the platform
		cl_device_id devicesArray[] = new cl_device_id[numberOfDevices];
		CL.clGetDeviceIDs(theId, CL.CL_DEVICE_TYPE_ALL,numberOfDevices , devicesArray , null);
		
		//Fill HashMap
		for(int deviceIndex = 0; deviceIndex < devicesArray.length; deviceIndex ++) {
			cl_device_id temp = devicesArray[deviceIndex];
			returnValue.put(temp, theId);
		}
		return returnValue;
	}
	
	/**
	 * Gets the names of the devices in the given array.
	 * 
	 * @param theDevice the device to get the names of
	 * @return HashMap of device mapped with String value
	 */
	public static String getDeviceName(cl_device_id theDevice) {
		long size[] = new long[1];
		CL.clGetDeviceInfo(theDevice, CL.CL_DEVICE_NAME, 0, null, size);
		byte buffer[] = new byte[(int)size[0]];
		CL.clGetDeviceInfo(theDevice, CL.CL_DEVICE_NAME, buffer.length, Pointer.to(buffer), null);
		String deviceName = new String (buffer, 0, buffer.length - 1);
		return deviceName;
	}
}
