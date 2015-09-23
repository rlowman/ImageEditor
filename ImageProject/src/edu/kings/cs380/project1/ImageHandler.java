package edu.kings.cs380.project1;

import java.awt.Color;
import java.awt.image.BufferedImage;

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
	
//	public BufferedImage grayScaleParallel(BufferedImage theImage) {
//		BufferedImage returnImage = theImage;
//		
//	}
}
