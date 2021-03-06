package edu.kings.cs380.project1;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;

import org.jocl.CL;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;


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
	
	/**Tool to help handle the algorithms necessary for the images.*/
	private ImageHandler handler;
	
	/**Panel that handles the buttons to operate features.*/
	private JPanel buttonPanel;
	
	/**Panel that handles drawing the image to the screen.*/
	private ImagePanel drawingPanel;
	
	/**Button that makes a picture grayscale.*/
	private JButton grayscaleButton;
	
	/**Button that makes a picture grayscale using parallel programming.*/
	private JButton grayscaleButtonParallel;
	
	/**Button that saves the current image to the file.*/
	private JMenuItem save;
	
	/**Button that closes the current file.*/
	private JMenuItem close;
	
	/**Label that states the time of each processing algorithm.*/
	private JTextArea timeLabel;
	
	/**File menu of the program.*/
	private JMenu file; 
	
	/**Open menu of the program.*/
	private JMenuItem open;
	
	/**The button group of available devices.*/
	private ButtonGroup deviceGroup;
	
	/**Menu Item for the saveAs function.*/
	private JMenuItem saveAs;
	
	/**Menu Item for available devices.*/
	private JMenu devices;
	
	/**Button that blurs the image using sequential computing*/
	private JButton seqBlur;
	
	/**Button that blurs the image using parellel computing*/
	private JButton parallelBlur;
	
	/**Button that equalizes the image using sequential computing*/
	private JButton seqEqualization;
	
	/**Button that equalized the image using parallel computing*/
	private JButton parallelEqualization;
	
	/**Optimized parallel version of equalization*/
	private JButton optimized;
	
	/**Button that removes the red eye from an image*/
	private JButton redEye;
	
	/**Button for seamless image clone*/
	private JButton imageClone;
	
	/**Button for sequential sepia*/
	private JButton sepiaSeq;
	
	/**Button for parallel sepia*/
	private JButton sepiaPar;
	
	
//	private JMenu zoom;
//	
//	private ButtonGroup zoomGroup;
//	
//	private JRadioButtonMenuItem normal;
//	
//	private JRadioButtonMenuItem plusFifty;
//	
//	private JRadioButtonMenuItem doubleZoom;
//	
//	private JRadioButtonMenuItem plusOneFifty;
//	
//	private JRadioButtonMenuItem tripleZoom;
//	
//	private JRadioButtonMenuItem plusTwoFifty;
//	
//	private JRadioButtonMenuItem quadZoom;
//	
//	private JRadioButtonMenuItem plusThreeFifty;
//	
//	private JRadioButtonMenuItem fiveZoom;
	
	/**HashMap of all the Devices.*/
	private HashMap<cl_device_id, cl_platform_id> theDevices;
	
	/**HashMap of all the device names.*/
	private HashMap<String, cl_device_id> deviceNames;
	
	/**HashMap of device buttons.*/
	private HashMap<JRadioButtonMenuItem, cl_device_id> buttons; 
	
	/**
	 * Constructor class for the main frame.
	 */
	public Window() {
		
		buttons = new HashMap<JRadioButtonMenuItem, cl_device_id>();
		
		mainFrame = new JFrame("Monarch Image Editing Studio");
		mainFrame.setSize(1000, 500);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		deviceNames = new HashMap<String, cl_device_id>();
		
		cl_platform_id[] thePlatforms = DeviceSetUp.getPlatformIDs();
		for(cl_platform_id temp: thePlatforms){
			theDevices = DeviceSetUp.getDevices(temp);
			if(theDevices.isEmpty()) {
				System.out.println("flag");
			}
		}
		
		drawingPanel = new ImagePanel(null);
		setDefaultGPU();
		
		menuBar = new JMenuBar();
		file = new JMenu("File");
		file.addActionListener(this);
		open = new JMenuItem("Open");
		open.addActionListener(this);
		devices = new JMenu("Devices");
		devices.addActionListener(this);
		save = new JMenuItem("Save");
		save.addActionListener(this);
		saveAs = new JMenuItem("Save As");
		saveAs.addActionListener(this);
		close = new JMenuItem("Close");
		close.addActionListener(this);
		file.add(open);
		file.add(close);
		file.add(save);
		file.add(saveAs);
		menuBar.add(file);
		menuBar.add(devices);
		mainFrame.setJMenuBar(menuBar);
		
		GridLayout mainLayout = new GridLayout(1,2);
		mainFrame.setLayout(mainLayout);
		
		GridLayout buttonLayout = new GridLayout(6,1);
		buttonPanel = new JPanel();
		buttonPanel.setLayout(buttonLayout);
		
		grayscaleButton = new JButton("Grayscale");
		grayscaleButton.addActionListener(this);
		grayscaleButtonParallel = new JButton("Grayscale Parallel");
		grayscaleButtonParallel.addActionListener(this);
		seqBlur = new JButton("Sequential Blur");
		seqBlur.addActionListener(this);
		parallelBlur = new JButton("Parallel Blur");
		parallelBlur.addActionListener(this);
		seqEqualization = new JButton("Sequential Equalization");
		seqEqualization.addActionListener(this);
		parallelEqualization = new JButton("Parallel Equalization");
		parallelEqualization.addActionListener(this);
		optimized = new JButton("Optimized Parallel E.");
		optimized.addActionListener(this);
		redEye = new JButton("Remove Red-Eye");
		redEye.addActionListener(this);
		imageClone = new JButton("Seamless Image Clone");
		imageClone.addActionListener(this);
		sepiaSeq = new JButton("Sepia Sequential");
		sepiaSeq.addActionListener(this);
		sepiaPar = new JButton("Sepia Parallel");
		sepiaPar.addActionListener(this);
		buttonPanel.add(grayscaleButton);
		buttonPanel.add(grayscaleButtonParallel);
		buttonPanel.add(seqBlur);
		buttonPanel.add(parallelBlur);
		buttonPanel.add(seqEqualization);
		buttonPanel.add(parallelEqualization);
		buttonPanel.add(optimized);
		buttonPanel.add(imageClone);
		buttonPanel.add(sepiaSeq);
		buttonPanel.add(sepiaPar);
		mainFrame.add(buttonPanel);
		mainFrame.add(drawingPanel);
		
		timeLabel = new JTextArea("Time Label");
		timeLabel.setEditable(false);
		buttonPanel.add(timeLabel);
		
		deviceGroup = new ButtonGroup();
		createButtons();
		mainFrame.setVisible(true);
	}
	
	/**
	 * Handles the actions performed for the GUI.
	 * 
	 * @param ae the action event that triggered the method
	 */
	public void actionPerformed(ActionEvent ae) {
		if(ae.getSource() == open) {
			JFileChooser chooser = new JFileChooser();
			if(chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
				File tempFile = chooser.getSelectedFile();
				try {
					handler.loadFile(tempFile);
					mainFrame.repaint();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(mainFrame, "File Not Found", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		else if(ae.getSource() == grayscaleButton) {
			long startTime = System.nanoTime();
			boolean temp = handler.grayScale();
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
			boolean temp;
			try {
				temp = handler.saveFile();
				if(temp) {
					JOptionPane.showMessageDialog(mainFrame, "File Saved", "Save", JOptionPane.OK_OPTION);
				}
				else {
					JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
				}
			} catch (IOException e) {
				JOptionPane.showMessageDialog(mainFrame, "The File cound not be saved", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == saveAs) {
			JFileChooser chooser = new JFileChooser();
			if(chooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
				File newFile = chooser.getSelectedFile();
				if(!chooser.getSelectedFile().getAbsolutePath().contains(".png")){
					 newFile = new File(chooser.getSelectedFile().getAbsolutePath() + ".png");
				}
				boolean saved = handler.saveAsFile(newFile);
				if(!saved) {
					JOptionPane.showMessageDialog(mainFrame, "The File cound not be saved", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		else if(ae.getSource() == close) {
			handler.close();
			mainFrame.repaint();
		}
		else if(ae.getSource() == grayscaleButtonParallel) {
			long temp = handler.grayScaleParallel();
			if(temp > 0) {
				double time = temp / 1000000;
				timeLabel.setText("Parallel Grayscale Algorithm Time in ms:\n" + time);
				mainFrame.repaint();
			}
			else {	
				JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == seqBlur) {
			Scanner input = new Scanner(System.in);
			System.out.println("Enter the size of the filter for the blur: ");
			int blurWidth = input.nextInt();
			System.out.println("Enter the sigma for the blur: ");
			int sigma = input.nextInt();
			long temp = handler.sequentialBlur(blurWidth, sigma);
			if(temp > 0) {
				long runTime = temp / 1000000;
				timeLabel.setText("Sequential Blur Algorithm Time in milliseconds:\n" + runTime);
				mainFrame.repaint();
			}
			else {
				JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == parallelBlur){
			Scanner input = new Scanner(System.in);
			System.out.println("Enter the size of the filter for the blur: ");
			int blurWidth = input.nextInt();
			System.out.println("Enter the sigma for the blur: ");
			int sigma = input.nextInt();
			long temp = handler.parallelBlur(blurWidth, sigma);
			if(temp > 0) {
				timeLabel.setText("Parallel Blur Algorithm Time in nanoseconds:\n" + temp);
				mainFrame.repaint();
			}
			else {
				JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == seqEqualization) {
			double time = handler.sequentialEqualization();
			if(time > 0) {
				timeLabel.setText("Sequential Equalization ms: " + time);
				mainFrame.repaint();
			}
			else {
				JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == parallelEqualization) {
			double time = handler.UnoptimizedParallelEqualization();
			if(time > 0) {
				double ms = time / 1000000;
				timeLabel.setText("Unoptimized Histogram Algorithm ms:\n " + ms + " ms");
				mainFrame.repaint();
			}
			else {
				JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == optimized) {
			double time = handler.parallelEqualization();
			if(time > 0) {
				double ms = time / 1000000;
				timeLabel.setText("Optimized Histogram Algorithm ms:\n " + ms + " ms");
				mainFrame.repaint();
			}
			else {
				JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == redEye) {
			double time;
			try {
				time = handler.redEyeRemoval();
				if(time > 0) {
					double ms = time / 1000000;
					timeLabel.setText("Red-Eye Removal Time ms:\n" + ms + " ms");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if(ae.getSource() == imageClone) {
			JFileChooser chooser = new JFileChooser();
			if(chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
				File source = chooser.getSelectedFile();
				BufferedImage ri;
				try {
					ri = ImageIO.read(source);
					BufferedImage tempImage = new BufferedImage(ri.getWidth(), ri.getHeight(), 
							BufferedImage.TYPE_INT_ARGB);
					Graphics g = tempImage.getGraphics();
					g.drawImage(ri, 0, 0, null);
					double time = handler.seamlessImageClone(tempImage);
					if(time > 0) {
						double ms = time / 1000000;
						timeLabel.setText("Image Clone Algorithm ms:\n " + ms + " ms");
						mainFrame.repaint();
					}
					else {
						JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else if(ae.getSource() == sepiaSeq) {
			double time = handler.sepiaSequential();
			if(time > 0) {
				double ms = time / 1000000;
				timeLabel.setText("Sepia Sequential Algorithm ms:\n " + ms + " ms");
				mainFrame.repaint();
			}
			else {
				JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() == sepiaPar) {
			double time = handler.sepiaParallel();
			if(time > 0) {
				double ms = time / 1000000;
				timeLabel.setText("Sepia Parallel Algorithm ms:\n " + ms + " ms");
				mainFrame.repaint();
			}
			else {
				JOptionPane.showMessageDialog(mainFrame, "No Image Selected", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if(ae.getSource() instanceof JRadioButtonMenuItem){
//			if(ae.getSource() == normal){
//				
//			}
//			else if(ae.getSource() == plusFifty){
//				
//			}
//			else if(ae.getSource() == plusOneFifty){
//				
//			}
//			else if(ae.getSource() == plusTwoFifty){
//				handler.adjustSize(3);
//				mainFrame.repaint();
//			}
//			else if(ae.getSource() == plusThreeFifty){
//				handler.adjustSize(3.5);
//				mainFrame.repaint();
//			}
//			else if(ae.getSource() == doubleZoom){
//				handler.adjustSize(2);
//				mainFrame.repaint();
//			}
//			else if(ae.getSource() == tripleZoom){
//				handler.adjustSize(3);
//				mainFrame.repaint();
//			}
//			else if(ae.getSource() == quadZoom){
//				handler.adjustSize(4);
//				mainFrame.repaint();
//			}
//			else if(ae.getSource() == fiveZoom){
//				handler.adjustSize(500);
//				mainFrame.repaint();
//			}
//			else{
				boolean found = false;
				Iterator<JRadioButtonMenuItem> theIter = buttons.keySet().iterator();
				while(theIter.hasNext() && !found) {
					JRadioButtonMenuItem temp = theIter.next();
					if(ae.getSource() == temp) {
						cl_device_id newDevice = buttons.get(temp);
						handler.setSelectedDevice(newDevice);
						handler.setSelectedPlatform(theDevices.get(newDevice));
					}
				}
//			}
		}
	}
	
	/**
	 * Creates the buttons necessary for the available devices.
	 */
	private void createButtons() {
		cl_platform_id[] thePlatforms = DeviceSetUp.getPlatformIDs();
		for(int i = 0; i < thePlatforms.length; i ++) {
			for(cl_device_id temp: theDevices.keySet()) {
				String deviceName = DeviceSetUp.getDeviceName(temp);
				if(deviceName == null) {
					System.out.println("flag");
				}
				if(temp == null) {
					System.out.println("flag");
				}
				JRadioButtonMenuItem newButton = new JRadioButtonMenuItem(deviceName);
				deviceGroup.add(newButton);
				devices.add(newButton);
				deviceNames.put(deviceName, temp);
				buttons.put(newButton, temp);
				if(temp.equals(handler.getSelectedDevice())) {
					newButton.setSelected(true);
				}
			}
		}
	}
	
	/**
	 * Sets the device at the beginning of the program.
	 */
	private void setDefaultGPU() {
		Iterator<cl_platform_id> iter = theDevices.values().iterator();
		boolean gpuFound = false;
		while(iter.hasNext() && !gpuFound) {
			cl_platform_id temp = iter.next();
			int numberOfGpuDevicesArray[] = new int[1];
			CL.clGetDeviceIDs(temp, CL.CL_DEVICE_TYPE_GPU,0, null, numberOfGpuDevicesArray);
			int numberOfDevices = numberOfGpuDevicesArray[0];
			if(numberOfDevices > 0) {
				cl_device_id devicesArray[] = new cl_device_id[numberOfDevices];
				CL.clGetDeviceIDs(temp, CL.CL_DEVICE_TYPE_GPU, numberOfDevices, devicesArray, null);
				handler = new ImageHandler(drawingPanel, devicesArray[0], temp);
			}
		}
	}
}
