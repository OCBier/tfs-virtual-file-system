package tfs.structures;

/**
 * PartitionControlBlock.java
 * 
 * Defines a PartitionControlBlock to be used in a file system which relies on a FAT.
 * 
 * Constructor creates a FAT using an instance of the FileAccessTable class, which is large
 * enough to manage all the logical blocks available in the system, given the number of blocks.
 * This value is given as an argument, as is the block size.
 * 
 *  
 * The class holds instance data which records the block size being used and the total number of
 * blocks = size of the FAT. Also, instance data for the root directory and the first
 * available block is held. The directory is an instance of the tfs.structures.Directory class.
 * 
 * Access and control over the FAT is provided by methods which call the appropriate methods
 * of FileAccessTable.
 * 
 * Methods to get the information stored in this PCB are also defined.
 *  
 * 
 * @author Oloff Biermann
 * @version 8.77
 * 
 **/

public class PartitionControlBlock 
{
	private int blockSize;
	private int numBlocks;			//This is the number of blocks in total.
									//Number of blocks is equal to the size of the FAT.
	private int firstFreeB;			//The first free block. This marks the end of the PCB.
	private int rootDir;		   //The root directory's location (block)
	
	private FileAccessTable FAT;
	
	
		
	/**
	 * Constructor initializes the FAT in memory so that it is empty. Now calculate
	 * the first free block, given the block size and the bytes the PCB must take up.
	 * Record this information in the FAT. Also record the first free block.
	 * This first free block is marked as the location as the root directory.
	 * This can be changed by calling setRootDirLoc.
	 * 
	 * Now all data in the PCB is ready to be written to the emulated disk file.
	 * 
	 * @param blockSize int Size of emulated disk blocks in bytes.
	 * @param numBlocks int Number of blocks in the emulated disk.
	 */
	public PartitionControlBlock(int blockSize, int numBlocks)
	{
		this.numBlocks = numBlocks;
		FAT = new FileAccessTable(numBlocks);	//Initialize the FAT.
		
		this.blockSize = blockSize;
		this.numBlocks = numBlocks;
		
		//Calculate number of blocks used to to store PCB and FAT. First 4 items in PCB
		//are 4 ints = 16 bytes. FAT entries are also int, so length of FAT can be
		//multiplied by 4 to get total bytes. Then divide by block size.
		int pCBSize = 16 + (4 * numBlocks);
		int usedBlocks = pCBSize / blockSize;
		firstFreeB = rootDir = usedBlocks + 1;		//Set first free block, and default rootDir.
		
				
	}
	
	/**
	 * Changes the location of the root directory.
	 * 
	 * @param root The block where root directory starts.
	 * @return 0 if success, -1 if error.
	 */
	public int setRootDir(int root)
	{
		if (root < 0 || root > numBlocks)
			return -1;
		
		rootDir = root;
		return 0;
		
	}
	
	/**
	 * Changes the firstFreeBlock
	 */
	public int setFirstFreeBlock(int b)
	{
		if (b < 0 || b > numBlocks)
			return -1;
		
		firstFreeB = b;
		return 0;
		
	}
	
		
	
	/**
	 * Get the int array which is the current FAT.
	 * 
	 * @return int[] The FAT.
	 */
	public int[] getFAT()
	{
		return FAT.getTable();
	}
	
	/**
	 * Sets the FAT for a modified FAT, if needed.
	 * 
	 */
	public void setFAT(int [] repFAT)
	{
		FAT.setTable(repFAT);
	}
	
	/**
	 * Get the first available free block.
	 * 
	 * @return int First free block.
	 */
	public int getFirstFreeBlock()
	{
		return firstFreeB;
	}
	
	/**
	 * Get the current location (block) of the root directory.
	 * 
	 * @return int The root directory location which is a block.
	 */
	public int getRootDir()
	{
		return rootDir;
	}
	
	/**
	 * 
	 * @return int Total number of blocks managed by PCB.
	 */
	public int getNumBlocks()
	{
		return numBlocks;
	}
	
	/**
	 * Returns the block size for this partition.
	 * 
	 * @return int Block size used by partition
	 */
	public int getBlockSize()
	{
		return blockSize;
	}
	
	
	/**
	 * Change the value of an index in the FAT so that blocks can be marked to
	 * show the next block for that file or mark them as empty or EOF.
	 * Method ensures that it is not possible to modify entries in the FAT which
	 * represent blocks in which the PCB is stored, IF these values have already been set.
	 * 
	 * @param index The index of the entry to update.
	 * @param entryValue The block number to which this entry's block
	 * should be linked.
	 * @return 0 if success, -1 if error.
	 */
	
	public int updateFAT(int index, int entryValue)
	{
		//Index must not be < 0.
		if (index < 0)
		{	
			System.err.println("Index of entry to modify in FAT must not be < 0\n");
			return -1;
		}
		 //Cannot modify entries for blocks storing this PCB, if it has already been stored.
		if (index < rootDir && ((FAT.getEntry(index)) != 0))  //!= 0 indicates entry has been stored.
		{
			return -1;
		}		
		
		else if (entryValue > numBlocks)
		{
			System.err.println("Cannot modify entry. Index is beyond size of partition.\n");
			return -1;
		}
			
		
		FAT.setEntry(index, entryValue);
		
		return 0;
		
	}
	
	/**
	 * Prints this PCB, including the FAT.
	 * 
	 * @return String representing the entire PCB.
	 * 
	 */
	public String toString()
	{
		String result = "**Partition Control Block (PCB)**\n";
		result += "\nBlock Size: " +blockSize + "\nNumber of Blocks: " + numBlocks;
		result+= "\nFirst Free Block after PCB: Block " +firstFreeB;
		result +="\nBlock (location) of root directory: Block "+rootDir;
		result +="\n"+ FAT.toString();
		
		return result;
		
	}
}
