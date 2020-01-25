package tfs;

/**
 * TFSFileSystem.java
 * 
 *
 * This class provides an API for the TFS file system which uses a FAT allocation method. It 
 * contains methods for creating, mounting, unmounting, exiting, and opening a TFS file system. 
 * For the directory structure, this API has methods for creating, viewing, and removing 
 * directories. It also has methods for opening and closing files, as well as reading from 
 * and writing to files.
 * 
 * The class manages many aspects of this file system. First, the tfs_mkfs method creates 
 * the PCB (Partition Control Block). This partition control block is represented by an instance 
 * of the class PartitionControlBlock which is defined in tfs.structures. PartitionControlBlock 
 * class holds the FileAccessTable, which stores an array that is used to maintain the FAT.
 * See the comments in tfs.structures.FileAccessTable and the comments in
 * tfs.structures.PartitionControlBlock for more details.
 * 
 * 
 * The functioning of TFSFileSystem is also supported by TFSDiskInputOutput, to create, access 
 * and manipulate blocks in the file which emulates the file system's storage space on a disk.
 * 
 * Directories in the file system are implemented by the tfs.structures.Directory class.
 * This class manages the locations of files or directories using a doubly linked list.
 * This list stores instances of the tfs.structures.FCB class, which represents
 * directory entries. These entries hold all the attributes needed for a directory entry.
 * The Directory class gives convenient access to any and the ability to update any entries
 * in memory. It also provides a static method for creating an instance of Directory given
 * an array of the bytes, Directory.bytesToDir(). The main use for this method is in loading 
 * directories from disk. It is called in tfs_load_dir().
 * tfs.structures.Directory also provides a method for creating an array of bytes of all 
 * entries in a directory, getByteArr(). This is useful for writing a directory from memory to disk, 
 * and is called in tfs_store_dir. 
 * See the comments in Directory class and FCB class for more info on how the class stores 
 * each directory entry.
 * 
 * In memory, TFSFileSystem keeps track of open files by using a file descriptor table.
 * The file descriptor table is managed as an object of the tfs.structures.FDT class. This class
 * keeps track of file name, location (starting block number), and size, as well as current 
 * position (offset) for every open file. Every time an entry is added(file opened),
 * a File Descriptor is returned for that file. This allows the entry to be updated
 * if file is moved or manipulated in other ways, or removed, when file is closed.
 * So this FDT essentially combines the functionality provided by an 
 * inode table, a system-wide open file table, and a traditional file descriptor table.
 * This class is implemented by using two arrays. 
 * The first array contains pointers to the FCB of each open file while the second
 * array contains corresponding offset data. The tfs.structures.FDT class also provides several
 * methods for adding and removing entries to the FDT, as well as a method for finding entries
 * and methods to check if the table is full or empty.
 * For debugging, a toString() method is also provided.
 * See documentation included in tfs.structures.FDT for more information on the implementation
 * of this class.
 * 
 * 
 * 
 * 
 * @author Oloff Biermann
 * @version 8.77
 * 
 */


import tfs.structures.*;
import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;


public class TFSFileSystem 
{
	public final static String ENCODING = "UTF-8";      //UTF-8 encoding will be used for chars written to disk.
	private final static String ROOT_NAME = "ROOT";    //Reserved name for root directory.
	
	
	private PartitionControlBlock PCB;        //The process control block for this file system.
	private FDT fd_Table;                    //The file descriptor table for this system.
	Directory rD;							//The root directory stored in memory.
	
	
	private boolean isMounted;            //Keeps track of whether or not file system is mounted.
	
	
	
	/**
	 * This method creates an entirely new TFS file system. The tfs_dio_create() method
	 * is used to create the emulated disk file, given parameters for name, length, and
	 * block size.
	 * 
	 * Any existing file system will be destroyed, as TFSDiskInputOutput.tfs_create()
	 * destroys the virtual disk (file) and creates a new one in its place.
	 * 
	 * ***IMPORTANT*** 
	 * (see documentation on tfs_sync() method for more details)
	 *  The order in which the PCB data is written to disk is:
	 * 
	 * blockSize
	 * numBlocks
	 * firstFreeBlock
	 * rootDir
	 * FAT
	 * 
	 * The empty root directory is created in memory as an instance of tfs.structures.Directory.
	 * The FAT is updated accordingly and then written to disk via tfs_sync() method.
	 * 
	 * @return 0 if success, -1 if an error occurred.
	 */
	public int tfs_mkfs(String pName, int length, int blockSize) throws UnsupportedEncodingException
	{
				
		//Set mount flag to false.
		isMounted = false;
		int result = 0;
		
		byte [] tempName = pName.getBytes(ENCODING);
		result = TFSDiskInputOutput.tfs_dio_create(tempName, length, blockSize);	  //Create the emulated disk, destroying an existing disk file.
		result = TFSDiskInputOutput.tfs_dio_open(tempName, length, blockSize);        //Open the disk (file session).
		
		if (result != 0)
		{
			return result;    //Return error code if  virtual disk could not be created.
		}
			
		PCB = new PartitionControlBlock(blockSize, TFSDiskInputOutput.tfs_dio_getSize());	//Initialize the PCB (which initializes FAT as well) with block size and numBlocks.
		int firstFreeBlock, rootDir = 0;
		firstFreeBlock = rootDir = PCB.getFirstFreeBlock();  //Get the block number for first free block.
		
		//Modify the FAT to represent the fact that PCB will now be written to disk.
		for(int i = 0; i < firstFreeBlock; i++)
		{
			if (i != firstFreeBlock - 1)
				result = PCB.updateFAT(i, (i + 1));		//Mark blocks represented in FAT so that each block is "linked" to the next block.
			else 
				result = PCB.updateFAT(i, -1);			//Else mark end of PCB with EOF.
			
			if (result == -1)
			{
				return result;
				
			}
		}
		
		//Create empty root directory in memory as an instance of the tfs.structures.Directory class.
		rD = new Directory();
		
		/*
		 * Add the FCB representing root directory to the beginning of the root directory 
		 * Other directories will not do this. The FCB's that manage them
		 * will be stored in their parent directory. But obviously the root directory
		 * doesn't have a parent directory, so the FCB that represents it is simply stored at
		 * its beginning and will be loaded from this point, when needed.
		 */
		
		rD.addNewEntry(new FCB(ROOT_NAME, true, rootDir, FCB.FCB_SIZE));
		
		//Write root dir to disk at rootDir.
		tfs_store_dir(rD, rootDir);
		
		//Update FAT to represent the fact that root dir is now in file system. Also update next free block.
		PCB.updateFAT(rootDir, -1);
		PCB.setFirstFreeBlock(PCB.getFirstFreeBlock() + 1);  
				
		/*Now write the PCB data into the emulated disk in the order specified above 
		 constructor header in comments. FAT is written last. This is done using tfs_sync(). */
		return tfs_sync();
			
	}
	
	/**
	 * Mounts the file system by reading the PCB and FAT from disk to memory. This replaces
	 * the current PCB in memory, if it is loaded anyway. The root directory is then loaded
	 * in to memory. Then flag isMounted is set.
	 * 
	 * A File Descriptor Table is also created, as an object of tfs.structures.FDT class.
	 * This is used to manage open files in the file system, especially to keep track of the
	 * current file pointer (current position within) each file.
	 * 
	 * Uses private method readPCB to read FAT, PCB into memory.
	 * 
	 *@return int 0 if success, -1 if error occurred.
	 */
	public int tfs_mount(String pName, int length, int blockSize) throws UnsupportedEncodingException
	{
		//Make sure file system is open.
		if ( (TFSDiskInputOutput.tfs_dio_open_existing(pName.getBytes(ENCODING), blockSize)) < 0)
				return -1;
		

		try{
			this.PCB = tfs_readPCB();      //Read PCB into memory from file system.
					
			}catch (Exception e)
			{
				return -1;
			}
		
	   /**
		 * Size of root directory cannot be known, because its FCB is its first entry.
		 * So first load this directory as a directory with only this one entry.
		 */
		rD = tfs_load_dir(PCB.getRootDir(), FCB.FCB_SIZE);   
		//Then use the root FCB to load entire root directory since size is now known from the root dir FCB.
		rD = tfs_load_dir(PCB.getRootDir(), ((rD.getFCBByName(ROOT_NAME, true)).getSize()));
						
		//Initialize the (initially empty) FDT in memory with an arbitrary capacity of file system capacity/blocksize
		fd_Table = new FDT(length/PCB.getBlockSize());
		
		//Set isMounted to true.
		isMounted = true;
		
		return 0;
	}
	
	
	/**
	 * Method stores the current PCB and FAT in memory to disk so they are synced to record
	 * changes made to file system during the current session.
	 * 
	 * As noted above, the write order is:
	 * blockSize
	 * numBlocks
	 * firstFreeBlock
	 * rootDir
	 * FAT
	 * 
	 * @return int 0 if sync completed successfully, -1 if error.
	 */
	public int tfs_sync()
	{		
				if (PCB == null)     //If partition control block is not loaded, return -1.
					return -1;
				
				//PCB data to be written to emulated disk.
				int blockSize = TFSDiskInputOutput.getBlockSize();
				int numBlocks = PCB.getNumBlocks();
				int firstFreeB = PCB.getFirstFreeBlock();
				int rootDir = PCB.getRootDir();
				
				int [] curFAT = PCB.getFAT();
				
					
				//Now convert and store the first 4 PCB entries into an array of bytes. Size needed is calculated as 16 bytes + number of blocks (entries in FAT) * 4
				//since int is stored as 4 bytes.
				
								
				byte [] PCB_Array = new byte[(16 + (4 * curFAT.length))];			//Array of all the bytes in the PCB.
				byte [][] tempBytes = new byte[4 + curFAT.length][4];               //Temporary 2D array containing 4 bytes in each row.
				
				tempBytes[0] = intGetBytes(blockSize);
				tempBytes[1] = intGetBytes(numBlocks);
				tempBytes[2] = intGetBytes(firstFreeB);
				tempBytes[3] = intGetBytes(rootDir);
				
				//Now convert and copy all the FAT entries using the same method. Start at beginning of FAT and iterate.
				int curRow = 4;
				int FATEntry = 0;
				
				while (curRow < tempBytes.length)
				{
					tempBytes[curRow] = intGetBytes(curFAT[FATEntry]);	 //Convert the current entry in the FAT to an array of bytes.					
					FATEntry++;
					curRow++;
				}
				
				int curPos = 0;
				
				//Now copy everything from tempBytes to the large linear array of bytes, PCB_Array.
				for (int row = 0; row < tempBytes.length; row++)
				{
					for (int col = 0; col < tempBytes[0].length; col++)
					{
						PCB_Array[curPos] = tempBytes[row][col];						
						curPos++;
					}
					
				}
				
				//Finally, write all the blocks into emulated disk file as buffers of blockSize arrays.
				//Uses Arrays.copy of method to copy. Important that this method's upper range is NOT inclusive.
				byte [] tempBuf = new byte[blockSize];
				
				int j = 0;
				int blockNum = 0;
				
						
				while (j < PCB_Array.length - blockSize)
				{
					tempBuf = Arrays.copyOfRange(PCB_Array, j, (j + blockSize));		//Copy next part of array to the tempBuf.
					TFSDiskInputOutput.tfs_dio_writeBlock(blockNum, tempBuf);			//Write the buffer to a block.
					
					j += blockSize;			//Increment j by blockSize.
					blockNum++;				//Increment blockNum to write to next.
				}
				
							
				return 0;
			
		}
	
	/**
	 * Reads the PCB and FAT from disk into a large array of bytes.
	 * 
	 * It is assumed that the order in which PCB entries are stored is still:
	 * blockSize
	 * numBlocks
	 * firstFreeBlock
	 * rootDir
     * FAT
	 * 
	 * @return A PCB object which hold the values read from the PCB and FAT stored in disk.
	 */
	
	private PartitionControlBlock tfs_readPCB()
	{ 
		int blockSize = TFSDiskInputOutput.getBlockSize();
		int numBlocks = TFSDiskInputOutput.tfs_dio_getSize();
		
		//First create the temp PCB with the data directly from emulated disk.
		PartitionControlBlock tempPCB = new PartitionControlBlock(blockSize, numBlocks);
		
		//Now copy all the PCB blocks from disk and convert them to a 2D array of int.
		//Blocks used to store PCB must be (16 bytes + numBlocks * 4) bytes / blockSize
		
		int numPCBBlocks = (16 + numBlocks * 4) / blockSize;
				
		int[][] PCBArray = new int [numPCBBlocks][(blockSize / 4)];
		byte [] b = new byte[blockSize];
		
		for (int row = 0; row < PCBArray.length; row++)
		{
			TFSDiskInputOutput.tfs_dio_readBlock(row, b);		//Read blocks into b
			PCBArray[row] = arrBytesGetInt(b);					//Copy the int array PCBArray
		}
		
		int[] tempFAT = new int[numBlocks];					//Array for the read FAT.
		int cur = 0;
		
		//Finally, update the temp PCB with the data.
		for (int row = 0; row < PCBArray.length; row++)
		{
			for (int col = 0; col < PCBArray[0].length; col++)
			{
				if ((row == 0 && col == 0) || row == 0 && col == 1)	//Ignore first two ints.
				{}
				
				else if (row == 0 && col == 2)						//3rd int, setFirstFreeBlock
					tempPCB.setFirstFreeBlock(PCBArray[0][2]);
				
				else if (row == 0 && col == 3)						//4th int, set rootDir
					tempPCB.setRootDir(PCBArray[0][3]);
				
				else 
				{
					tempFAT[cur] = PCBArray[row][col];				//Else add data to tempFAT.
					cur++;
				}
			}
		}
		
		
		tempPCB.setFAT(tempFAT);									//Set tempPCB's FAT.
		
		return tempPCB;
		
		
	}
	
	
	
	/**
	 * Unmounts the file system by storing both PCB and FAT in memory to disk using
	 * the sync() method. Also updates root directory's FCB and stores the root dir to disk.
	 *  Then set PCB and FDT to null.
	 * 
	 * @return int 0 is success, -1 if error.
	 */
	public int tfs_unmount()
	{	
		if (!(isMounted))                                         //Do nothing if it is not mounted.
			return -1;
		
		int success = tfs_sync();                                 //Attempt sync.
		rD.updateEntrySize(ROOT_NAME, rD.getByteSize(), true);   //Update FCB held in rD index 0.
		tfs_store_dir(rD, PCB.getRootDir());                      //Store it root dir to disk.
		
						
		 //If sync was successful, set root directory, PCB and FDT to null in memory so they can be removed by garbage collection. Then return 0.
		if (success == 0)                    
		{
			isMounted = false;     //Set mount flag to false.
			PCB = null;
			fd_Table = null;
			rD = null;
			return 0;
		}
		
				
		else              //Otherwise, if sync was not successful, return -1.
			return -1;
				
		
	}
	
	
	
	/**
	 * Similar to the tfs_prmfs method, except that this method returns the
	 * PCB and FAT as it is ON DISK at the current time. So it is read directly from 
	 * disk, in the file system. 
	 * 
	 * Reading from emulated disk is provided by service method readPCB().
	 * 
	 * @return String representation of the PCB and FAT ON DISK.
	 */
	
	public String tfs_prffs() throws UnsupportedEncodingException
	{
		PartitionControlBlock diskPCB = tfs_readPCB();
					
		return diskPCB.toString();
	}
	
	
	/**
	 * Calls toString method of PartitionControlBlock object to get a string
	 * representation of the PCB and the FAT IN MAIN MEMORY. If not mounted, 
	 * this is indicated instead.
	 * 
	 * @return String representation of the PCB and the FAT IN MEMORY.
	 */
	public String tfs_prmfs()
	{
		if (!(isMounted))
			return "TFS file system not mounted";
		
		return PCB.toString();
		
	}
	
	/**
	 * Creates an empty directory down the absolute path, if it does not already exist.
	 * Element after last "/" delimeter specifies new directory name. 
	 * Updated parent directory is written to disk. File system metadata is updated 
	 * appropriately by service methods. 
	 * 
	 * Path is searched before creating new directory. 
	 * If any directory(except the one to create) in the specified path does not exist, 
	 * operation will fail. 
	 * If parent directory already contains the specified directory, no new directory
	 * will be created.
	 * 
	 * @param path String containing the absolute path from the root directory with
	 * the element after the final forward slash being the directory to create. Does
	 * not include name of root directory.
	 * Eg. "/folder_biz/folder_baz/new_folder_name"
	 * @return 0 if success, or -1 if directory could not be created.
	 * 
	 */
	public int tfs_mkdir(String path)
	{
		if (!isMounted)                                  //Make sure file system is mounted.
			return -1;
			
		String[] dirs = getValidPath(path);
		if (dirs == null)
			return -1;
		
		int pathLength = dirs.length + 1;               //Length of path, given by size of dirs + 1 for root dir.
			
		if(pathLength == 2)                               //If target should be created in root directory (length of path is only 2)
		{
				if (rD.contains(dirs[0], true))          //If RD already contains the target., return -1.
					return -1;
					
				int loc = PCB.getFirstFreeBlock();
				Directory tmp = new Directory();
				tfs_store_dir(tmp, loc);                             //Store the new Directory to disk at first free block.
				rD.addNewEntry(new FCB(dirs[0], true, loc, 0));                 //Add new entry directly in root directory.
				rD.updateEntrySize(ROOT_NAME, rD.getByteSize(), true);          //Update root directory's stored FCB for itself.
				return (tfs_store_dir(rD, PCB.getRootDir()) >= 0 ? 0 : -1);     //Store root dir to disk
		}
			
						
		/* 
		 *  If path is longer, search path to make sure it contains the directories before the one to be created. 
		 *  Search entire path, starting from root. When search is finished, if path is good, parent pointer will
		 *  point to parent of target. Ancestor pointer will point to parent of this parent.
		 */
			                                          
		int dirsChecked = 0;                                                  //Number of dirs checked so far, excluding root..
		Directory parent = rD;                                                //Set parent to root dir. 
		Directory ancestor = null;
		FCB nextParentFCB = null;
		FCB ancestorFCB = null;
				
		while (dirsChecked < pathLength - 2)                                  //Only go as far as parent of target                                          
		{
			if ( !(parent.contains(dirs[dirsChecked], true)) )               //Look for next directory in current directory
			{
				System.err.println("Directory " +dirs[dirsChecked] + " not in path\n");
				return -1;                                                    //Return -1 if it is not found.
			}
			
			if (pathLength > 3)               //Store pointer to current parent as ancestor, unless path is too short.
			{
					ancestor = parent;  
					ancestorFCB = nextParentFCB;    //This FCB represents current ancestor ---> parent, whose FCB is nextParentFCB.
			}
			
			//Get FCB of the next directory down path from current parent.		         
			nextParentFCB = parent.getFCBByName(dirs[dirsChecked], true);
			
			//Load next parent from disk in its current state.
			parent = tfs_load_dir(nextParentFCB.getLocation(), nextParentFCB.getSize());  
					
			dirsChecked++;                                                 //Increment number of directories checked.  
		}  //End- while
			
	      /*
	       * If above loop completes without returning -1, check parent to make sure it 
	       * DOESN't already contain target. Target is at dirsChecked.
	       */
		    if (parent.contains(dirs[dirsChecked], true))
				return -1;
			
		   //Now make the new blank directory, since all conditions are met. Store it at first free block.
		   int location = PCB.getFirstFreeBlock();
		   tfs_store_dir(new Directory(), location);
		   
		   /*
		    *   Update parent and ancestor (paren't parent) and write them to disk.
			*   This is necessary because a new entry has been added to parent, meaning its contents
			*   has changed. The size value in ancestor's entry for this parent needs to be updated to 
			*   reflect this change.
			*/
			
		   parent.addNewEntry(new FCB(dirs[dirsChecked], true, location, 0));           //Add entry in parent for the new empty. dir.
		   if ( (tfs_store_dir(parent, nextParentFCB.getLocation())) < 0)               //Store parent.
				return -1;
	 
		   if (pathLength > 3)                                                         //Update ancestor, if path length > 3.
		   {
				ancestor.updateEntrySize(dirs[dirsChecked - 1], parent.getByteSize(), true);       //Update ancestor's entry.
				return ( (tfs_store_dir(ancestor, ancestorFCB.getLocation())) >= 0 ? 0 : -1);					   //Store ancestor to disk
					
		   }
				
		   else if(pathLength == 3)                //Special case. Update root dir if pathLength is 3, write it to disk at rootDir.
			{
				rD.updateEntrySize(dirs[0], parent.getByteSize(), true);
				return ( (tfs_store_dir(rD, PCB.getRootDir())) >= 0 ? 0 : -1);
			}
						
			return 0;
		}
		
		
	/**
	 *  Removes a directory at the given location down path absolute path
	 *  from root. Root name is NOT included in path. Directory is only
	 *  removed if it is empty.
	 *  
	 *  @param path The path from root to the target, including target name.
	 *  @return int 0 if success, -1 if path is invalid or other error occurs,
	 *   -2 if directory is not empty. 
	 * 
	 */
	public int tfs_rmdir(String path)
	{
		if (!isMounted)                              //Check if file system is mounted.
			return -1;
		
		String[] dirs = getValidPath(path);         //Get valid path to target.
		if (dirs == null)
			return -1;
		
		if (dirs[0] == ROOT_NAME)                   //Important. Prevents root metadata from being deleted.
			return -1;
				
		int pathLength = dirs.length + 1;            //Length of path, given by size of dirs + 1 for root dir.
		
		if (pathLength == 2)                          //If target directory is in root directory.
		{
			if (!(rD.contains(dirs[0], true)))         //If target NOT in rD, return -1.
				return -1;
			
			FCB targetFCB = rD.getFCBByName(dirs[0], true);
			if ((targetFCB.getSize()) != 0 )           //If target in rD is not empty, return -2.
				return -2;
			
			rD.removeEntry(targetFCB);                               //Otherwise remove target from rD
			rD.updateEntrySize(ROOT_NAME, rD.getByteSize(), true);   //Update RD's entry about itself.
			tfs_clear_blocks(targetFCB.getLocation());               //Update FAT for these blocks.
			return ( (tfs_store_dir(rD, PCB.getRootDir())) >= 0 ? 0 : -1);        //Store root dir to disk and return 0 if success, of -1 if fail.
					
		}
		
		//If path length is longer, search still needs to start at rD but continues to more sub directories.
		
		int dirsChecked = 0;                                 //Number of dirs checked so far, excluding root..
		Directory parent = rD;                              //Set the first parent to root directory. 
		Directory ancestor = null;
		FCB nextParentFCB = null;
		FCB ancestorFCB = null;
		
			
		while (dirsChecked < pathLength - 2)                                  //Only go as far as parent of target                                          
		{
			if ( !(parent.contains(dirs[dirsChecked], true)) )               //Look for next directory in current directory
			{
				System.err.println("Directory " +dirs[dirsChecked] + " not in path\n");
				return -1;                                                    //Return -1 if it is not found.
			}
			
			if (pathLength > 3)                                             //Store pointer to current parent as ancestor.
			{
					ancestor = parent;  
					ancestorFCB = nextParentFCB;
			}
	                 
			nextParentFCB = parent.getFCBByName(dirs[dirsChecked], true);		          //Get FCB of the next directory down path from current parent.		
			parent = tfs_load_dir(nextParentFCB.getLocation(), nextParentFCB.getSize());  //Load next parent from disk.
					
			dirsChecked++;                                                 //Increment number of directories checked.  
		}  //End- while
		
		
		  /*
	       * If above loop completes without returning -1, check parent to make sure it 
	       * contains the target and that the target is empty. Target is at dirsChecked.
	       */
		   		    
		    FCB targetFCB = parent.getFCBByName(dirs[dirsChecked], true);      //Get FCB of target.
		    if (targetFCB == null)                                             //If null pointer is returned, target not found.
		    	return -1;
		    
		    if (targetFCB.getSize() != 0)
		    	return -2;                                                    //If target not empty, return -1
		    
		    tfs_clear_blocks(targetFCB.getLocation());                        //Clear FAT entry for this dir.
		    
		    parent.removeEntry(targetFCB);                                    //If it is empty, remove target from parent.
		    
		    tfs_store_dir(parent, nextParentFCB.getLocation());           //Store updated parent back to disk.
		
		    if (pathLength > 3)                                    //If path length is > 3, update ancestor's entry
		    {
		    	ancestor.updateEntrySize(dirs[dirsChecked - 1], parent.getByteSize(), true);       //Update ancestor's entry.
				return ((tfs_store_dir(ancestor, ancestorFCB.getLocation())) >= 0 ? 0 : -1);	  //Store ancestor to disk
		    }
		    
		    else if(pathLength == 3)                //Special case. Update root dir if pathLength is 3, write it to disk at rootDir.
			{
				rD.updateEntrySize(dirs[0], parent.getByteSize(), true);
				return ( (tfs_store_dir(rD, PCB.getRootDir())) >= 0 ? 0 : -1);
			}
		    
		    return 0;   //Not really necessary.
	}
	
	/**
	 * Creates a String buffer which describes the contents of the target directory specified
	 * down the path. If target cannot be found, this is indicated in String buffer.
	 * If target can be found, but it is a file, this is also indicated.
	 * 
	 * Special case: If only "/" is given as path, root directory is printed.
	 * 
	 * @param path The absolute path from root to target.
	 * @return StringBuffer which contains description listing contents of target,
	 *  or that it cannot be found.
	 */
	public StringBuffer tfs_ls(String path)
	{
		StringBuffer descBuf = new StringBuffer(FCB.FCB_SIZE);
		if (!isMounted)                                          //Check if file system is mounted.
		{
			descBuf.append("TFS file system not mounted.\n");
			return descBuf;
		}
		
		//Special case if "/" is given as path, print root dir.
		if (path.equals("/"))
		{
			descBuf.append(rD.listContents());
			return descBuf;
		}
				
		String tmpPath = path.split("/", 2)[1];                  //Get rid of first '/'
		String[] dirs = tmpPath.split("/");                      //Split the path string by / symbol
		
		
		if (dirs.length == 0)       //If no path is given
		{
			descBuf.append("Empty path given.\n");
			return descBuf;
		}
		
		int pathLength = dirs.length + 1;        //Length of path is number of elements + the rootdir.
		Directory curDir = rD;                   //Start at root.
		FCB curD_FCB = null;
		
		for (int i = 0; i < pathLength - 2; i++) //Go down path an check that it is valid, up to parent directory of target.
		{
			curD_FCB = curDir.getFCBByName(dirs[i], true);  //Note method returns null if FCB is not found.
			
			if(curD_FCB == null)                           //If current directory doesn't contain next dir in path, path is invalid.
			{
				descBuf.append("Invalid path.\n");
				return descBuf;                             //Return the buffer with message "invalid path."
			}			
			curDir = tfs_load_dir(curD_FCB.getLocation(), curD_FCB.getSize());      //Load next directory from disk, if previous one contains it.
		}
		
		//Now look in the parent of target.
		curD_FCB = curDir.getFCBByName(dirs[dirs.length - 1], true);
		
		if (curD_FCB == null)                        //If parent contains target, load it from disk and print its contents.
		{
			descBuf.append("Target directory \"" + dirs[dirs.length-1] + "\" not found");
			return descBuf;                         //Return the StringBuffer with above message.
		}
					
		else                                         //Otherwise parent contains target. load it from disk.
		{
			curDir = tfs_load_dir(curD_FCB.getLocation(), curD_FCB.getSize());
			descBuf.append(curDir.listContents());   //Add contents of this directory to the string buffer.
			return descBuf;                          //Return the StringBuffer.
			
		}
	}
	
	/**
	 * Overloaded version of tfs_create.
	 * Creates an empty file (size = 0 bytes).
	 * 
	 * @param path The path from root to where file should be created. Last
	 * element in path is new file name.
	 * @return int write start location (block) if success, 
	 * -1 for general failure, or -2 if  file already exists in parent directory.
	 */
	public int tfs_create(String path)
	{
		return ( tfs_create(path, 0) );          //Call tfs_create with the block size as size.
	}
	
	/**
	 * Creates a new file at a given location. Element after last "/" 
	 * delimeter specifies name of file.
	 * 
	 * @param path The absolute path to where file should be created.
	 * @param size The initial size of the empty file to create.
	 * @return int write start location (block) if success, 
	 * -1 for general failure, or -2 if  file already exists in parent directory.
	 */
	public int tfs_create(String path, int size)
	{
		if (!isMounted)                              //Check if file system is mounted.
			return -1;
		
		int writeSize;                              //Actual number of bytes to write.
		int blSize = PCB.getBlockSize();
		if (size < blSize)
			writeSize = blSize;        //Set to one block, if < blockSize.
		
		else if (size % blSize == 0)  //If size is divisible by block size, use size as is.
			writeSize = size;
		
		//Else set to closest value divisible by block size by discarding fractional part and adding block size.
		else
		{
			 writeSize = ((size / blSize) * blSize ) + blSize;    
		}

		String[] dirs = getValidPath(path);           //Get valid path using service method.
		if (dirs == null)
			return -1;
		
		int pathLength = dirs.length + 1;            //Length of path, given by size of dirs + 1 for root dir.
		
		if (pathLength == 2)                          //If file will be created in root directory.
		{
			if ((rD.contains(dirs[0], false)))         //If rD already contains this file, return -2.
				return -2;
			
			//Otherwise write a block with size bytes to disk, getting location of first block.
			int writeLocation = tfs_write_blocks(new byte[writeSize]); 
			if (writeLocation < 0)                     //Check if write was successful.
				return -1;
			
			//Add new entry for empty file to root dir with location and name.
			rD.addNewEntry(new FCB(dirs[0], false, writeLocation, size));            
			rD.updateEntrySize(ROOT_NAME, rD.getByteSize(), true);   //Update RD's entry about itself.
			
			return ( (tfs_store_dir(rD, PCB.getRootDir())) >= 0 ? writeLocation : -1);        //Store root dir to disk and return 0 if success, of -1 if fail.
		}
		
		//If path length is longer, more dirs need to be searched before file is created.
		else
		{
			/* 
			 *  If path is longer, search path to make sure it contains the directories before the one to be created. 
			 *  Search entire path, starting from root. When search is finished, if path is good, parent pointer will
			 *  point to parent of target. Ancestor pointer will point to parent of this parent.
			 */
				                                          
			int dirsChecked = 0;                                                  //Number of dirs checked so far, excluding root..
			Directory parent = rD;                                                //Set parent to root dir. 
			Directory ancestor = null;
			FCB nextParentFCB = null;
			FCB ancestorFCB = null;
					
			while (dirsChecked < pathLength - 2)                                  //Only go as far as parent of target                                          
			{
				if ( !(parent.contains(dirs[dirsChecked], true)) )               //Look for next directory in current directory
				{
					System.err.println("Directory " +dirs[dirsChecked] + " not in path\n");
					return -1;                                                    //Return -1 if it is not found.
				}
				
				if (pathLength > 3)               //Store pointer to current parent as ancestor, unless path is too short.
				{
						ancestor = parent;  
						ancestorFCB = nextParentFCB;    //This FCB represents current ancestor ---> parent, whose FCB is nextParentFCB.
				}
				
				//Get FCB of the next directory down path from current parent.		         
				nextParentFCB = parent.getFCBByName(dirs[dirsChecked], true);
				
				//Load next parent from disk in its current state.
				parent = tfs_load_dir(nextParentFCB.getLocation(), nextParentFCB.getSize());  
						
				dirsChecked++;                                                 //Increment number of directories checked.  
			}  //End- while
			
			/*
		       * If above loop completes without returning -1, check parent to make sure it 
		       * DOESN't already contain target. Target is at dirsChecked.
		       */
			    if (parent.contains(dirs[dirsChecked], false))
					return -2;                                      //Return -2 if it does already contain it.
				
			   //Now simply write a buffer of size bytes, at default value for byte (0).
			 
			  int writeLocation = tfs_write_blocks(new byte[writeSize]); 
			  if (writeLocation < 0)                     //Check if write was successful.
					return -1;
			   			   
			   /*
			    *   Update parent and ancestor (paren't parent) and write them to disk.
				*   This is necessary because a new entry has been added to parent, meaning its contents
				*   has changed. The size value in ancestor's entry for this parent needs to be updated to 
				*   reflect this change.
				*/
				
			   parent.addNewEntry(new FCB(dirs[dirsChecked], false, writeLocation, size));     //Add entry in parent for the new file.
			   if ( (tfs_store_dir(parent, nextParentFCB.getLocation())) < 0)               //Store parent.
					return -1;
		 
			   if (pathLength > 3)                                                         //Update ancestor, if path length > 3.
			   {
					ancestor.updateEntrySize(dirs[dirsChecked - 1], parent.getByteSize(), true);       //Update ancestor's entry.
					return ( (tfs_store_dir(ancestor, ancestorFCB.getLocation())) >= 0 ? writeLocation : -1);	//Store ancestor to disk
						
			   }
					
			   else if(pathLength == 3)                //Special case. Update root dir if pathLength is 3, write it to disk at rootDir.
				{
					rD.updateEntrySize(dirs[0], parent.getByteSize(), true);
					return ( (tfs_store_dir(rD, PCB.getRootDir())) >= 0 ? writeLocation : -1);
				}
							
				return 0;
			
			
			
		}
		
	}
	
	/**
	 * Removes a file from a given directory, if it exists.
	 * 
	 * 
	 * @param path The path from root to the file to create. Last element path is the
	 * new file to create.
	 * @return 0 if success, -2 if file does not exist, or -1 if general error occurs.
	 */
	public int tfs_rm(String path)
	{
		if (!isMounted)
			return -1;
		
		/**
		 * Check validity of path and get a valid path.
		 */
		String[] dirs = getValidPath(path);
		if (dirs == null)
			return -1;
		
		int pathLength = dirs.length + 1;
		
		if (pathLength == 2)                             //If file will be removed from root directory.
		{
			if (!(rD.contains(dirs[0], false)))          //If rD does not contain file, return -2.
				return -2;
			
			//Otherwise, remove the FCB for this file from the directory.
			FCB remFCB = rD.removeEntry(dirs[0], false);
			tfs_clear_blocks(remFCB.getLocation());     //Reset FAT entries for current location and any linked blocks.
			
			if (!(fd_Table.isEmpty()) && fd_Table.isOpen(remFCB))  //If file is open, close it.
			{
				int tmpFD = fd_Table.getFD(remFCB);
				tfs_close_updateFDT(tmpFD);
			}
			
			rD.updateEntrySize(ROOT_NAME, rD.getByteSize(), true);   //Update RD's entry about itself.
			return ( (tfs_store_dir(rD, PCB.getRootDir())) >= 0 ? 0 : -1);        //Store root dir to disk and return 0 if success, of -1 if fail.
		}
		
		//If path length is longer, more dirs need to be searched before file can be removed.
		else
		{			
			int dirsChecked = 0;                      //Number of dirs checked so far, excluding root..
			Directory parent = rD;                    //Set the first parent to root directory. 
			Directory ancestor = null;
			FCB nextParentFCB = null;
			FCB ancestorFCB = null;
			
				
			while (dirsChecked < pathLength - 2)                                  //Only go as far as parent of target                                          
			{
				if ( !(parent.contains(dirs[dirsChecked], true)) )               //Look for next directory in current directory
				{
					System.err.println("Directory " +dirs[dirsChecked] + " not in path\n");
					return -1;                                                    //Return -1 if it is not found.
				}
				
				if (pathLength > 3)                                             //Store pointer to current parent as ancestor.
				{
						ancestor = parent;  
						ancestorFCB = nextParentFCB;
				}
		                 
				nextParentFCB = parent.getFCBByName(dirs[dirsChecked], true);		          //Get FCB of the next directory down path from current parent.		
				parent = tfs_load_dir(nextParentFCB.getLocation(), nextParentFCB.getSize());  //Load next parent from disk.
						
				dirsChecked++;                                                 //Increment number of directories checked.  
			}  //End- while
			
			
			  /*
		       * If above loop completes without returning -1, the parent directory needs to be checked
		       * to see if it actually contains the file. 
		       */
			   		    
			    FCB targetFCB = parent.getFCBByName(dirs[dirsChecked], false);      //Get FCB of target file.
			    if (targetFCB == null)                                             //If null pointer is returned, target not found.
			    	return -1;
			    
			    if (!(fd_Table.isEmpty()) && fd_Table.isOpen(targetFCB))        //If file is open, close it.
				{
					int tmpFD = fd_Table.getFD(targetFCB);
					tfs_close_updateFDT(tmpFD);
				}
			    			    
			    tfs_clear_blocks(targetFCB.getLocation());                        //Clear FAT entry or entries for this file.
			    parent.removeEntry(targetFCB);                                   //Remove the entry for the file from parent.
			    
			   tfs_store_dir(parent, nextParentFCB.getLocation());           //Store updated parent dir back to disk.
			
			    if (pathLength > 3)                                    //If path length is > 3, update ancestor's entry of parent.
			    {
			    	ancestor.updateEntrySize(dirs[dirsChecked - 1], parent.getByteSize(), true);       //Update ancestor's entry.
					return ((tfs_store_dir(ancestor, ancestorFCB.getLocation())) >= 0 ? 0 : -1);	  //Store ancestor to disk
			    }
			    
			    else if(pathLength == 3)            //Special case. Update root dir if pathLength is 3, write it to disk at rootDir.
				{
					rD.updateEntrySize(dirs[0], parent.getByteSize(), true);
					return ( (tfs_store_dir(rD, PCB.getRootDir())) >= 0 ? 0 : -1);
				}
			    
			    return 0;   //Not really necessary.
		}
	}
	
	
	/**
	 * Appends a String of characters to the end of this file. If file
	 * is not open, it is opened and an entry is added to the FDT.
	 * Bytes are then appended to the end of the file's bytes in
	 * memory, and file is written back to disk.
	 * 
	 * @param path The path from root to the target file.
	 * @param data The string of characters to append to the end of the file
	 * @return 0 if operation is successful, -1 if a general error occurs,
	 * or -2 if file is not found in the parent directory.
	 */
	public int tfs_append(String path, String data)
	{
		if (!isMounted)       //Check if TFS is mounted.
		return -1;
	
		String[] dirs = getValidPath(path);  //Get a valid path.
		if (dirs == null)                    //Make sure path was valid.
		{
			return -1;
		}
		
		if (data.length() == 0)        
			return -1;
		
		byte[] app;
		try {
		app = data.getBytes(ENCODING);  //If String is not empty, convert it to bytes.
		}catch (UnsupportedEncodingException e)
		{
			e.printStackTrace(System.err);
			return -1;
		}
		
		int pathLen = dirs.length + 1;        //Length of path, add 1 for root.
		int traversed = 0;                    //Number of directories traversed.
		Directory parent = this.rD;          //First parent is root.
		FCB nextParent_FCB = null;
		
		while(traversed < pathLen - 2)       //Look through path to make sure it can be traversed to target.
		{
			if ( !(parent.contains(dirs[traversed], true)) )     //Make sure parent contains next dir in path.
			{   
				System.err.println("Directory " +dirs[traversed] + " not in path\n");
				return -1;
			}
						
			nextParent_FCB = parent.getFCBByName(dirs[traversed], true);                    //Get FCB of next parent.
			parent = tfs_load_dir(nextParent_FCB.getLocation(), nextParent_FCB.getSize());  //Load next parent.
			traversed++;                                                                    //Increment num dirs traversed.
		}
		
		FCB targetFCB = parent.getFCBByName(dirs[traversed], false);  //Attempt to get FCB for file.
		if (targetFCB == null)
			return -2;                                                //Return -2 if parent doesn't contain the target.
		
		
		
		int fd = -1;
		
		if (!(fd_Table.isEmpty()))
			fd = fd_Table.getFD(targetFCB);             //Get FD for file, which also determines if file is open.
				
		if (fd < 0)                                     //If file is not open, open it.
		{
			fd = tfs_open_getFD(targetFCB);
			if (fd < 0)                                 //If file could not be opened, return -1.
				return -1;
		}
		
		tfs_seek_updateFDT(fd, targetFCB.getSize());                  //Seek to EOF.
		if ( (tfs_write_bytes_FDT(fd, app, app.length)) < 0)          //Begin write at this location.
		{
			System.err.println("Write failed.\n");
			return -1;
		}
		
		targetFCB.setSize(targetFCB.getSize() + app.length);  //Update size of file.
		fd_Table.updateFCB(fd, targetFCB);                    //Update FDT's entry for file.
		parent.update(targetFCB);                             //Update parent directory's entry.
		
		/*
		 * Special case for a file in root directory. rD and parent point to the same
		 * directory in memory in this situation.
		 */
		if (pathLen == 2)
			return ( (tfs_store_dir(parent, PCB.getRootDir())) >= 0 ? 0 : -1); 
		
		/*
		 * General case for file in directories further down the directory tree. 
		 */
		else
			return ( (tfs_store_dir(parent, nextParent_FCB.getLocation())) >= 0 ? 0 : -1);    
		
		                                                         
		
		
	}
	
	
	/**
	 * Method prints number characters from the 
	 * 
	 * 
	 * @param path Path from the root to the target file.
	 * @param position File offset from which to begin read.
	 * @param number The number of characters to read.
	 * @return StringBuffer with the characters that were read from file or appropriate
	 * error message if an error occurs.
	 */
	public StringBuffer print(String path, int position, int number)
	{
		StringBuffer sB = new StringBuffer(24);
		if (!isMounted)       //Check if TFS is mounted.
		{
			sB.append("Error. TFS file system not mounted\n");
			return sB;
		}
		
		String[] dirs = getValidPath(path);  //Get a valid path.
	
		if (dirs == null)                    //Make sure path was valid.
		{
			sB.append("Invalid path.\n");
			return sB;
		}
		
		int pathLen = dirs.length + 1;        //Length of path, add 1 for root.
		int traversed = 0;                    //Number of directories traversed.
		Directory parent = this.rD;          //First parent is root.
		FCB nextParent_FCB = null;
		
		while(traversed < pathLen - 2)       //Look through path to make sure it can be traversed to target.
		{
			if ( !(parent.contains(dirs[traversed], true)) )     //Make sure parent contains next dir in path.
			{   
				sB.append("Directory " +dirs[traversed] + " not in path\n");
				return sB;
			}
						
			nextParent_FCB = parent.getFCBByName(dirs[traversed], true);                    //Get FCB of next parent.
			parent = tfs_load_dir(nextParent_FCB.getLocation(), nextParent_FCB.getSize());  //Load next parent.
			traversed++;                                                                    //Increment num dirs traversed.
		}
		
		FCB targetFCB = parent.getFCBByName(dirs[traversed], false);  //Attempt to get FCB for file.
		if (targetFCB == null)
		{
			//Add err message to sB, return sB.
			sB.append("Target file not in directory "); 
			return sB;
		}
		
		//Check if offset and number are valid.
		if (position < 0 || position > targetFCB.getSize())
		{
			sB.append("Invalid file position.\n");
			return sB;
		}
		
		if (number < 0 || ((targetFCB.getSize() - position) - number ) < 0 )
		{
			sB.append("Invalid number of characters to read from file\n");
			return sB;
		}
					
		int fd = -1;
		if (!(fd_Table.isEmpty()))                //If FDT is not empty, check if file is open.
			fd = fd_Table.getFD(targetFCB);            
		
		if (fd < 0)                               //Open file if it isn't.	
		{
			fd = tfs_open_getFD(targetFCB);
			if (fd < 0)                           //If file can't be opened, add error message to sB, return sB.
			{
				sB.append("Cannot open file for read.\n");
				return sB;
			}
		}
		
		tfs_seek_updateFDT(fd, position);                     //Seek to position in file.
		
		byte[] readBuf = new byte[number + 1];                //Buffer to read bytes into
		if ( (tfs_read_bytes_FDT(fd, readBuf, number)) < 0)   //Read number bytes into readBuf.
		{
			sB.append("Read failed.\n");
			return sB;
		}
		
		try
		{
			sB.append(new String(readBuf, ENCODING));       //Convert the read bytes into a String.
		}
		catch (UnsupportedEncodingException ex)
		{
			sB.append("Ouputting characters failed.\n");
			return sB;
		}
		
		return sB;                                         //Return ByteBuffer sB with containing String.
				
	}
	
	
	/**
	 * Renames the target file, if it exists in the parent directory and a
	 * file with the specified name does not already exist in that directory.  
	 * 
	 * @param path The path from the root to the target file to rename
	 * @param repName The new name for the target file.
	 * @return int 0 if success, -1 for general failure, or -2 if a file
	 * with given new name already exists in directory.
	 */
	public int tfs_rename(String path, String repName)
	{
		if (!isMounted)       //Check if TFS is mounted.
			return -1;
		
		String[] dirs = getValidPath(path);  //Get a valid path.
		if (dirs == null)                    //Make sure path was valid.
		{
			return -1;
		}
			
		
		int pathLen = dirs.length + 1;        //Length of path, add 1 for root.
		int traversed = 0;                    //Number of directories traversed.
		Directory parent = this.rD;          //First parent is root.
		FCB nextParent_FCB = null;
		
		while(traversed < pathLen - 2)       //Look through path to make sure it can be traversed to target.
		{
			if ( !(parent.contains(dirs[traversed], true)) )     //Make sure parent contains next dir in path.
			{   
				System.err.println("Directory " +dirs[traversed] + " not in path\n");
				return -1;
			}
						
			nextParent_FCB = parent.getFCBByName(dirs[traversed], true);                    //Get FCB of next parent.
			parent = tfs_load_dir(nextParent_FCB.getLocation(), nextParent_FCB.getSize());  //Load next parent.
			traversed++;
		}
		
		if ( !(parent.contains(dirs[traversed], false)))   //If parent doesn't contain target file, return -1.
		{
			System.err.println("File " +dirs[traversed] + " not in found \n");
			return -1;
		
		}
		
		if (parent.contains(repName, false))               //If parent of target already contains file with new name, return -2.
			return -2;
		
		/**
		 * Handling for open file.
		 */
		int fd = -1;
		boolean fileOpen = false;
		FCB oldTarget = parent.getFCBByName(dirs[traversed], false);
		if (!(fd_Table.isEmpty()) && fd_Table.isOpen(oldTarget))
		{
			fd = fd_Table.getFD(oldTarget);                        //Get old FCB's fd.
			fileOpen = true;
		}
		
		parent.updateEntryName(dirs[traversed], repName, false);   //Call Directory class updateEntryName() method to update entry.
		FCB updatedTarget = parent.getFCBByName(repName, false);   //Get new FCB.
		
		/*
		 * Since file will only be accessed by one process at a time, it can be renamed while open.
		 * Update entry in fd_Table.
		 */
		if (fileOpen)
		{			
			fd_Table.updateFCB(fd, updatedTarget);
		}
		
		//Special case for target file in root directory. rD and parent point to the same object.
		if (pathLen == 2)
			return ( (tfs_store_dir(rD, PCB.getRootDir())) < 0 ? -1 : 0);
		
		//General case. Store parent back to disk at location given by the FCB which represents it, nextParent_FCB.
		else
			return ( (tfs_store_dir(parent, nextParent_FCB.getLocation())) < 0 ? -1 : 0);
				
	}
	
	
	/**
	 * Copies the contents of one file into a new file at a specified destination.
	 * The contents of the original file is not changed.
	 * 
	 * Checks to make sure that the source file exists in its parent directory and
	 * that the destination file to create does not already exist in its parent 
	 * directory.
	 * 
	 * @param sourcePath Path from route to source file.
	 * @param destinationPath The path from root to destination file to create.
	 * 
	 * @return 0 if success, -1 if a general error occurs, -2 if the source file
	 * cannot be found, and -3 if the destination file already exists in the
	 * location specified.
	 * 
	 */
	public int tfs_cp(String sourcePath, String destinationPath)
	{
		String[] sPath = getValidPath(sourcePath);             //Get source path array.
		if (sPath == null)
		{
			System.err.println("Invalid source path.\n");
			System.err.flush();
		}
		
		if ( (getValidPath(destinationPath)) == null)          //Check destination path.
		{
			System.err.println("Invalid destination path.\n");
			System.err.flush();
		}
		
		//Check if source file exists.
		
		int pathLen = sPath.length + 1;        //Length of path, add 1 for root.
		int traversed = 0;                    //Number of directories traversed.
		Directory parent = this.rD;          //First parent is root.
		FCB nextParent_FCB = null;
		
		while(traversed < pathLen - 2)       //Look through path to make sure it can be traversed to target.
		{
			if ( !(parent.contains(sPath[traversed], true)) )     //Make sure parent contains next dir in path.
			{   
				System.err.println("Directory " +sPath[traversed] + " not in path\n");
				System.err.flush();
				return -1;
			}
						
			nextParent_FCB = parent.getFCBByName(sPath[traversed], true);                    //Get FCB of next parent.
			parent = tfs_load_dir(nextParent_FCB.getLocation(), nextParent_FCB.getSize());  //Load next parent.
			traversed++;
		}
		
		FCB sourceFCB = parent.getFCBByName(sPath[traversed], false);                       //FCB of the source file.
		
		if ( sourceFCB == null)                                    
		{
			System.err.println("File " +sPath[traversed] + " not found \n");
			System.err.flush();
			return -2;              //If parent doesn't contain source file, return -2.
		}
		
		//Check if source file is empty.
		if (sourceFCB.getSize() == 0)
		{
			System.err.println("Source file empty\n");
			System.err.flush();
			return -1;
		}
			
		/*
		 * If the parent contains the source file, continue with creating destination file at the specified destination.
		 * New file will be same size as source file.
		 */
		int writeLoc = tfs_create(destinationPath, sourceFCB.getSize());
		
		if (writeLoc == -2)                 //File already exists in parent directory error.
			return -3;
		
		if (writeLoc == -1)                //General error.
			return -1;
		
		byte[] srcBytes;                    //Pointer to memory location holding bytes from source file.
		
		try
		{
			srcBytes = tfs_read_blocks(sourceFCB.getLocation());            //Now read from source file into memory.
		} catch (IllegalArgumentException e)
		{
			System.err.println("Reading from source file failed\n");
			System.err.flush();
			e.printStackTrace(System.err);
			return -1;
		}
		
		int writeResult = tfs_write_blocks(srcBytes, writeLoc);		       //If read is successful, write bytes to destination file.
		if (writeResult < 0)
		{
			System.err.println("Writing to destination file failed.\n");
			return -1;
		}
		
		return 0;	
		
		
	}
		
	
	
	/**
	 * Exit the file system by calling unmount to store PCB and FAT in disk, and then
	 * closing the file session by calling TFSDiskInputOutput.tfs_dio_close().
	 * 
	 * @return 0 if exit is successful, -1 if it fails.
	 */
	public int tfs_exit()
	{	
		if (isMounted)                                  //Unmount if mounted.
			tfs_unmount();
		return TFSDiskInputOutput.tfs_dio_close();		//Close session.
				
	}
	
	/**
	 * Simple method to check if file system is mounted or not
	 * 
	 * @return boolean true if mounted, false otherwise
	 */
	public boolean tfs_isMounted()
	{
		return (isMounted ? true : false);
	}
	

	
	
	
	
	
	
	
	/****************************************************************************
	 * 
	 * Private methods for handling the many different supporting functions 
	 * needed by the methods in the public TFSFileSystem API. 
	 * 
	 ****************************************************************************
	 */
		
	/**
	 * Reads length bytes into buf from the file associated with the file descriptor fd.
	 * Returns the number of bytes read. Read starts at current file offset in FDT.
	 * Only the required number of blocks are read so that the number of disk
	 * reads is minimized. Other file blocks are not read.
	 * 
	 * @param fd The file descriptor for file from which to read from disk.
	 * @param buf Buffer in memory to read into.
	 * @param length The number of bytes to read from disk.
	 * 
	 * @throws IllegalArgumentExcpeption if fd is invalid
	 * @return int bytes read, or -1 if an error occurs in read operation
	 */
	private int tfs_read_bytes_FDT(int fd, byte[] buf, int length)
	{
		if (length <= 0)                      //If length is invalid.
		{
			System.err.println("Invalid value for length. Read failed");
			return -1;
		}
		if (buf.length < length)              //If capacity of buf is < length, return -1
		{
			System.err.println("Not enough space in buffer. Cannot read from file.");
			return -1;
		}
					
								
		int blSize = PCB.getBlockSize();
		int offset = fd_Table.getOffset(fd); //Get current offset.            
		if (offset < 0)                     //Make sure file is open.
		{
			System.err.println("File not open. Cannot read from file.\n");
			return -1;                
		}
		/**
		 * Calculate start location (block) for read, given current offset.
		 */
		int readLocation = fd_Table.getEntryLocation(fd);   //Location (start block of file) on disk.
		             
		int startBlockNum = offset / blSize;        //The nth block used by file where offset is.    
		for (int i = 0; i < startBlockNum; i++)   //Find actual block num of nth block.
		{
			readLocation = PCB.getFAT()[readLocation];
		}
		
		byte[] tmpBytes = new byte[blSize];    //Temporary buffer for blocks.
		int bytesRead = 0;                      //Number of bytes read so far.
		int inner = 0;                          //Position in tmpBytes.
		
		
		
		/**
		 * Read the first block of (possibly) others into memory.
		 * The extra bytes in the block are NOT written to buf, only
		 * bytes beginning at current offset.
		 */
		if ( (TFSDiskInputOutput.tfs_dio_readBlock(readLocation, tmpBytes)) < 0)
		{
			return -1;
		}
		/*
		 * Calc number of bytes to discard. This relies on fractional part of being discarded when
		 * startBlockNum is calculated.
		 */
				
		int discardBytes = offset - ((startBlockNum) * blSize );        
		
		int i;
		for(i = 0; i < length && i < buf.length && ( (discardBytes < tmpBytes.length)); i++)
		{
			buf[i] = tmpBytes[discardBytes];
			discardBytes++;
		}
		bytesRead = i;                      //Number of bytes copied into buf.
			
		
		/*
		 * Iterate and write contents of tmpBytes to buf on each pass. Increment by block size.
		 * Continue until EOF is reached, or length bytes have been read. Uses FAT
		 * to get block number for each linked block where data is stored. 
		 * 
		 * This block won't execute if bytesRead is already == length.
		 * Note that curBlock is updated to the next block to read.
		 */
		for(int curBlock = (PCB.getFAT())[readLocation]; curBlock > 0 && bytesRead < length; curBlock = (PCB.getFAT())[curBlock])
		{
			if( (TFSDiskInputOutput.tfs_dio_readBlock(curBlock, tmpBytes)) < 0 )  //Read from disk into tmpBytes.
			{
				System.err.println("Disk read failed.\n");                           //Make sure read succeeded.
				return -1;
			}
			/*
			 * Now copy from tmpBytes into buf until entire block has been copied, or
			 * a total of length bytes have been copied.
			 */
			  while (inner < tmpBytes.length && bytesRead < length)
			  {
				  buf[bytesRead] = tmpBytes[inner];
				  bytesRead++;                  //Increment number of bytes read.
				  inner++;                      //Increment position in tmpBytes.
				  
			  }
			  
			  inner = 0;                       //Reset inner to 0 for (possible) next block.
		}
		
		return bytesRead;                      //Return number of bytes read.            
	}
	
	/**
	 * Writes length bytes to the location associated with the FDT entry for fd.
	 * Write starts at the current offset for the specific file, also held in
	 * FDT entry.
	 * 
	 * The number of disk reads and writes is limited by only reading part
	 * of the file into memory. That is, those blocks that will be modified
	 * and written back to disk. No other blocks are modified.
	 * 
	 * Returns the number of bytes written.
	 * 
	 * @param fd The file descriptor for file to write to disk
	 * @param buf Memory location from which to write to disk.
	 * @param length Number of bytes to write to disk.
	 * @return int The number of bytes written or -1 if an error occurs.
	 * 
	 */
	private int tfs_write_bytes_FDT(int fd, byte[] buf, int length)
	{
		if (length < 0)              //If length is invalid.
		{
			System.err.println("Invalid value for number of bytes to write");
			return - 1;
		}
		
		//If size of buf is smaller than length, meaning length bytes CANNOT be written
		if (buf.length < length)
		{
			System.err.println("Cannot write. Buffer has < length bytes to write.");
			return -1;
		}
			
		int location = 0;               //Disk location of file (starting block).
		int offset = 0;                 //Offset at which to begin write.
		try
		{                               //Attempt to get location.
			location = fd_Table.getEntryLocation(fd);
			
		}catch (IllegalArgumentException e)
		{
			System.err.println("Error. Cannot write to disk. File not open.");
			return -1;                          //Return -1 if entry for fd not in table.
		}
		
		/**
		 * Read part of file into memory, starting at the current offset.
		 * 
		 */
		
		
		offset = fd_Table.getOffset(fd);        //Get current offset in file.
		System.out.println("File offset at beginning of current write is  " + offset +"\n");
		
		int startBlockNum = offset / PCB.getBlockSize();  //Which nth block of file?
		int readLocation = location;
		for (int i = 0; i < startBlockNum; i++)           //Find actual block, beginning at 1st block of file.
		{
			readLocation = PCB.getFAT()[readLocation];
					
		}
		
		byte [] filePart;                                 //Will point to memory where part of file is stored.
		
		try{
		filePart = tfs_read_blocks(readLocation);        //Read blocks into memory, starting at readLocation.
		} catch (IllegalArgumentException e)
		{
			e.printStackTrace(System.err);
			return -1;
		}
		//Calculate number of bytes to move forward in filePart before beginning copy.
		int skip = offset - (startBlockNum * PCB.getBlockSize());
		
		/*
		 * Expand capacity of, if necessary when number of bytes wanted to be written is
		 *  larger than the bytes that were read in - the bytes that will be skipped.
		 *  This is filePart.length - skip.
		 */
		
		if (length > filePart.length - skip)
		{
			byte [] tmp = Arrays.copyOf(filePart, length + skip);  //The new expanded array.
			
			filePart = tmp;                 //filePart now points to expanded array.
		}
		
		/*
		 * Copy from buf into the part of file's bytes in memory. 
		 * Start after the bytes that were skipped initially.
		 */
		
		for (int cur = 0; cur < length; cur++)
		{
			filePart[cur+skip] = buf[cur];
		}
		
		//Now write the modified blocks back into file, starting at readLocation.
		try
		{			
			if ( (tfs_write_blocks(filePart, readLocation)) < 0)
				return -1;
								
		}catch (RuntimeException ex)            //If an exception occurs during write, print the stack trace and return -1.
		 {
			ex.printStackTrace();
			return -1;
		 }
		
		return length;                         //Otherwise return length to indicate success.
		
	}
	
		
	
	/**
	 * Loads a directory from disk from the given location (block) into the buffer.
	 * Uses tfs_read_blocks() to read the blocks disk into memory from the given location.
	 * These bytes are then use to reconstruct a Directory object in memory, which is
	 * returned. 
	 * 
	 * Size parameter will typically be found by consulting the FCB which manages this 
	 * directory.
	 * 
	   @param location int block number of location (block) on disk to load
	 * directory from.
	 * @param size The exact size of the directory to be loaded.
	 *
	 * @return Directory object loaded from disk.
	 * @throws IllegalArgumentException if location or size given is invalid.
	 */
	
	private Directory tfs_load_dir(int location, int size)
	{
		byte[] bDir = tfs_read_blocks(location);   //Call tfs_read_blocks and load them into memory here.
		
		/*
		 * Now use the byte[] to reconstruct the Directory in memory, of size given by int size.
		 * This is done by static Directory.bytesToDir() method. Note that this method
		 * will not catch any IllegalArgumentException thrown by Directory.bytesToDir().
		 */
		return Directory.bytesToDir(bDir, size);
	}
	
	/**
	 * Stores a directory to disk at the given location (block). Uses tfs_write_blocks()
	 * to write the blocks of the directory to disk to the given location. Note that
	 * tfs_write_blocks calls tfs_sync() to synchronize file system data in memory with
	 * data held on disk.
	 * 
	 * @param dir The Directory to store to disk.
	 * @param location int block number of the location to store the directory.
	 * @return 0 if success, or -1 if write fails.
	 */
	private int tfs_store_dir(Directory dir, int location)
	{
		return tfs_write_blocks(dir.getByteArr(), location);
				
	}
	
    /**
	 * Writes the bytes in the buffer to disk, starting at the specified block number int
	 * location. Handles several cases which may occur:
	 * 
	 * 1)Starting block is empty, so data is written to an empty section of disk.
	 *   Avoid FAT lookup if possible by checking if specified location is first free block
	 *   index maintained by PCB.
	 *   
	 * 2)Starting block is NOT empty. 
	 *   FAT lookup occurs to find linked blocks of existing data (file or directory). 
	 *   Data stored in these blocks is overwritten.
	 * 
	 * In both cases, if additional blocks are required, these are obtained by looking up available 
	 * block in the FAT.
	 * The FAT is updated accordingly and written back to disk.
	 * 
	 * @param buf The buffer in memory to write from.
	 * @param location The location (block num) to begin write. 
	 * @return int The starting block written to if success or -1 if write fails.
	 * @throws IllegalArgumentException if location (block number) is invalid.
	 * @throws RuntimeException if sufficient space is not available on disk.
	 */

	private int tfs_write_blocks(byte[] buf, int location) throws IllegalArgumentException
	{
		if (location < 0 || location > PCB.getNumBlocks())  //Invalid location, throw IllegalArgumentException.
			throw new IllegalArgumentException("Cannot write blocks. Illegal argument for disk location (block). Block number " + location + " not in file system.");
		
		int blocksNeeded = 0;
		if (buf == null)      //For empty blocks, just write a dummy block.
			blocksNeeded = 1;
		else
			blocksNeeded = tfs_calcBlocksNeeded(buf.length);      //Calc number of blocks needed.
		
        int blSize = PCB.getBlockSize();                          //Current block size. 
		
		/**
		 *  Case 1: Starting block is empty. Write will begin at first free block index stored in PCB.
		 *  Write continues across available blocks found in FAT, if additional blocks are required.
		 *  Resets first free block index in PCB to another free block.
		 */
		 if (PCB.getFAT()[location] == 0)
		 {
			  //If only one block is needed.
			  if (blocksNeeded == 1)
			  {
				TFSDiskInputOutput.tfs_dio_writeBlock(location, buf); //Now write the block to disk.
				PCB.updateFAT(location, -1);                          //Mark entry in FAT as EOF.
				if (location == PCB.getFirstFreeBlock())              //If first free block has been used
					PCB.setFirstFreeBlock(tfs_getOneFreeBlock());     //Find another free block to which to set value of first free block.
			  }
			  
			  //If more than one block is needed, use tfs_getFreeBlocks to get a queue of available blocks.
			  else 
			  {
				Queue<Integer> freeQ = tfs_getFreeBlocks(blocksNeeded - 1);    //Get one less block, since arg location will be used for first.
				int curPos = 0;                                              //Current position in buf.
                int writeLoc = location;                                     //First write location. 
		         
				 /*
				  * Write all the bytes in buf to available blocks.
				  * Write blocks as subsets of buf.
				  */
				  while(curPos < buf.length)
				   {
						/*
						 * Write subset of buf of size block size to disk at writeLoc, which is a free location found in freeQ.
						 * FAT is updated accordingly so that blocks are linked in their FAT entries. Current write location's
						 * (block's) entry is updated with value at head of the queue of free blocks, freeQ.
						 */
						  //Perform write of subset of buf.
						 int writeResult = TFSDiskInputOutput.tfs_dio_writeBlock(writeLoc, Arrays.copyOfRange(buf, curPos, (curPos + blSize))); 
						 if (writeResult < 0)
						 {
							 System.err.println("Write to disk block "+writeLoc + " failed.");
							 return -1;
						 }
							 
						  if (!(freeQ.isEmpty()))                                                        //Make sure there are more free blocks.
						  {
							PCB.updateFAT(writeLoc, freeQ.peek());                                     //Update FAT to reflect write.
							writeLoc = freeQ.poll();                                                  //Get next write location.
						  }
						 
						 curPos += blSize;                                                 //Add block size to cur position in buf.              
				   }
				   
				   PCB.updateFAT(writeLoc, -1);                                                        //Mark last block written as EOF.
				   
				   if (location == PCB.getFirstFreeBlock())              //If first free block has been used
						PCB.setFirstFreeBlock(tfs_getOneFreeBlock());     //Find another free block to which to set value of first free block.
				   
				}   //End-else
				
		}  //End-if
		
		
		/**
		 *  Case 2: Starting block is not empty. This implies that the location and any logically linked blocks already
		 *  contain data and must be overwritten. Performs check to make sure that enough space is available. Free
		 *  blocks are acquired, if needed. Also, excess blocks marked as linked in FAT are marked as free.
		 *  
		 **/
		else
		{
			/*
			 * Continue writing from buf until EOF is reached. Check to see if
			 * all blocks could be written over existing blocks. If buf.length
			 * is shorter than the space available in the currently linked blocks,
			 * free up these blocks by marking them with 0 in FAT.
			 */
			int curEntry = location; 
            int curPos = 0;                                      //Current position in arg buf.
			int blocksWritten = 0;                               //Number of blocks written so far. 
			
			//Continue until current EOF is reached. Write at least one block to location given.
			do            		
		    {
			     if (blocksWritten < blocksNeeded)                //If there is more to write, write the current block at curEntry.
				 {
			    	if (buf != null)                              //If buf is empty, no need to actually write.
			    		TFSDiskInputOutput.tfs_dio_writeBlock(curEntry, Arrays.copyOfRange(buf, curPos, (curPos + blSize)));
			    	
					blocksWritten++;
					curPos += blSize;                           //Update position in buf.
				 }
				 //Otherwise, update FAT to reflect the fact that some blocks are now available, since fewer were needed.
				 else
				 {
					PCB.updateFAT(curEntry, 0);             //Mark these blocks as free.
				 }
			   //Set value of curEntry to next linked block value held in this entry. Don't update it if value is EOF.
				 if ( (PCB.getFAT()[curEntry]) != -1)
					 curEntry = PCB.getFAT()[curEntry];  
				 
			}while ( (PCB.getFAT()[curEntry]) != -1);
			
			
			
			//If more blocks are still needed, get free blocks from FAT and update FAT accordingly.
		    if(blocksWritten < blocksNeeded) 
			{
			    Queue<Integer> availBlocks = tfs_getFreeBlocks(blocksNeeded - blocksWritten);
			    PCB.updateFAT(curEntry, availBlocks.peek());                    //Update FAT to reflect previous write.
				curEntry = availBlocks.poll();                                  //Set next write location at next free block.
				
				while (curPos < buf.length)
				{
				          //Perform write of subset of buf.
						 TFSDiskInputOutput.tfs_dio_writeBlock(curEntry, Arrays.copyOfRange(buf, curPos, (curPos + blSize)));   
						  if (!(availBlocks.isEmpty()))                                     //Make sure there are more free blocks.
						  {
							PCB.updateFAT(curEntry, availBlocks.peek());                    //Update FAT to reflect write.
							curEntry = availBlocks.poll();                                  //Get next write location.
						  }
						 
						 curPos += blSize;                                                 //Add block size to cur position in buf.
				}
				
				PCB.updateFAT(curEntry, -1);                                              //Mark last block written to as EOF.
				
			}
		
		}//End-else
			  
		 //Call tfs_sync() to write PCB back to disk, with FAT.
		tfs_sync();
		
		return location;
		
	}
	
	
	/**
	 * Overloaded version of tfs_write_blocks(byte[] buf, int location)
	 * Takes no arg for location, so any location (start block) may be used on disk.
	 * 
	 * Useful if desired location is not known and any location may be used.
	 * First free block stored in PCB will be used for initial block by
	 * calling PCB.getFirstFreeBlock.
	 * 
	 * @param buf The byte buffer to write to disk
	 * @return The starting location of write or -1 if write fails.
	 */
	private int tfs_write_blocks(byte[] buf)
	{
		return tfs_write_blocks(buf, PCB.getFirstFreeBlock()); //Start write at first free block.
			
	}
	
	
	/**
	 *  Reads blocks starting at the given disk location and returns an array of bytes
     *  holding the bytes of these blocks.
	 *  
	 *  FAT is consulted to ensure that all linked blocks are read following this block.
	 *  Read continues until EOF marker (-1) entry is found at an index for a given block.
	 * 
	 * @param location int giving starting location (block) for read
	 * @return byte[] holding bytes read from location.
	 * @throws IllegalArgumentException if block number is invalid or block is empty.
	 */
	
	private byte[] tfs_read_blocks(int location)
	{
		if (location < 0 || location > PCB.getNumBlocks())  //Invalid location, throw IllegalArgumentException.
			throw new IllegalArgumentException("Cannot read blocks. Illegal argument for disk location (block). Block number " + location + " not in file system.");
		
		else if ((PCB.getFAT())[location] == 0)  //If block is empty, throw IllegalArgumentException.
			throw new IllegalArgumentException("Cannot read from block " + location + " since block is empty.");
		
		
		int numBlocks = 0;
		
		//Determine the size of the output array  by looking in FAT. Count num blocks until an entry marked EOF is reached.
		for (int fB = location; fB > 0; fB = (PCB.getFAT())[fB])
		{
			numBlocks++;
		}
				
		byte[] outArr = new byte[PCB.getBlockSize() * numBlocks]; //The output array of bytes of size block size * numBlocks.
		int outPos = 0;                                          //Position in large array of bytes to output.
		int bufPos = 0;                                        //Position in the small buffer.
		
		/*
		 *  Outer loop to get linked blocks using FAT lookup to get block numbers.  Continue until FAT entry holding EOF (-1) is encountered,
		 *  meaning current block won't have any blocks following it.
		 *  Moves curBlock to next block after each iteration.
		 */
		for (int curBlock = location; curBlock > 0; curBlock = (PCB.getFAT())[curBlock])
        {
			byte[] buf = new byte[PCB.getBlockSize()];                                  //The buffer.
			TFSDiskInputOutput.tfs_dio_readBlock(curBlock, buf);                         //Read the block into the buffer buf.

            while (bufPos < buf.length && outPos < outArr.length)                 //Add bytes from current buffer to the large output array.
			{
				outArr[outPos] = buf[bufPos];
				
				outPos++;          //Increment both indexes.
				bufPos++;
			}
			//Reset buffer index after buffer has been read into outArr.
			bufPos = 0;
		}
			
		return outArr;                                                                //Return the large array of bytes of these block(s).
		
			
	}
	
	
    /**
	 * Service method for calculating the number of blocks that will
	 * be needed to store a given element.
	 * 
	 * @param elemSize The size of the file or directory to be stored.
	 * @return int The number of blocks needed to store an element.
	 */
	private int tfs_calcBlocksNeeded(int elemSize)
	{
		//If size is 0 bytes, 0 blocks are really needed but return 1 so dummy blocks may be written.
		if (elemSize == 0)     
			return 1;
		if (elemSize <= PCB.getBlockSize()) //If size is <= blocksize, 1 block is needed.
			return 1;
		
		//Otherwise more blocks are needed.
		int blocksNeeded = elemSize / PCB.getBlockSize();       //Calc number of blocks needed to store directory.
			
		//If elemSize is not divisible by blocksize, an extra block is needed for remainder.
		if ((elemSize % PCB.getBlockSize()) != 0);
			blocksNeeded++;
			
		return blocksNeeded;
	}
	
	/**
	 * Finds index of one free block in FAT and returns it.
	 * Ignores the block number of first free block entry in FCB.
	 * 
	 * @return int index of one free block, or -1 if no block found.
	 */
	private int tfs_getOneFreeBlock()
	{
		int [] tmpFAT = PCB.getFAT(); 
		int numBlocksTFS = PCB.getNumBlocks();
				
		//Start looking just after root directory. Ignore first free block.
		for (int i = (PCB.getRootDir() + 1); i <= numBlocksTFS; i++)
		{
			if ((tmpFAT[i] == 0) && (i != PCB.getFirstFreeBlock()))
				return i;
		}
				
		return -1;
			
	}
	
	/**
	 * Service method for block numbers of n free blocks. These
	 * are returned as a queue of Integers which are the block numbers of
	 * the free blocks. Consults FAT to find n free blocks.
	 * 
	 * Note that, like tfs_getOneFreeBlock, first free block will NOT
	 * be included. 
	 * 
	 *  
	 * @param n The number of free blocks needed
	 * @return List<Integer> A list with indexes of free blocks with one extra free block
	 * @throws RuntimeException If enough blocks could not be found.
	 * 
	 */
	private Queue<Integer> tfs_getFreeBlocks(int n)
	{
		int num = n;                             //Number of blocks required.
		int blocksFound = 0;
		int [] tmpFAT = PCB.getFAT();            //Get the current FAT in memory.
		int numBlocksTFS = PCB.getNumBlocks();
		Queue<Integer> freeQ = new LinkedList<Integer>();
		
				
		/*
		 * If more blocks are needed, find the required number of free blocks and store 
		 * their indexes in list
		 * Does not attempt to search blocks before or at block of rootDir.
		 */
		for (int i = (PCB.getRootDir() + 1); blocksFound < num && i < numBlocksTFS; i++)
		{
			if ((tmpFAT[i] == 0) && (i != PCB.getFirstFreeBlock()))   //If block is a free block, add its index to list
			{
				blocksFound++;           //Increment blocks found to indicate another has been found
				freeQ.add(new Integer(i));
			}
			
		}
		
		if (blocksFound < num)            //Throw RunTimeException if enough blocks not found.
			throw new RuntimeException("Insufficient space available on disk.");
		
		return freeQ;
		
	}
	
	/**
	 * Logically frees up linked blocks on disk, starting at the location specified. 
	 * This is simply done by updating FAT.
	 * Prevents clearing the PCB or FAT blocks.
	 * 
	 * @param location The location from which to start
	 *
	 * @throws IllegalArgumentException if location is not within file system or numBytes is invalid.
	 * 
	 * 
	 */
	private void tfs_clear_blocks(int location)
	{
		if (location < PCB.getRootDir() || location > PCB.getNumBlocks())
			throw new IllegalArgumentException("Cannot clear blocks. Invalid location given");
						
		//Set entries in FAT to 0 for these linked blocks to reflect changes.
		
		int curBlock = location;
		int oldVal = curBlock;
		do
		{
			oldVal = PCB.getFAT()[oldVal];     //Save the value previously held in this index.
			PCB.updateFAT(curBlock, 0);        //Set index to 0.
			curBlock = oldVal;
					
		} while (oldVal > 0);
			
		tfs_sync();                           //Sync to store updated PCB to disk, with FAT.
				
	}
	
	
	
	/*********************************************************************************
	 * Private FDT related methods for updating FDT.
	 * Used when opening and closing as well as seeking.
	 */
	
    /**
	 *  Creates an entry in the FDT for a file that will be opened. FCB is taken as arg, since
	 *  an FCB object contains all the information that will be stored for entry, including
	 *  name, length, location (first block).
	 *
	 *  Then returns the file descriptor for this file, so its entry can be looked up in FDT later
	 * 
	 *  @param fileFCB The FCB representing the file that will be opened.
	 *  @return int File descriptor for this file or -1 if exception occurred.
	 */
	private int tfs_open_getFD(FCB fileFCB)
	{
		int fd = -1;
		try{
		fd = fd_Table.add(fileFCB, 0);        //Add the FCB as entry to the FDT, offset set at 0 initially. Returns the fd.
		}catch(RuntimeException ex)
		 {
		   return -1;                        //Return -1 if entry could not be added to FDT.
		 }
		
		return fd;                          //Otherwise return the file descriptor.
	}
	
	/**
	 *  Updates the file offset for the FDT entry of the file 
	 *  associated with file descriptor fd.
	 *
	 *  @param fd The file descriptor for the file
	 *  @param offset The updated file offset.
	 *  @return int File descriptor fd, or -1 if exception occurs.
	 */
	 
	private int tfs_seek_updateFDT(int fd, int offset)
	{   
		try
		{
			fd_Table.updateOffset(fd, offset);           //Set the offset in FDT fd_Table for this fd.
		}catch(IllegalArgumentException ex)
		 {
			return -1;                                   //Return -1 if exception occurs in updating offset.
		 }
		
		return fd;
	}
	
	/**
	 *   Removes entry for file associated with fd from the FDT.
	 *   Makes sure FDT is not empty.
	 *   
	 *   @param fd File descriptor for entry to remove.
	 *   @throws IllegalArgumentException if entry could not be found in FDT
	 *
	 */
	private void tfs_close_updateFDT(int fd)
	{
		if (!(fd_Table.isEmpty()) && fd_Table.remove(fd) < 0)
			throw new IllegalArgumentException("Entry not in FDT. File not open.");
	}
	
			
	
	/*****************************************************************************
	 * Private byte handling methods. Provides functionality needed by other private
	 * methods for converting primitive types to and from bytes. Will mainly be used
	 * for adding data to files.
	 * Bytes are typically inserted at or loaded from given offset in a 
	 * given array of bytes.
	 * 
	 */
	
	
						
	/**
	 * Private method for converting int to an array of bytes.
	 * 
	 */
	private byte[] intGetBytes(int i ) 
	{
	    ByteBuffer buf = ByteBuffer.allocate(4); 
	    buf.putInt(i); 
	    return buf.array();			//Return array of length 4 of the 4 bytes in the buffer.
	}
	
	/**
	 * Private method for converting array of bytes to an int array.
	 * 
	 */
	private int [] arrBytesGetInt(byte [] b)
	{
		int [] outArray = new int[(b.length / 4)];  //Create array of size b/4 since ints take up 4 bytes.
		ByteBuffer wB = ByteBuffer.wrap(b);			//byte array is wrapped into buffer
		
		for (int j = 0; j < outArray.length; j++)   //Convert 4 bytes at a time, store in int outArray.
			outArray[j] = wB.getInt();
		
		return outArray;
		
	}
	
	/**
	 * Processes a string path into an array of string containing each of the elements
	 * along the path. Last element will contain target.
	 * 
	 * @param path The path to process
	 * @return String[] of the different directories + the target at last array subscript.
	 *  Will return null if path is invalid.
	 */
	private String[] getValidPath(String path)
	{
		/*
		 * Path must start with "/", as it is from root, and it must not end with "/". It
		 * also have a length >=2
		 */
		if (!(path.startsWith("/")) || path.endsWith("/") || path.length() < 2)                  
			return null;
		String tmpPath = path.split("/", 2)[1];       //Get rid of first '/'
		String[] dirs = tmpPath.split("/");          //Split the resulting path string by / symbol
		if (dirs.length == 0)                        //Return -1 since path contains no elements.
			return null;
		
		return dirs;
	}
			
	
}
		