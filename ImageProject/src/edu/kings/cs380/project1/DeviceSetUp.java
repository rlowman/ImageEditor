package edu.kings.cs380.project1;

import java.util.ArrayList;

import org.jocl.CL;
import org.jocl.cl_platform_id;
/**
 * @author guest-zAlVnh
 *
 */
public class DeviceSetUp {
	
	public static ArrayList<String> getPlatformIDs() {
		CL.setExceptionsEnabled(true);
		ArrayList<String> returnValue = new ArrayList<String>();
		int [] numPlatformsArray = new int [1];
		CL.clGetPlatformIDs(0, null, numPlatformsArray);
		int numPlatforms = numPlatformsArray [0];
		cl_platform_id [] platforms= new cl_platform_id [ numPlatforms ];
		CL.clGetPlatformIDs( platforms . length , platforms , null);
//		cl_platform_id platform = platforms [platformIndex];
		return returnValue;	
	}
}
