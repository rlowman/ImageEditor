package edu.kings.cs380.project1;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;


/**
 * The main terminal of the program
 * 
 * @author Robert Lowman
 * @date 9.6.2015
 */
public class Window implements ActionListener {
	
	/**The main frame of the program*/
	private JFrame mainFrame;
	
	/**The submit button for the file to load*/
	private JButton enter;
	
	/**The text field to enter the name of the file to load*/
	private JTextArea fileName;
	
	/**Tool to help load files to the frame*/
	private ImageLoader loader;
	
	/**Tool to help handle the algorithms necessary for the images*/
	private ImageHandler handler;
	
	/**Panel that handles the buttons to operate features*/
	private JPanel buttonPanel;
	
	/**Panel that handles drawing the image to the screen*/
	private JPanel drawingPanel;
	
	/**Button that makes a picture grayscale*/
	private JButton grayscaleButton;
	
	/**Button that saves the current image to the file*/
	private JButton saveButton;
	
	/**Button that closes the current file*/
	private JButton closeFileButton;
	
	/**Label to appear when no image is drawn to screen*/
	private JLabel noImageLabel;
	
	/**Label that states the time of each processing algorithm*/
	private JLabel timeLabel;
	
	/**The current image drawn to the screen*/
	private BufferedImage currentImage;
	
	/**The current file being processed*/
	private File currentFile;
	
	/**
	 * Constructor class for the main frame 
	 */
	public Window() {
		currentImage = null;
		currentFile = null;
		
		mainFrame = new JFrame("Monarch Image Editing Studio");
		mainFrame.setSize(700, 500);
		
		GridLayout mainLayout = new GridLayout(1,2);
		mainFrame.setLayout(mainLayout);
		
		GridLayout buttonLayout = new GridLayout(6,1);
		buttonPanel = new JPanel();
		buttonPanel.setLayout(buttonLayout);
		drawingPanel = new JPanel();
		drawingPanel.setLayout(buttonLayout);
		timeLabel = new JLabel("Time label");
		
		enter = new JButton("Enter");
		enter.addActionListener(this);
		saveButton = new JButton("Save Image");
		saveButton.addActionListener(this);
		closeFileButton = new JButton("Close File");
		closeFileButton.addActionListener(this);
		grayscaleButton = new JButton("Grayscale");
		grayscaleButton.addActionListener(this);
		fileName = new JTextArea();
		loader = new ImageLoader();
		handler = new ImageHandler();
		buttonPanel.add(fileName);
		buttonPanel.add(enter);
		buttonPanel.add(grayscaleButton);
		buttonPanel.add(closeFileButton);
		buttonPanel.add(saveButton);
		buttonPanel.add(timeLabel);
		mainFrame.add(buttonPanel);
		mainFrame.add(drawingPanel);
		
		noImageLabel = new JLabel("No Image Selected");
		drawingPanel.add(BorderLayout.CENTER, noImageLabel);
		mainFrame.setVisible(true);
	}

	/**
	 * Handles the actions performed for the GUI
	 */
	@Override
	public void actionPerformed(ActionEvent ae) {
		if(ae.getSource() == enter) {
			String result = fileName.getText();
			if(!result.equals("")) {
				try {
					currentFile = new File(result);
					currentImage = loader.loadFile(currentFile);
					drawImage(currentImage);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(mainFrame, "File Not Found", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			else {
				 JOptionPane.showMessageDialog(mainFrame, "No File Information Entered", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == grayscaleButton) {
			if(currentImage != null) {
				long startTime = System.nanoTime();
				BufferedImage tempImage = handler.grayScale(currentImage);
				long runtime = System.nanoTime() - startTime;
				timeLabel.setText("Grayscale Algorithm Time in ms:/n" + runtime);
				drawImage(tempImage);
			}
			else {
				JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == saveButton) {
			try {
				loader.saveFile(currentImage, currentFile);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(mainFrame, "File could not be saved", "Error", JOptionPane.ERROR_MESSAGE);
			} 
		}
	}
	
	/**
	 * Helper method that draws image to the screen of the editor
	 * 
	 * @param drawImage the image to draw to the screen
	 */
	private void drawImage(BufferedImage drawImage) {
		Graphics g = drawImage.getGraphics();
		g.drawImage(drawImage, 0, 0, drawingPanel);
	}
}
