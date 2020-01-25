/**
 * Basic test to make sure the TFSDiskInputOutput class correctly does its job of providing an
 * API for creating a file to emulate the disk and to enable writing and reading to this "disk."
 * 
 * Screenshots of the output console output and the resulting file are included in the Project's
 * ScreenShots folder.
 * 
 * @author Oloff Biermann
 * @version 8.77
 */

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import tfs.TFSDiskInputOutput;

public class Driver_TFSDiskInputOutput 
{

	public static void main(String[] args) throws UnsupportedEncodingException 
	{
		String fileName = "TestFile";
		byte[] bName = fileName.getBytes("UTF-8");			//Convert string to an array of bytes.
		
		System.out.println("Creating emulated file using tfs_dio_create() returns: " + 				//Test tfs_dio_create()
		TFSDiskInputOutput.tfs_dio_create(bName, 1024, 128));
		
		System.out.println("Opening the emulated disk using tfs_dio_open() returns:" +               //Test tfs_dio_open()
		TFSDiskInputOutput.tfs_dio_open(bName, 1024, 128));
		
		System.out.println("Calling tfs_dio_getSize() shows that file has "+           //Test tfs_dio_getSize()
		TFSDiskInputOutput.tfs_dio_getSize() + " blocks since length is 1024 bytes.");
		
		System.out.println("\nTest tfs_dio_writeBlock():");
		System.out.println("The following string will be written into the file at block 0, without quotes: \"This is BLOCK 0\" ");
		System.out.println("Similar content written to block 1: \"This is BLOCK 1\" ");
		String testStr1 = "This is BLOCK 0";
		String testStr2 = "This is BLOCK 1";
		byte [] testBuf1 = testStr1.getBytes("UTF-8");
		byte [] testBuf2 = testStr2.getBytes("UTF-8");
		
		System.out.println("Writing to block 0 returns: " + TFSDiskInputOutput.tfs_dio_writeBlock(0, testBuf1));					//Test tfs_dio_writeBlock
		
		System.out.println("Write to next block, block 1. Writing returns: " +TFSDiskInputOutput.tfs_dio_writeBlock(1, testBuf2));
		
		byte [] inputBuf1 = new byte [128];
		byte [] inputBuf2 = new byte[128];
		
		System.out.println("\nRead blocks using tfs_dio_readBlock(): \nReading block 0 returns: " + TFSDiskInputOutput.tfs_dio_readBlock(0, inputBuf1));	//Test tfs_dio_readBlock ()
		System.out.println("Reading block 1 returns: " + TFSDiskInputOutput.tfs_dio_readBlock(1, inputBuf2));
		
		System.out.println("Contents of buffer read from from block 0: " + new String(inputBuf1, "UTF-8"));		
		System.out.println("Contents of buffer read from block 1: " + new String(inputBuf2, "UTF-8"));   
		
		System.out.println("\nClosing emulated disk file returns: " + TFSDiskInputOutput.tfs_dio_close());							  	//Test tfs_dio_close()
		
		
	}

}
