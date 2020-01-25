package tfs.structures;
/**
 * FDT.java
 * 
 * Class maintains the File Descriptor Table for the TFS file system in memory. The goal
 * is to keep track of every open file for the process. Essentially, the functionality of a
 * iNode table and system-wide open file table is combined with that of a per-process
 * file descriptor table in this class.
 * 
 * The key way in which the FDT works is that it returns a file descriptor when an entry
 * is added. This allows that file's entry to be updated and removed easily.
 * 
 * The FDT is simply implemented using two arrays.
 * The first array holds copies of the File Control Block for each open file. This allows
 * file name, location (starting block number), and length in bytes to be stored for every open file.
 *
 * The second array holds the current file pointer (position) for each entry. Indexes in the second
 * table correspond to FCB entries in the first table.
 * 
 * Several methods are provided for accessing and modifying entries in the two tables. This is to 
 * keep track of opening and closing operations for files in the system.
 * 
 * @author Oloff Biermann
 * @version 8.77
 *
 */

public class FDT 
{
	private int counter;                  //Counter to keep track of number of entries in FDT.
	private FCB [] cntrlBlockTable;       //The first table for storing FCB's
	private int [] offsetTable;           //Second table for storing offsets (current file pointers) for each entry in first table.
	
	/**
	 *   Constructor takes only 1 argument for max size of table. This is used to initialize the two arrays with an appropriate capacity.
	 *   Table size determines how many files may be open on the system concurrently.
	 *
	 *    @param size The size of the table.
	 *
	 **/
	public FDT(int size)
	{
		counter = 0;                         //Initialize counter to 0. 
		cntrlBlockTable = new FCB[size];     //Initialize first table holding FCB's.
		offsetTable = new int[size];         //Initialize second table for offsets.
	}
	
	/**
	 *   Returns the capacity of the FDT.
	 *
	 *   @return capacity of FDT.
	 *
	 **/
	 public int getCapacity()
	 {
		return cntrlBlockTable.length;
	 
	 }
	 /**
	  *   Method to check if FDT is full.
	  *
	  *   @return boolean true if table is full, false otherwise.
	  *
	  */
	 public boolean isFull()
	 {  
		return ((counter >= getCapacity()) ? true : false);
	 }
	 
	 /**
	  *   Checks to see if table is empty.
	  *
	  *   @return boolean true if table is empty, false otherwise.
	  */
	  public boolean isEmpty()
	  {
		return ((counter == 0) ? true : false);
	  }
	 
	/**
	 * Adds an entry to the FDT. Both tables are updated.
	 * If the entry is already in the FDT, the entry is updated instead.
	 * 
	 * @param cntrlblck Pointer to FCB representing this open file.
	 * @param offset int offset within this open file, the current file pointer. Should be 0
	 * for a new file to be opened.
	 * @throws IllegalArgumentException if offset is invalid
	 * @throws RuntimException if FDT is full
	 * 
	 * @returns int File Descriptor for this entry.
	 * 
	 */
	public int add(FCB cntrlblck, int offset)
	{
		if (offset < 0 || offset > cntrlblck.getSize())            //Invalid offset
			throw new IllegalArgumentException("Invalid offset");
			
		if (isFull()) 								//Table is full.
			throw new RuntimeException("FDT full. No more files may be opened currently.");
		
		int i = 0;
		//Otherwise add entry data to both tables. Find empty space in tables.
		for (i = 0; i < cntrlBlockTable.length && cntrlBlockTable[i] != null; i++)
			{ }
		
		cntrlBlockTable[i] = cntrlblck;
		offsetTable[i] = offset;        //Add entries to both tables.
			
		counter++;                    //Increment counter to indicate another entry added.
		
		return i;
	}
	
	
	/**
	 *   Search the table to determine if a given entry is in the table.	
	 *	 Entries are actually compared by FCB.equals method. First check if table is empty.
	 *		
	 *   @param target An FCB object representing the file to be located in table.
	 *   @return boolean true if file entry is in FDT, or false otherwise
	 *   @throws IllegalArgumentException if fd is invalid.
	 **/
	public boolean isOpen(int fd)
	{
		if (fd < 0 || fd >= offsetTable.length)
			throw new IllegalArgumentException("File descriptor invalid");
		
		if (isEmpty())
			return false;
		
		if (cntrlBlockTable[fd] != null)
		{
			return true;
		}
		
		return false;		//If not found, return -1;
	}
	
	/**
	 * Overloaded version of isOpen(int fd).
	 * Checks if this entry is open by looking at FCB param
	 * and attempting to find it in table.
	 * 
	 * @param f The FCB for the element for which to check if 
	 * it is open in the file system.
	 * @return boolean true if the element is open, false otherwise
	 */
	public boolean isOpen(FCB f)
	{
		if (this.getFD(f) >= 0) //If entry found in FDT, return true.
			return true;
		
		return false;      //Otherwise return false, meaning elem is not open.
		
	}
	
	/**
	 * Get the FCB for the entry associated with fd.
	 * 
	 * @param fd File descriptor associated with an entry to locate
	 * @return The FCB for entry associated with fd, or null if no such entry exists in FDT.
	 * @throws IllegalArgumentException if fd is out of range of table size.
	 */
	public FCB getFCB(int fd)
	{
		if (!(isOpen(fd)))
			return null;                  //Return null if not found. 
		
		return cntrlBlockTable[fd];       //Otherwise return FCB for entry associated with fd.
		
	}
	
	/**
	 * Get the fd associated with an entry by looking at the FCB argument.
	 * Location, name, and type of entry must match. Returns -1 if they do not,
	 * which means element associated with entry is not open in file system
	 * 
	 * @param f FCB for which to return fd
	 * @return fd of entry associated with FCB or -1 if not found
	 */
	
	public int getFD(FCB f)
	{
		if (f == null)
			return -1;
		
		for (int i = 0; i < cntrlBlockTable.length; i++)
		{
			if (cntrlBlockTable[i] != null)         //Make sure entry isn't empty.
			{
				if (cntrlBlockTable[i].equals(f) && cntrlBlockTable[i].getLocation() == f.getLocation())
				{
					return i;
				}
			}
		}
				
		return -1;
	}
	
	
	/**
	 * Get the disk location (block) associated with entry for fd.
	 * 
	 * @param fd File descriptor associated with the entry to locate
	 * @return The disk block (location) for fd.
	 * @throws IllegalArgumentException if fd is out of range or FDT doesn't
	 *  contain this entry.
	 * 
	 */
	
	public int getEntryLocation(int fd)
	{
		if (!(isOpen(fd)))
			throw new IllegalArgumentException("Cannot get location. Entry not found for fd " + fd);         //Throw exception if no entry. 
		
		return cntrlBlockTable[fd].getLocation();       //Otherwise return FCB for entry associated with fd.
	}
	
	/**
	 * Get the offset within the file (current file pointer) for the entry
	 * associated with fd.
	 * 
	 * @param fd File descriptor associated with an entry to locate
	 * @return int File offset for entry associated with fd, or -1 if no such entry exists.
	 * @throws IllegalArgumentException if fd is out of range of table size
	 */
	public int getOffset(int fd)
	{
		if (!(isOpen(fd)))
			return -1;                  //Return null if not found. 
		
		return offsetTable[fd];       //Otherwise return FCB for entry associated with fd.
	}
	
	
	/**
	 * Updates offset in file for an entry. Use file descriptor
	 * to access entry and update.
	 * 
	 * @param fd File descriptor of file entry to update
	 * @param updatedOffset New value for offset for this entry.
	 * @throws InvalidArgumentException if fd is invalid.
	 */
	public void updateOffset(int fd, int updatedOffset)
	{
		if (fd < 0 || fd >= offsetTable.length)
			throw new IllegalArgumentException("File descriptor invalid");
		
		if (cntrlBlockTable[fd] == null)    //If entry doesn't exist.
			throw new IllegalArgumentException("File not open in table");
		//If offset is invalid.
		else if(cntrlBlockTable[fd].getSize() < updatedOffset) 
		{
			throw new IllegalArgumentException("Offset "+updatedOffset+ 
					"is an invalid offset. Offset must not larger than "
					+ "file size or < 0");
		}
		else
			offsetTable[fd] = updatedOffset;
	}
	
	/**
	 * Allows an FCB pointer to be updated for a given entry in table.
	 * 
	 * @param fd File descriptor of file entry to update
	 * @param updatedFCB The updated FCB for this entry
	 * @throws InvalidArgumentException if fd is invalid.
	 */
	public void updateFCB(int fd, FCB updatedFCB)
	{
		if (fd < 0 || fd > offsetTable.length)
			throw new IllegalArgumentException("File descriptor invalid");
		
		if (cntrlBlockTable[fd] == null)    //If entry doesn't exist.
			throw new IllegalArgumentException("File not open in table");
		
		else
			cntrlBlockTable[fd] = updatedFCB;
		
	}
	
	/**
	 *   Removes an entry from the FDT to indicate closure of file.
	 *   Entry is found by fd
	 *
	 *   @param fd file descriptor of entry to remove
	 *   @return int 0 if success or -1 if removal failed.
	 **/
	public int remove(int fd) 
	{
				
		if (this.isEmpty() || fd < 0 || fd >= offsetTable.length)     //If fd is not valid.
			return -1;
			
		cntrlBlockTable[fd] = null;   //Otherwise, remove entry and return index. Both table's values at that index are reset.
		offsetTable[fd] = 0;
		
		counter--;						//Decrement counter to mark removal.
		
		return 0;
		
	}
	
	/**
	 *   Creates a string representation of the FDT. All data, including offset, is added to a string.
	 *
	 *   @return String representation of FDT.
	 *
	 */
	 
	 public String toString()
	 {
		String result = "File Descriptor Table:\n";
		
		for (int j = 0; j < getCapacity(); j++)
		{
			result += j + ".";
			//Call toString() method of each FCB entry to add it to string. Include null (empty) entries.
			if (cntrlBlockTable[j] == null)
				result += " Empty cell\n";
			else
			{
				result +=" " + cntrlBlockTable[j].toString();
				//Now include the offset for each entry in offsetTable. Include empty entries.
				result += "\nOffset within file: " + offsetTable[j]+ "\n\n";
			}
		
		}
		
		return result;
	 	 
	 }
	 
	 
	 
	

}
