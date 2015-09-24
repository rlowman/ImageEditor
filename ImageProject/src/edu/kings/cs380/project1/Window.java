package edu.kings.cs380.project1;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;


/**
 * The main terminal of the program.
 * 
 * @author Robert Lowman
 * @version 9.6.2015
 */
public class Window implements ActionListener {
	
	/**The main frame of the program.*/
	private JFrame mainFrame;
	
	/**MenuBar of the program.*/
	private JMenuBar menuBar;
	
	/**Tool to help load files to the frame.*/
	private ImageLoader loader;
	
	/**Tool to help handle the algorithms necessary for the images.*/
	private ImageHandler handler;
	
	/**Panel that handles the buttons to operate features.*/
	private JPanel buttonPanel;
	
	/**Panel that handles drawing the image to the screen.*/
	private ImagePanel drawingPanel;
	
	/**Button that makes a picture grayscale.*/
	private JButton grayscaleButton;
	
	/**Button that makes a picture grayscale using parallel programming*/
	private JButton grayscaleButtonParallel;
	
	/**Button that saves the current image to the file.*/
	private JMenuItem save;
	
	/**Button that closes the current file.*/
	private JMenuItem close;
	
	/**Label that states the time of each processing algorithm.*/
	private JLabel timeLabel;
	
	/**The current image drawn to the screen.*/
	private BufferedImage currentImage;
	
	/**The current file being processed.*/
	private File currentFile;
	
	/**File menu of the program.*/
	private JMenu file; 
	
	/**Open menu of the program.*/
	private JMenuItem open;
	
	/**
	 * Constructor class for the main frame.
	 */
	public Window() {
		currentImage = null;
		currentFile = null;
		
		mainFrame = new JFrame("Monarch Image Editing Studio");
		mainFrame.setSize(700, 500);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		menuBar = new JMenuBar();
		file = new JMenu("File");
		file.addActionListener(this);
		open = new JMenuItem("Open");
		open.addActionListener(this);
		file.add(open);
		menuBar.add(file);
		mainFrame.setJMenuBar(menuBar);
		
		GridLayout mainLayout = new GridLayout(1,2);
		mainFrame.setLayout(mainLayout);
		
		GridLayout buttonLayout = new GridLayout(6,1);
		buttonPanel = new JPanel();
		buttonPanel.setLayout(buttonLayout);
		drawingPanel = new ImagePanel(null);
		
		save = new JMenuItem("Save");
		save.addActionListener(this);
		file.add(save);
		close = new JMenuItem("Close");
		close.addActionListener(this);
		grayscaleButton = new JButton("Grayscale");
		grayscaleButton.addActionListener(this);
		grayscaleButtonParallel = new JButton("Grayscale Parallel");
		grayscaleButtonParallel.addActionListener(this);
		loader = new ImageLoader();
		handler = new ImageHandler(drawingPanel);
		buttonPanel.add(grayscaleButton);
		buttonPanel.add(grayscaleButtonParallel);
		file.add(close);
		mainFrame.add(buttonPanel);
		mainFrame.add(drawingPanel);
		
		timeLabel = new JLabel("Time Label");
		buttonPanel.add(timeLabel);
		
		mainFrame.setVisible(true);
	}
	
	/**
	 * Handles the actions performed for the GUI.
	 * 
	 * @param ae the action event that triggered the method
	 */
	@Override
	public void actionPerformed(ActionEvent ae) {
		if(ae.getSource() == open) {
			JFileChooser chooser = new JFileChooser();
			if( chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION ) {
				currentFile = chooser.getSelectedFile();
				try {
					handler.loadFile(currentFile);
					mainFrame.repaint();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(mainFrame, "File Not Found", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		else if(ae.getSource() == grayscaleButton) {
			long startTime = System.nanoTime();
			boolean temp = handler.grayScale(currentImage);
			long runTime = System.nanoTime() - startTime;
			long actualTime = runTime / 1000000;
			if(temp) {
				timeLabel.setText("Grayscale Algorithm Time in Milliseconds: " + actualTime);
				mainFrame.repaint();
			}
			else {	
				JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == save) {
			if(currentImage != null) {
				try {
					loader.saveFile(currentImage, currentFile);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(mainFrame, "File could not be saved", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			else {
				JOptionPane.showMessageDialog(mainFrame, "No File Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == close) {
			drawingPanel.setImage(null);
			currentFile = null;
			currentImage = null;
			mainFrame.repaint();
		}
		else if(ae.getSource() == grayscaleButtonParallel) {
			if(currentImage != null) {
				long startTime = System.nanoTime();
				BufferedImage tempImage = handler.grayScaleParallel(currentImage);
				long runTime = System.nanoTime() - startTime;
				long actualTime = runTime / 1000000;
				timeLabel.setText("Grayscale Algorithm Time in Milliseconds: " + actualTime);
				drawingPanel.setImage(tempImage);
				mainFrame.repaint();
			}
			else {
				JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
