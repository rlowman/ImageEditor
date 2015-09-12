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
	 * Loads an image from a file.
	 *  
	 * @param theFile the file to load to the program
	 * @throws IOException if the file cannot be found
	 * @return the image from the given file
	 * (Modified from http://kings.mrooms2.net/mod/resource/view.php?id=147952)
	 */
	public BufferedImage loadFile(File theFile) throws IOException {
		BufferedImage ri = ImageIO.read(theFile);
		BufferedImage theImage = new BufferedImage(ri.getWidth(), ri.getHeight(), 
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = theImage.getGraphics();
		g.drawImage(ri, 0, 0, null);
		return theImage;
	}
	
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
