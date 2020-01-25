package tfs.structures;

/**
 * Manages a directory to be used in the file system, in memory. Uses a doubly linked linear list
 * to store directory entries (FCB objects). The list is an instance of LinkedList and 
 * so it can use the methods implemented by this class from the List interface. 
 * 
 * Methods are provided to perform the various basic functions needed in a directory.
 * 
 * Directory entries are essentially stored as instances of the tfs.structures.FCB class. 
 * This class defines file control blocks. FCB also defines the equals() method so that directory
 * entries be compared using a specified attribute, in this case by their name. See the comments in 
 * FCB class for more details.
 * 
 * @author Oloff Biermann
 * @version 8.77
 */

import tfs.structures.FCB;
import tfs.exceptions.DirModException;
import java.util.*;
import java.nio.ByteBuffer;


public class Directory 
{
	//Create the list of FCB objects to contain this directory.
	private List <FCB> dirList;
	
	
	/**
	 * Constructor for Directory class initializes data member dirList as a pointer to an
	 * an LinkedList storing FCB objects. 
	 * 
	 */
	public Directory()
	{
		dirList = new LinkedList<FCB>();
	}
	
	/**
	 * Add an entry to the directory, if this directory doesn't already contain that entry.
	 * Update() method should be used instead, if the goal is to update an existing directory
	 * entry.
	 * 
	 * @param entry The FCB to be added to the directory as entry.
	 */
	public void addNewEntry(FCB entry) throws DirModException
	{
		if(dirList.contains(entry))   //Throw DirAddException if entry is already in dir. 
			throw new DirModException("Could not add entry for \"" + entry.getStrName() +"\".Directory already contains entry.");
		
		else                          //Otherwise add the entry to directory.
			dirList.add(entry);   
				
	}
	
	/**
	 * Updates an existing entry in this directory. Calls find() to get the index
	 * of the entry and then overwrites the entry which was found.
	 * 
	 * 
	 * @param updatedEntry The updated version of an existing entry.
	 * @throws DirModException if the directory does not contain the entry.
	 */
	public void update(FCB updatedEntry) throws DirModException
	{
		int index = find(updatedEntry);
		
		if (index < 0)  //DirModException thrown if update fails.
			throw new DirModException("Cannot update entry since it does not exist in this directory");
		
		//Otherwise, update the entry which was found.
		else
		{
			dirList.set(index, updatedEntry);    //Replace entry with new entry.
		}
				
	}
	
	/**
	 *   Updates entry name only, using the name provided by second arg. 
	 *   Basically makes updating entry names more convenient.
	 *   
	 *  @param original The original name of entry.
	 *  @param updatedName The new name to store for this entry.
	 *  @param dOrF boolean to specify is target is directory or file.
	 *  @throws DirModException if the directory does not contain the entry.
	 */
	 public void updateEntryName(String original, String updatedName, boolean dOrF) throws DirModException
	 {
		int index = find(original, dOrF);    //Find by string.
		
		if (index < 0)
			throw new DirModException("Could not update \"" +original +"\". Target not found in directory.");
		
		else
		{
			FCB tmp = dirList.get(index);                          //Get the target to update
			tmp.setName(updatedName);                             //Update name only.
			dirList.set(index, tmp);					          //Write updated entry back into directory.
		}
	 }
	 
	 /**
	  *   Updates disk location associated with this entry only.
	  *   (starting block) only, using the blockNum provided by second arg. 
	  *   More convenient of updating location only.
	  *   @param name String Name of entry to update.
	  *   @param updatedLoc int The new disk location (block) of entry.
	  *   @param dOrF boolean to specify is target is directory or file.
	  *   @throws DirModException if the directory does not contain the entry.
	  */
	  
	  public void updateEntryLoc(String name, int updatedLoc, boolean dOrF) throws DirModException
	  {
		int index = find(name, dOrF);                  //Find by string name.
		
		if (index < 0)
			throw new DirModException("Could not update location for. \"" +name +"\". Target not found in directory.");
			
		FCB tmp = dirList.get(index);                          //Get the target to update
		tmp.setLocation(updatedLoc);                             //Update location only.
		dirList.set(index, tmp);					          //Write updated entry back into directory.
	  
	  }
	  
	  
	   /**
	  *   Updates size of file or directory associated with this entry only. 
	  *   Uses the updated size provided are argument. 
	  *   More convenient of updating location only.
	  *   @param name String Name of entry to update.
	  *   @param updatedSize int The new size of dir or file managed by this entry.
	  *   @param dOrF boolean to specify is target is directory or file.
	  *   @throws DirModException if the directory does not contain the entry.
	  */
	  
	  public void updateEntrySize(String name, int updatedSize, boolean dOrF) throws DirModException
	  {
		int index = find(name, dOrF);                  //Find by string name.
		
		if (index < 0)
			throw new DirModException("Could not update size for \"" +name +"\".. Target not found in directory.");
			
		FCB tmp = dirList.get(index);                          //Get the target to update
		tmp.setSize(updatedSize);                             //Update size only.
		dirList.set(index, tmp);					          //Write updated entry back into directory.
	  
	  
	  }
	
	/**
	 * Finds a target within the directory and returns its index. 
	 * 
	 * @param target The FCB to find.
	 * @return Index of target in directory, or -1 if it was not found.
	 */
	public int find(FCB target)
	{
		return dirList.indexOf(target);
	}
	
	
	/**
	 * Overloaded version of find method. Uses entry name instead of FCB target to
	 * locate and return the index of an entry in the directory list.
	 * 
	 * @param name of target entry in directory
	 * @param dOrF boolean to tell if target is directory (true) or file (false)
	 * @return int Index of target entry specified by name, or -1 if not found
	 * 
	 */
	
	public int find(String name, boolean dOrF)
	{
		for (int i = 0; i < dirList.size(); i++) //Iterate through list and look at names of entries
		{
			FCB current = dirList.get(i);
			
			/*
			 *  Uses FCB.equals method by creating another temporary FCB and comparing with current entry, FCB current.
			 *  Return index of current entry if it matches name and is also either a directory or file.
			 */
			if( (new FCB(name, dOrF, 1, 1)).equals(current) )    
				return i;
		}
		
		//If not found, return -1;
		return -1;
	}
	
	/**
	 * Determine if this directory contains the specified file or directory.
	 * 
	 * @param name The name of file or directory to check for
	 * @param dOrF boolean to tell if target is file or directory
	 * 
	 * @return boolean true if this directory contains the specified file or directory, false otherwise
	 * 
	 */
	public boolean contains(String name, boolean dOrF)
	{
		return ( ((this.find(name, dOrF)) >= 0) ? true : false); 
	}
	
		
	/**
	 * Finds a target FCB within the directory by name. Returns the FCB associated
	 * with this entry, or null if it is not found. 
	 * 
	 * @param name containing name of entry to find.
	 * @param dOrF Boolean to specify if target is directory or not (file)
	 * @return The FCB that was found in the directory containing the given name, or null
	 * if not found.
	 * 
	 */
	
	public FCB getFCBByName(String name, boolean dOrF)
	{
		int index = find(name, dOrF);
		
		if (index < 0)  //Return null pointer if element could not be found.
			return null;
		
		return dirList.get(index);
	}
	
	/**
	 * Removes an entry from the directory, it it exists.
	 * 
	 * @param victim
	 * @return The FCB of entry that was removed
	 * @throws DirModException Thrown if entry could not be found for removal.
	 */
	public FCB removeEntry(FCB victim) throws DirModException
	{
		//If list is empty, throw a DirModException.
		if (dirList.isEmpty())
			throw new DirModException("Directory is empty.");
				
		int index = find(victim);    //Attempt to get index of FCB to remove
		
		//If element is not found, throw DirModException
		if (index < 0)
			throw new DirModException("Entry could not be found for removal");
		
		//Otherwise, if element is found, return this element.
		else
		{
			return dirList.remove(index);
		}
		
	}
	
	/**
	 * Overloaded version of removeEntry. Finds associated FCB of the name specified
	 * in argument. For convenience, uses getByName to find the FCB. 
	 * 
	 * @param Name String name of element to remove
	 * @param dOrF Boolean to tell if entry to remove is file or directory.
	 * @return The FCB found in directory associated with the specified name.
	 * @throws DirModException if element specified by name is not found in directory
	 * 
	 */
	public FCB removeEntry(String name, boolean dOrF)
	{
		FCB remEntry = getFCBByName(name, dOrF);
		
		if (remEntry == null)    //Throw Exception if target not found.
			throw new DirModException("Target \"" + name + " \" could not be found for removal.");
		
		return removeEntry(remEntry);
				
	}
	
	/**
	 * Get the number of entries in this directory.
	 * 
	 * @return int num entries in directory or 0 if empty
	 */
	public int numEntries()
	{
		return dirList.size();
	}
	
	/**
	 *  Calculates the size of this directory in bytes.
	 *
	 *  @return int The number of bytes that hold this directory.
	 **/
	public int getByteSize()
	{
		//Calculate size of directory in bytes as number of entries multiplied by constant size of FCB entries.
		return (dirList.size() * FCB.FCB_SIZE);
	}
	
	
	/**
	 * Creates and returns an array of bytes of the FCB's for the files and directories
	 * stored in this directory. Utilizes the getByteArr() method of tfs.structues.FCB for
	 * convenience. Useful for storing directory to disk.
	 * 
	 * @return byte[] An array of bytes holding the bytes of the FCB's stored in this directory.
	 *         
	 */
	public byte[] getByteArr()
	{
		if (dirList.isEmpty())
			return null;                //Return null pointer if directory is empty.
		
		//Number of entries multiplied by constant size of FCB's gives size of output array.
		byte [] outArr = new byte[getByteSize()];
		byte [] tmpArr;
		
		int outPos = 0;   //Current position in the output array.
		int inPos = 0;   //Current position in the array of the current FCB.
		
		//Now copy bytes of each entry into the output array of bytes.
		for (int i = 0; i < dirList.size(); i++) //Outer loop for the list, iterates through dirList.
		{
			tmpArr = dirList.get(i).getByteArr();          //Get byte array of the next entry in directory.
			
			//Null entries not added. Inner loop, loop for all the bytes of current entry.
			while (tmpArr != null && inPos < tmpArr.length)   
			{                   
				outArr[outPos] = tmpArr[inPos];                      //Copy byte to output array.
			
				outPos++;
				inPos++;
			}
			inPos = 0;    //Reset position in inner array to 0 for next entry.
		
		}
		
		return outArr;
	}
	
	/**
	 * Static method which creates a directory using the bytes provided 
	 * in byte array bDir.
	 * This directory will be size bytes in size.
	 * 
	 * @param bDir Byte[] containing byte of directory to create.
	 * @param size The size of directory in bytes.
	 * @return Directory of size bytes created using the bytes in byte[]
	 * @throws IllegalArgumentExceptions If directory creation fails due to illegal arguments.
	 */
	public static Directory bytesToDir(byte [] bDir, int size) throws IllegalArgumentException
	{
		if (size < 0 || size % FCB.FCB_SIZE != 0)        //Invalid size is < 0 or not divisible by size of an FCB.
			throw new IllegalArgumentException("Cannot create directory. Directory size must be >= 0 and directory must contain n FCB entries, "
					+ "\nmeaning size of directory must be divisible by size of FCB");
		
		Directory outDir = new Directory();            //The directory to return.
		
		//Iterate through and recreate each entry (FCB) in memory and add to the directory. Increment by FCB.FCB_SIZE each time.
		for (int i = 0; i < size; i += FCB.FCB_SIZE)  
		{
			//Temporary array hold bytes of the current entry. which is from i (inclusive) to i + FCB.FCB_SIZE (exclusive).
			byte[] tmpFCBBytes = Arrays.copyOfRange(bDir, i, i + FCB.FCB_SIZE);
			ByteBuffer tmpBB = ByteBuffer.wrap(tmpFCBBytes);   //Wrap the FCB bytes of this entry into a byte buffer.
			
			//Now get bytes from buffer for the various data members of the FCB.
			byte[] tmpName = new byte[15];    //The name stored by entry.
			tmpBB.get(tmpName, 0, 15);        //Copy first 15 bytes for name into byte array name.
			byte tmp_isDir = tmpBB.get();     //Get byte for is directory or not.              
			int tmpLocation = tmpBB.getInt(); //Get int for disk location (block num)               
			int tmpSize = tmpBB.getInt();     //Get int for Size (num bytes)
			
					
			//Add an FCB entry to directory with the above data as its stored data.
			outDir.addNewEntry(new FCB(tmpName, tmp_isDir, tmpLocation, tmpSize));
			
		}
			
		return outDir;
	}
	
	/**
	 * Similar to toString() method but info is output in a more accessible
	 * way. The string contains entry name for each entry, kind of entry (file or directory),
	 * and its size (for files) or number of entries (for directories). Number of items in this 
	 * directory is also listed.
	 * 
	 * @return String containing description of each entry
	 */
	public String listContents()
	{
		if (dirList.isEmpty())                //Indicate if dir is empty.
			return "Empty directory";
					
		StringBuffer sB = new StringBuffer(this.getByteSize() - 2*(dirList.size()));   //String buffer for contents of dir.
		sB.append("Directory with " + this.getByteSize() / FCB.FCB_SIZE +" entries contains:\n"); //Add number of entries to info string buffer.
		for (FCB f : dirList)
		{
			//Append this entry's type (file or dir), name, and size to sB.
			sB.append("\n\tType (File or directory): " + (f.is_Dir() ? "Directory" : "File") + "\n\tName: " + f.getStrName());
			//Add size of num entries if directory
			sB.append("\n\t" + (f.is_Dir() ? "Number of entries: " + f.getSize()/FCB.FCB_SIZE : "Size: " + f.getSize() + " Bytes") + "\n\n");   
		}
		
		return sB.toString();
		
	}
		
	/**
	 * Prints technical info for each file or directory held in this directory.
	 * 
	 * @return String representation of the directory.
	 */
	public String toString()
	{
		StringBuffer sB = new StringBuffer(this.getByteSize() - 3*(dirList.size()));    //New String buffer for contents of dir.
		for (FCB f : dirList)                                                           //Iterate over list backing directory.
			sB.append(f.toString() +"\n\n");                                            //Add current entry to String buffer.
		
		return sB.toString();			
	}
	
	
	
}


