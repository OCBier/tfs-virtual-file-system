package tfs.testing;

/**
 *  Driver program to test the various methods provided by the tfs.structures.Directory class. 
 *  The most important aspect to test is to see if the class reliably converts Directory
 *  objects to bytes for storage using the getByteArr() method, and if the static 
 *  bytesToDir() method is able to create a new Directory correctly, given an array of bytes
 *  holding a Directory object.
 *  
 *  The addEntry(), find(), removeEntry() and the different update methods are also tested,
 *  as is the toString method. 
 *  
 * 
 * @author Oloff Biermann
 * @version 8.77
 *
 */

import tfs.structures.*;
import tfs.exceptions.*;


public class Driver_Directory 
{

	public static void main(String[] args) 
	{
		
		Directory testDir = new Directory();
		
		FCB heldTestFile = new FCB("Test_File", false, 200, 1000); //Test file at block 200 with size 1000 bytes.
		FCB heldTestDir = new FCB("Test dir", true, 42, 72);       //Held directory in directory containing 3 entries.
		FCB non_existant_entry = new FCB("doesn't exist", false, 500, 2000);
		//Now add entries to testDir.
		
		testDir.addNewEntry(heldTestFile);
		testDir.addNewEntry(heldTestDir);
		
		//Print the directory using toString()
		System.out.println("Print directory using toString() method\n" +testDir.toString());
		//Print the directory using listContents()
		System.out.println("Print directory using listContents() method\n" + testDir.listContents());
		
		//Test the find() method on existant entry "Test_File".
		System.out.println("\nCalling find() for first entry returns index " + testDir.find("Test_File", false));
		System.out.println("Calling find() for second entry returns index " + testDir.find("Test dir", true));
		
		//Test find() with non-existant entry.
		System.out.println("Calling find() for non-existant entry returns: " + testDir.find("Doesn't exist", false));
		
		System.out.println("\nUpdate name, location, and size of first entry");
		//Update first entry (which is a file) by calling the different update methods. Check results by printing directory again.
		testDir.updateEntryName("Test_File", "New Name", false);            //Change entry name.
		testDir.updateEntryLoc("New Name", 500, false);                    //Change entry location.
		testDir.updateEntrySize("New Name", 1200, false);
		
		System.out.println("Directory is now: ");
		System.out.println(testDir.toString());
		
		
		
		/*
		 * Test the removeEntry method and then put entries back in.
		 */
		System.out.println("Testing remove function by removing both entries:");
		FCB remEntry1 = testDir.removeEntry("New Name", false);
		FCB remEntry2 = testDir.removeEntry(heldTestDir);
		System.out.println("Removed first entry:\n" + remEntry1 + "\n\nRemoved second entry:\n" + remEntry2);
		
		/*
		 * Put entries back in using addNewEntry().
		 */
		System.out.println("\nPutting entries back in. Directory is now:");
		testDir.addNewEntry(remEntry1);
		testDir.addNewEntry(remEntry2);
		
		System.out.println(testDir);
		
		
		/*
		 * Byte conversion testing.
		 */
		System.out.println("\n"+ testDir.numEntries() + " entries with size of directory in bytes: " + testDir.getByteSize());  //Test getByteSize()
		
		System.out.println("\nConverting directory to array of bytes and restoring:");
		
		//Test the getByteArr() method to convert the directory to array of bytes.
		byte [] dirBytes = testDir.getByteArr();
		//Recreate the directory from bytes by using static bytesToDir() method.
		Directory restoredDir = Directory.bytesToDir(dirBytes, testDir.getByteSize());
		
		/*
		 *  Verify that the old directory and the new directory are the same by printing, 
		 *  and attemping to locate the two directory entries again within the restored directory.
		 */
		
		System.out.println("Restored directory:\n" + restoredDir);
		System.out.println("\n"+ testDir.numEntries() + " entries with size of directory in bytes: " + testDir.getByteSize());  //Test getByteSize()
		System.out.println("Finding the first (file) entry in restored dir returns index " + restoredDir.find("New Name", false)); 
		System.out.println("Finding the second (directory) entry in restored dir returns index " + restoredDir.find(heldTestDir));
				
		
		
		/*
		 * Finally, make sure exception is thrown if attempt is made to add an entry
		 * which already exists in directory or remove an entry which does not exist.
		 * Should throw DirModException.
		 */
		System.out.println("\n Attempt to add existing entry and remove non-existant entry. "
				+ "Exceptions should be thrown and caught here.");
		try
		{
		testDir.addNewEntry(remEntry1);         //Add entry which already exists.   
		 
		}catch (DirModException e)
		{
			e.printStackTrace();
		}
		
		try
		{
			testDir.removeEntry("non_existant_file_entry", false);
		}catch (DirModException e)
		{
			e.printStackTrace();
		}

	}

}
