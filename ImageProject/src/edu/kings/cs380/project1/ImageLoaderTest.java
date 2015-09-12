package edu.kings.cs380.project1;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

/**
 * Tests the class of ImageLoader.
 * 
 * @author Robert
 * @version 9.11.2015
 */
public class ImageLoaderTest {

	/**The test instance of ImageLoader.*/
	private ImageLoader testLoader;
	
	/**
	 * Constructor for this test class.
	 */
	public ImageLoaderTest() {
		testLoader = new ImageLoader();
	}
	
	/**
	 * Tests the loadFile method.
	 * 
	 * @throws IOException If file cannot be read
	 */
	@Test
	public void testLoadFile() throws IOException {
//		File test = new File("Balloons.png");
//		BufferedImage actual = ImageIO.read(test);
//		BufferedImage tempImage = testLoader.loadFile(test);
//		assertSame(actual, tempImage);
	}
	
	/**
	 * Tests that loadFile throws and exception when necessary.
	 * 
	 * @throws IOException when the file cannot be read
	 */
	@Test(expected = IOException.class)
	public void testLoadFileFail() throws IOException {
		File test = new File("failer");
		testLoader.loadFile(test);
	}
	
	/**
	 * Tests the saveFile method.
	 * @throws IOException  If file cannot be read
	 */
	@Test
	public void testSaveFile() throws IOException {
//		File test = new File("Balloons.png");
//		BufferedImage actual = ImageIO.read(test);
//		testLoader.saveFile(actual, test);
//		BufferedImage duplicate = ImageIO.read(test);
//		assertSame(duplicate, actual);
	}
	
	/**
	 * Tests that saveFile throws an exception when given an invalid file.
	 * 
	 * @throws IOException when the file cannot be read
	 */
	@Test(expected = IOException.class)
	public void testSaveFileInvalidFile() throws IOException {
		// code here to make and invalid file and a //
		// valid image and try to save //
	}
	
	/**
	 * Tests that saveFile throws an exception when given an invalid image.
	 * 
	 * @throws IOException when the file cannot be read
	 */
	@Test(expected = IOException.class) 
	public void testSaveFileInvalidImage() throws IOException {
		// code here to make a valid file and an //
		// invalid image and try to save //
	}
}
