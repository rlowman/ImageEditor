package edu.kings.cs380.project1;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Handles loading an image from a file and processing it to the frame.
 * 
 * @author Robert Lowman
 * @version 9.9.2015
 */
public class ImageLoader {
	
	/**
	 * Saves the changes made to the file.
	 * 
	 * @param theImage the Image to save to the given file
	 * @param theFile the File to save the image to
	 * @throws IOException if the image cannot be saved
	 * (Modified from http://kings.mrooms2.net/mod/resource/view.php?id=147952)
	 */
	public void saveFile(BufferedImage theImage, File theFile) throws IOException {
		ImageIO.write(theImage, "png", theFile);
	}
}
