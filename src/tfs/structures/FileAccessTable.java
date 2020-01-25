package tfs.structures;

/**
 * FileAccessTable.java
 * 
 * Defines a File Access Table (FAT) to be used to keep track of the different blocks which make
 * up the files and directories stored in the file system.
 * 
 * A simple array is used to store the table. The size of the table is set when the object
 * is instantiated. This size should be the number of logical blocks the table will manage. So 
 * this is the number of blocks of disk space used in the file system, if the table will be 
 * used to manage the entire file system.
 * 
 * Each array INDEX corresponds to a block, and holds the value of the next block which makes
 * up a file or anything else stored in the file system. Free blocks are marked as 0. All 
 * entries are set to 0 initially.
 *  
 * Methods are provided to determine the total number of blocks represented by the table
 * change an entry, access an entry to determine next block, as well as to return a string 
 * representation of the table. Finally, a method is provided which returns a copy of the table.
 *  
 * 
 * @author Oloff Biermann
 * @version 8.77
 *
 */

public class FileAccessTable 
{
	
	private int [] FAT;
	private int size;
		
	
	/**
	 * Constructor sets all entries in FAT to 0. Sets number of free blocks to size.
	 * @param int size Specifies the number of blocks in the table.
	 */
	public FileAccessTable(int size)
	{
		FAT = new int[size];
		for (int i = 0; i < FAT.length; i++)
			FAT[i] = 0;
					
	}
	
	/**
	 * Get the total number of blocks represented by the table.
	 * 
	 * @return The number of indexes in the table.
	 */
	public int getSize()
	{
		return size;
	}
	
	/**
	 * Set an entry in the table. 
	 * 
	 * @param index The index in the table.
	 * @param entry The entry in the table.
	 * 
	 * @return 0 if success, -1 if failure.
	 */
	public int setEntry(int index, int entry)
	{
		if (entry >= FAT.length || index >= FAT.length || index < 0)	 //Invalid entry or index.
		{	
			return -1;
		}
		
		FAT[index] = entry;				//Otherwise set the index in table to given entry.
		return 0;
		
	}
	
	
	/**
	 * Get the table entry at this index.
	 * 
	 * @param index The index for the entry to be accessed.
	 * @return The table entry. -2 is returned if index is invalid.
	 */
	  public int getEntry(int index)
	  {
		  if (index >= FAT.length || index < 0)
			  return -2;
		  
		  return FAT[index];
	  }
      	
	  /**
	   * Method returns a copy of the File Access Table so that it can be written to disk
	   * or otherwise used in memory.
	   * 
	   * @return A copy of the File Access Table.
	   */
	  
	  public int[] getTable()
	  {
		  return FAT;
	  }
	  
	  
	  /**
	   * Allows the table to be set.
	   * 
	   */
	  
	  public void setTable(int[] replaceFAT)
	  {
		  FAT = replaceFAT;
	  }
	  
	  /**
	   * Get a string representation of the table in format: 
	   * Index  :  Value
	   * 
	   * @return A String representing this FAT.
	   */
	  public String toString()
	  {
		  String result = "File Access Table";
		  for (int in = 0; in < FAT.length; in++)
		  {
			  result += "\n"+ in + ". " + FAT[in];
		  }
		  
		  return result;
	 
	  }
	  
	  
	  
	  
	
}
