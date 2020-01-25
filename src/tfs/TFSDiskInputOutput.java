package tfs;

/**
 *    TFSDiskInputOutput.java
 *    
 *    Class which defines a disk I/O API for the TFS. Used to create a file on disk which emulates a
 *    physical disk partition.
 *    
 *    Class provides several static methods which allow the creation of the emulated disk, opening
 *    of the disk for access, closing the disk to end the session, reading and writing blocks 
 *    to disk, as well as a method for returning the size of the emulated disk.
 *   
 *      
 *
 *    @author Oloff Biermann
 *    @version 8.77
 *
**/

import java.io.*;


public class TFSDiskInputOutput 
{

	
	private static int blockSize;
	private static RandomAccessFile tFS_Disk;			//The raf for the "disk."
	
	/**
	 * Creates the file for emulating a disk. Takes arguments for file name,
	 * length, and block size.
	 * 
	 * Any existing file is deleted.
	 * 
	 * @param byte[] fileName The name of the file as an array of bytes.
	 * @param int length The length (size) of the file in bytes.
	 * @param int bSize The block size in this emulated disk.
	 *
	 * @return int 0 if creation was successful, -1 if error encountered.
	 *
	**/
	
	public static int tfs_dio_create(byte [] fileName, int length, int bSize)
	{
			if (length < bSize)		//Length of file cannot be less than block size.
				return -1;
		try
		{
			String strName = new String(fileName, "UTF-8");  //Convert bytes to a string.
			File file = new File(strName);
			
			if (file.exists())              //Delete existing file, it it exists.
				file.delete();                  
			
			file.createNewFile();
		
			blockSize = bSize;		//Set the constant block length.
		
		} catch (IOException e)
		  {
			return -1;				
		  }
		  	
		return 0;					
		
	}
	
	
	/**
	 * Opens the virtual disk file for access. Operations are supported
	 * using a RandomAccessFile. Length of this file is set.
	 * 
	 * Method will not continue if the specified volume does not exist.
	 * 
	 * @param byte[] name Name of file.
	 * @param nlength length of file.
	 * @return int -1 if error, 0 if success.
	**/
	
	public static int tfs_dio_open(byte[] name, int nlength, int bSize)
	{
		try{ 
		String fN = new String(name, "UTF-8");         // for UTF-8 encoding, convert bytes to string.
		
		if (!(new File(fN).exists()))                  //If virtual disk doesn't exist, return -1.
				return -1;
		
		tFS_Disk = new RandomAccessFile(fN, "rw");		//Create RAF to enable random access to the "disk."
		tFS_Disk.setLength(nlength);
		blockSize = bSize;												//Set block size.
	
		}catch(Exception e)
			{
				e.printStackTrace();
				return -1;
			}
			
		return 0;
	}
	
	/**
	 *  Opens an existing virtual disk for access.
	 *  
	 * @param name
	 * @param bSize
	 * @return int 0 if success, -1 if error occurs in opening
	 */
	public static int tfs_dio_open_existing(byte[] name, int bSize)
	{
		try{
			String fN = new String(name, "UTF-8");         // for UTF-8 encoding, convert bytes to string.
			if( !(new File(fN).exists()) )                  //If virtual disk doesn't exist, return -1.
			{	
				return -1;
			}
						
			tFS_Disk = new RandomAccessFile(fN, "rw");		//Create RAF to enable random access to the "disk."
			blockSize = bSize;
			} catch (Exception e)
			{
				e.printStackTrace();
				return -1;
			}
		
		return 0;
		  
				
	}
	
	
	
	/**
	 *   Get the number of blocks in total in the emulated disk file.
	 *
	 *	@return int number of total blocks in file. -1 if error occurs
	 **/

	public static int tfs_dio_getSize()
	{
		try{
		return ((int)(tFS_Disk.length() / blockSize));	//Length divided by block size gives number of blocks.
		
		} catch (IOException ex)
		{
			return -1;
		}
	}
	
	
	/**
	 *  Reads all bytes from a single block from the emulated disk into memory in the
	 *  buffer provided as argument. Checks to make sure the size of the buffer
	 *  is large enough to hold one block.
	 *
	 *  @param blockNum The block number in the disk file.
	 *  @param buf byte[] Buffer to read block into.
	 *  @return int 0 if success, -1 if error encountered.
	 *
	 **/
	public static int tfs_dio_readBlock(int blockNum, byte[] buf)
	{
		if (buf.length < blockSize)			//Make sure buffer is big enough.
			return -1;
		
		if (blockNum > tfs_dio_getSize())  //Make sure this is a valid block number.
		{
			return -1;
		}
		
		//The offset for the block to be read in the RAF. Calculated as block number x block size.
		// Block numbers are from 0, as is the current file pointer.
		long fileOffset = blockNum * blockSize;					
		
		try {
		
				tFS_Disk.seek(fileOffset);		        //Move the pointer to correct offset at beginning of the block.
				tFS_Disk.readFully(buf, 0, blockSize);	//Copy all bytes from this block into the buffer.		
				
			}catch (IOException e)
			{
				return -1;
			}
	
		return 0;
	}
	
	/**
	 *   Writes all bytes to a single block in the emulated disk file
	 *   from the buffer passed. Checks to make sure buffer is not too big.
	 *
	 *  @return int 0 if write is successful, -1 if error.
	 **/
	public static int tfs_dio_writeBlock(int blockNum, byte[] buf)
	{
		if (buf == null)								    //Case for writing an empty block. No need to actually write.
			return 0;
		
		if (buf.length > blockSize)			//Make sure buffer is NOT greater than block size.
			return -1;
		
		if (blockNum > tfs_dio_getSize())  //Make sure this is a valid block number.
		{
			return -1;
		}
		
		long fileOffset = blockNum * blockSize;   //Calculate current file pointer offset.
		try
		{
			tFS_Disk.seek(fileOffset);
			tFS_Disk.write(buf, 0, buf.length);			//Write all bytes from the buffer into the block.
		
		}catch (IOException ex)
		 {
		   return -1;
		 }
		 
		 return 0;
		 
	}
	
	/**
	 * 
	 * @return The block size for this emulated disk.
	 */
	public static int getBlockSize()
	{
		return blockSize;
	}
	
	/**
	 *  Closes the file used to emulate the disk.
	 * 
	 *  @return int 0 if success, -1 if closing failed.
	 **/
	public static int tfs_dio_close()
	{
		try{
			  
			  tFS_Disk.close();				//Call close method of RandomAccessFile.	
		
			} catch(Exception e)
			  {
				return -1;
			  }
			  
		
		return 0;
	}
	
	
}
