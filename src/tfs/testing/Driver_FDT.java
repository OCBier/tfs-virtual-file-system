package tfs.testing;

/**
 * Driver program which tests the methods of the tfs.structures.FDT class. An FDT is created
 * and populated with several FCB's and corresponding current offsets within each. The various methods
 * of the FDT class are then called.
 * 
 * @author Oloff Biermann
 * @version 8.77
 *
 */

import tfs.structures.FCB;
import tfs.structures.FDT;

public class Driver_FDT 
{

	public static void main(String[] args) 
	{
		FDT testFDT = new FDT(10);          //Pointer to test FDT with size of 10.
		
		//A few test FCB's.
		FCB testFCB_1 = new FCB("test File 1", false, 16, 512);
		FCB testFCB_2 = new FCB("test File 2", false, 32, 1024);
		FCB testFCB_3 = new FCB("test File 3", false, 512, 2200);
		FCB testFCB_4 = new FCB("test File 4", false, 1000, 12);
		
		//Matching offsets corresponding to the positions within the files represented by each FCB.
		int offset_1 = 0;
		int offset_2 = 243;
		int offset_3 = 2200;
		int offset_4 = 0;
		
		//Add these entries to FDT.
		int fd1 = testFDT.add(testFCB_1, offset_1);
		int fd2 = testFDT.add(testFCB_2, offset_2);
		int fd3 = testFDT.add(testFCB_3, offset_3);
		int fd4 = testFDT.add(testFCB_4, offset_4);
		
		//Print the FDT.
		
		System.out.println(testFDT.toString());
		
		//Make sure update capability works for add() method by updating one of the entries
		//and then adding it again.
		
		testFCB_1.setSize(640);   //Increase size of testFCB_1
		offset_1 = 640;           //Change offset_1 to end of file.
		
		testFDT.updateFCB(fd1, testFCB_1);       //Update entry.
		testFDT.updateOffset(fd1, 50);           //Update offset within this file.
		
		//Print table again to make sure first entry was updated.
		System.out.println(testFDT.toString());
		
				
		System.out.println("\n\nRemove all entries from FDT. Before removes, isEmpty() returns: " + testFDT.isEmpty() + "\n");
		System.out.println("Calling isOpen() for the first entry using its file descriptor returns: "+ testFDT.isOpen(fd1));
		
		System.out.println("\nRemove all entries.");
		//Now delete all entries from FDT by calling remove using the file descriptors returned
		testFDT.remove(fd1);
		testFDT.remove(fd2);
		testFDT.remove(fd3);
		testFDT.remove(fd4);
		
		//Print table again to show they are removed.
		System.out.println("Table is now:\n");
		System.out.println(testFDT.toString());
		
		//Call isEmpty() to show table is empty().
		System.out.println("\nCalling isEmpty() shows that table is empty, since it returns: " + testFDT.isEmpty());
		System.out.println("Calling isOpen() for first entry now returns: " + testFDT.isOpen(fd1));

	}

}
