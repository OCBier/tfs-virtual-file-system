package tfs.testing;

/**
 * Driver program to test the tfs.structures.FCB class to make sure all methods work correctly. See screenshots
 * in TFS\ScreenShots folder for outputs.
 * 
 * @author Oloff Biermann
 * @version 8.77
 *
 */

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import tfs.structures.FCB;

public class Driver_FCB 
{

	public static void main(String[] args) 
	{
		
		FCB testF = new FCB("Testing File", false, 20, 1024);    //Test FCB for a file.
		FCB testD = new FCB("Testing Dir", true, 27, 128);									//Test FCB for a dir.
		
		System.out.println("Test FCB for file: \n" + testF.toString() + "\n");
		System.out.println("Test FCB for directory: \n" + testD.toString() + "\n");
		
		System.out.println("\nTest setters to change size, isDir, location, and size");
		
		//See what happens if longer name is used ( > 15 bytes). Should be truncated to 15 bytes.
		testF.setName("Modified name is a much longer name");   
		testF.setSize(2048);
		testF.setFileOrDir(false); //Still a file.
		testF.setLocation(50);    //Moved to block 50.
		
		System.out.println("Test getters to print modified name, size, etc.");
		try
		{
			System.out.println("New Name: " + new String(testF.getName(), "UTF-8")); //Get name bytes.
		}catch(UnsupportedEncodingException e)
		{}
		
		System.out.println("New Size: " + testF.getSize()); //Test getSize()
		System.out.println("Still a file, since is_Dir() returns: " + testF.is_Dir());  //Test is_Dir
		System.out.println("New location: " + testF.getLocation());  //Test getLocation().
		
		//Finally, test the getByteArr() method by outputting to a byte[] and then restoring.
		
		byte[] tmpStore = testD.getByteArr();
		ByteBuffer tmpBB = ByteBuffer.wrap(tmpStore);     //Wrap tmpStore into buffer.
		
		//Now move bytes from buffer back into the FCB test_cpy.
		byte[] name = new byte[15];
		byte isDir;
		int location;
		int size;
		
		//Get bytes from buffer.
		tmpBB.get(name, 0, 15);    //Copy first 15 bytes for name into byte array name.
		isDir = tmpBB.get();     
		location = tmpBB.getInt();
		size = tmpBB.getInt();
		
		FCB test_cpy= new FCB();
		
		//Set values of the copy restored from byte array.
		test_cpy.setName(name);
		test_cpy.setFileOrDir(isDir);
		test_cpy.setLocation(location);
		test_cpy.setSize(size);
		
		System.out.println("Load test FCB into byte array and restore:");
		System.out.println("\n\nComparing original FCB and the one that was restored from byte array shows they are the same.");
		System.out.println("\nOriginal:");
		System.out.println(testD.toString());
		System.out.println("\nCopy:");
		System.out.println(test_cpy.toString());
		
		//Test equals() method which compares FCB objects by name.
		System.out.println("\nComparing second test FCB with the one restored from byte buffer using equals() returns: " + testD.equals(test_cpy));
		System.out.println("Comparing first FCB(the file) with the second FCB(directory), which are different, returns: " + testF.equals(testD));
				
				
	}

}
