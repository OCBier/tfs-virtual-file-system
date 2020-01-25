package tfs.structures;

/**
 * FCB.java
 * 
 *  Class FCB represents a file control block. This structure manages several basic attributes of files
 *  and directories.
 *
 *  Attributes are as follows. Shows order in which attributes are output in the byte array
 *  via method getBytes():
 *  	byte[] File or directory name (Max 15 bytes)
 *		byte isDirectory (1 byte)
 *      int Starting block number(4 bytes)
 *      int Total size in bytes (4 bytes)
 *      
 *  The above gives a constant size of 24 bytes, stored in integral constant FCB_Size.
 *
 *  Several methods are provided for accessing and modifying these data members. A method for converting
 *  the FCB to an array of bytes is also provided, called getByteArr(), so that it can easily be stored to disk. 
 *  FCB attributes will be stored in the byte array in the order shown above. This array has a fixed size. 
 *  
 *  Finally, FCB overrides the Object equals method, so that FCB's can be compared.
 *  FCB's are compared by their name in a non-case sensitive manner.
 *
 * @author Oloff Biermann
 * @version 8.77
 *
 */


import java.nio.ByteBuffer;
import java.io.*;

public class FCB
{
	public final static int FCB_SIZE = 24;          //Size is fixed at 24 bytes.
	public final static int MAX_NAME = 15;           //Max length of name stored is 15 bytes.
	private byte[] name = new byte[15];              //Max file or directory name size is 15 bytes.
	private byte isDir;
	private int startingBlock;
	private int size;

	/**
	 * Constructor creates a blank FCB. Takes no args.
	 * 
	 */
	public FCB()
	{
		
	}
	
	/**
	 *    Constructor sets up a PCB initially. Each data member is initialized using the appropriate
	 *    accessors, to make sure valid values are passed.
	 *    
	 *    @param n String with name for file or directory
	 *    @param dir Boolean representing if FCB represents a file or directory
	 *    @param startingBlock int for the disk location of first block of file or directory
	 *    @param numBytes int for size of file or directory in bytes
	 *
	 */
	public FCB(String n, boolean dir, int startingBlock, int numBytes) throws IllegalArgumentException
	{
		setName(n);                  //Set the name.
		setFileOrDir(dir);           //Mark FCB as managing file or dir.
		setLocation(startingBlock);  //Set location of this file or dir.
		setSize(numBytes);				//Set size.		
	
	}
	
	/**
	 *  Overloaded constructor which allows FCB to be created with byte[] name and byte
	 *  representing boolean isDirectory. 
	 *  
	 *  @param bN byte[] with bytes for name string
	 *  @param dir byte representing if FCB is associated with a file or dir
	 *  @param startingBlock int for the disk location of first block of file or directory
	 *  @param numBytes int for size of file or directory in bytes
	 */
	public FCB(byte[] bN, byte dir, int startingBlock, int numBytes)
	{
		setName(bN);
		setFileOrDir(dir);
		setLocation(startingBlock);
		setSize(numBytes);
	}
	
	/**
	 *   Sets the name stored by this FCB. As mentioned, max name size is 15 bytes.
	 *   Any additional bytes will be discarded after string is converted to bytes.
	 *  
	 *
	 *   @param String n The name that will used for this file or directory.
	 **/
	public void setName(String n) 
	{
		byte [] nameBytes = new byte[MAX_NAME];
		
		try
		{
		  nameBytes = n.getBytes("UTF-8");   //Convert name to byte array. UTF-8 encoding.
		} catch (UnsupportedEncodingException e)
			{	
				System.err.println("Error in converting file or directory name");
			}
		
		setName(nameBytes); //Call overloaded version of setName to set the name as byte array.
	
	}
	
	/**
	 * Overloaded version of setName which directly takes an array of bytes to set name.
	 * 
	 * @param byte[] n Bytes of file or directory name. Will be shortened to 15 bytes if > 15 bytes.
	 * 
	 */
	public void setName (byte[] n)
	{
		int nLength = n.length;
		if (nLength > MAX_NAME)      //If the size of the byte array is > max length allows for name, truncate it.
			nLength = MAX_NAME;
			
		//First clear old name data member (byte array).
		name = null;
		name = new byte[MAX_NAME];
		
		for (int i = 0; i < nLength; i++)      //Copy bytes from n into data member name.
		{
			name[i] = n[i];
		}
				
		
		
	}
	
	/**
	 *   Sets the status of this FCB as either managing a file or a directory.
	 *   False means this is a file, true means this is a directory.
	 *
	 *  @param boolean isDir Sets this FCB as a file or a directory
	 *
	 */
	public void setFileOrDir(boolean is_D) 
	{
		isDir = (byte)(is_D ? 1 : 0);      //Note cast to byte.
	}
	
	/**
	 * Overloaded version of setFileOrDir which takes byte arg instead of boolean.
	 * @throws IllegalArgumentException if value is not 0 or 1  
	 */
	 public void setFileOrDir(byte iS) throws IllegalArgumentException
	 {
		 byte f = 0;
		 byte t = 1;
		 if (iS == f || iS == t)    //If arg is 0 or 1, set data member isDir.
			isDir = iS;
		
		 else    //Otherwise throw exception to indicate that invalid arg was given.
		 {
			 throw new IllegalArgumentException();
		 }
		 
	 }
	
	/**
	 * Sets the location (starting block) of this file or directory.
	 * 
	 * @param int s size of file or directory.
	 * @throws IllegalArgumentException if block location is invalid.
	 *
	 **/
	public void setLocation(int block) throws IllegalArgumentException
	{
		if (block < 0)
			throw new IllegalArgumentException("Argument for location (starting block) is not valid");
			
		else
			startingBlock = block;
	
	
	}
	
	/**
	 * Sets the size of the file or directory managed by this FCB.
	 * 
	 * @param int s size of file or directory.
	 * @throws IllegalArgumentException if size is invalid.
	 *
	 **/
	public void setSize(int s) throws IllegalArgumentException
	{
		if (s < 0)
			throw new IllegalArgumentException("Argument for file or directory size invalid.");
			
		else  //If size given is valid, set the size in this FCB.
			size = s;  
	
	}
	/**
	 *  Returns byte array name for name of file or dir.
	 *
	 *  @return byte[] Name of file or dir.
	 *
	 **/
	public byte[] getName()
	{
		return name;
	}
	
	/**
	 * Returns name as a UTF-8 string. Not UTF-16,
	 * since 1 byte chars are used internally. 
	 * 
	 * @return name as UTF-8 formatted string.
	 * 
	 */
	public String getStrName()
	{	
		String strName = ""; 
		try
		{
			strName = new String(name, "UTF-8");
		}catch (UnsupportedEncodingException e)
		{
			System.err.println("Could not convert byte name to string");
			e.printStackTrace();
		}
		return strName;   //The name as string.
		
	}
	
	/**
	 *  Returns true if this FCB manages a directory, or false if it manages a file.
	 *
	 *  @return boolean true if dir, otherwise false.
	 **/
	public boolean is_Dir()
	{
		return (isDir == 1 ? true : false);
	}
	
	/**
	 *  Returns the location of this file or directory on disk. This is the starting block number.
	 *
	 *  @return int starting block number of file or directory.
	 *
	 **/
	public int getLocation()
	{
		return startingBlock;
	}
	
	
	/**
	 *  Gives the size (in bytes) of this file or directory on disk.
	 *
	 *  @return int The length (size) of file or dir on disk in bytes.
	 *
	 **/
	public int getSize()
	{
		return size;
	}
	
	
	/**
	 *  Method for getting string representation of FCB object. Useful for debugging.
	 *
	 *  @return String data stored in this FCB
	 **/
	
	public String toString()
	{
		String tmpName = this.getStrName();		
		
		String result = "Name: " + tmpName + "\nisDirectory: " + isDir;
		result += "\nStarting Block (location): " + startingBlock + "\nSize in bytes: " + size;
		
		return result;
	}
	
	/**
	 *   Method returns FCB object as an array of bytes. This makes saving FCB to disk more convenient.
	 *   Note that array will always have a size constant SIZE(24 bytes) and the
	 *    order in which data members are written is the same as the order in which 
	 *    data members are are declared.
	 *
	 *   @return byte[] Array with length of 24.
	 **/
	 
	 public byte[] getByteArr()
	 {
		ByteBuffer tmpBB = ByteBuffer.allocate(FCB_SIZE);     //Byte buffer to hold all bytes.
		
		tmpBB.put(name);                     //Transfer name into buffer.
		tmpBB.put(isDir);                    //Transfer isDirectory into buffer.
		tmpBB.putInt(startingBlock);        //Put 4 bytes of startingBlock into buffer.
		tmpBB.putInt(size);                // Put 4 bytes of size into buffer.
		
		return tmpBB.array();             //Return the byte array backing this buffer.
	  }
	 
	 
	 /**
	  * Method compares two FCB objects by name and if they are both either directories or files.
	  * Returns true if names are equal(ignoring case) and they are both files or both directories.
	  * 
	  * Overrides Object.equals for FCB objects. 
	  * 
	  * @return boolean true if FCB's have same name and are both associated with files or directories, false otherwise
	  *  
	  */
	 
	 public boolean equals(Object other)
	 {
		 if (!(other instanceof FCB))                       //Can never be equal if argument is not an instance of FCB.
			 return false;
		 
		 String n = this.getStrName();                           //Name stored for this object
		 String otherObjName = ((FCB)other).getStrName();        //Name stored by other FCB. Note case to FCB
		 		 		 		 
		//Return true if the names match, ignoring case, both are files or dirs.
		 if ( (n.equalsIgnoreCase(otherObjName)) && (this.is_Dir() == (((FCB)other).is_Dir())) )  
			 return true;
		 
		 return false;                           //Otherwise return false.
	 }
	 
		
}	

