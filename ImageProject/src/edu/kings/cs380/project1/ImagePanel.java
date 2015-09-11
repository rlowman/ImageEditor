/**
 * 
 */
package edu.kings.cs380.project1;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Handles drawing the image to the screen of the program
 * 
 * @author Robert Lowman
 * @date 9.11.2015
 */
public class ImagePanel extends JPanel {
	
	/**Generated ID for this class*/
	private static final long serialVersionUID = 1L;
	
	/**The image posted to the program*/
	private BufferedImage image;
	
	/**
	 * Constructor class for the JPanel that handles images
	 * 
	 * @param theImage the image to post to the program
	 */
	public ImagePanel(BufferedImage theImage) {
		image = theImage;
	}
	
	/**
	 * Draws a custom image to the JComponenet
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if(image != null) {
			g.drawImage(image, 0, 0, this);
		}
	}

	/**
	 * Gets the current image drawn to the screen
	 * 
	 * @return the current image
	 */
	public BufferedImage getImage() {
		return image;
	}

	/**
	 * Sets and draws the current Image
	 * 
	 * @param image the image to 
	 */
	public void setImage(BufferedImage image) {
		this.image = image;
		
	}
}
