package edu.kings.cs380.project1;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Handles loading an image from a file and processing it to the frame
 * 
 * @author Robert Lowman
 * @date 9.9.2015
 */
public class ImageLoader {
	
	/**
	 * Loads an image from a file 
	 * @throws IOException if the file cannot be found
	 */
	public BufferedImage loadFile(File theFile) throws IOException {
		BufferedImage ri = ImageIO.read(theFile);
		BufferedImage theImage = new BufferedImage(ri.getWidth(), ri.getHeight(), 
				BufferedImage.TYPE_INT_ARGB);
		return theImage;
	}
	
	/**
	 * Saves the changes made to the file
	 * @throws IOException if the image cannot be saved
	 */
	public void saveFile(BufferedImage theImage, File theFile) throws IOException {
		ImageIO.write(theImage, "png", theFile);
	}
}
