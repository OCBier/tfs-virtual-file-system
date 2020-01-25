package tfs;

/**
 * TFSShell.java
 * 
 * This program makes all the functionality provided by TFSFileSystem API available as shell
 * commands. This allows a basic TFS file system to be created, modified and accessed.
 * 
 * TFSShell follows a pretty simple approach. Commands from the user are taken as Strings
 * and the appropriate methods from the TFSFileSystem API are called. 
 * 
 * If an error occurs or user enters an invalid command, the user is notified of the problem.
 * 
 * @author Oloff Biermann
 * @version 8.77
 *
 */

import java.util.Scanner;
import java.io.*;

public class TFSShell extends Thread
{
	private static final String PNAME = "TFSDiskFile";     //Constant for virtual disk name.
	private static final int SIZE = 65535;                 //Constant for size of file system.
	private static final int BLOCKSIZE = 128;              //Constant for block size.
	
	public static void main(String [] args) throws UnsupportedEncodingException
	{
		TFSFileSystem TFS = new TFSFileSystem();			//The TFS file system.
		
		System.out.println("*****TFS File System Shell******\n\n");
		System.out.println("Choose from the following commands to manage a TFS file system:");
		
		
		String[] commands = new String[17];               //Array of possible commands and descriptions of each.
		
		/**
		 * File system management commands for opening and printing metadata.
		 * 
		 */
		commands[0] = "help - |Display the list of available commands.";
		commands[1] = "mkfs - |Creates a new TFS file system.";
		commands[2] = "mount - |Mount the file system.";
		commands[3] = "sync - |Copy file system metadata to disk to store any changes.";
		commands[4] = "prrfs - |Prints the metadata currently on DISK (File Access Table and PCB)";
		commands[5] = "prmfs - |Prints the metadata currently IN MEMORY (File Access Table and PCB)\n";
		
		
		/**
		 *  Directory related commands.
		 * 
		 */
		commands[6] = "***Directory commands***\nmkdir /fullPath/.../dirName - |Creates a new directry down the given path, "
				+ "with the entered name if it does not exist.\nMaximum directory name length is 15 characters. Longer "
				+ "names will be truncated.\n"				
				+ "Intermediate directories will NOT be created.\nEntering \"mkdir /name_of_directory\" \n "
				+ "will create a directory in root directory.";
		
		commands[7] = "rmdir /fullpath/.../dirName - |Removes the directory at the given location,\n if it exists and it is empty.";
		
		commands[8] = "ls /fullPath/.../dirName - |Lists the contents of the directory \nat the given destination,"
				+ " with name, type (file or directory), and size of each entry.\nEntering \"ls /\" will print root directory. \n";
		
		
		/**
		 * The six file related commands.
		 * 
		 * 
		 */
		
		commands[9] = "***File Commands***\ncreate /fullPath/fileName - |Create a file down the path with the given name,\n"
				+ "if it doesn't exist. Note that max file length name is 15 characters.\nLonger names "
				+ " will be truncated. Entering \"create /name_of_file\" \n "
				+ "will create a file in root directory.";
		
		commands[10] = "rm /fullPath/fileName - |Removes the file if it exists.";
		
		commands[11] = "print /fullPath/Filename position numberChars - |Prints a number of characters\n"
				+ "from the position given in the file, if the file exists.";
		
		commands[12] = "append /fullPath/fileName  - |Adds an entered"
			   +" string of characters to the end of the file, if the specified file exists.";
				
		commands[13] = "cp /fullPath/Source_File /fullpath/Destination_File - | Copies "
				+ "the contents\nof one file to a destination directory.\nChecks if "
				+ "the Source_File exists and Destination_File does NOT exist.";
				
		commands[14] = "rename /fullpath/oldFileName newName - |Renames the file if a file\nwith the new name "
				+ "doesn't exist already in the file's directory.\n";
		
		
		
		/**
		 * File system management commands for closing file system.
		 * 
		 */
		
		commands[15] = "***Exit Commands***\numount |Unmounts the file system";
		commands[16] = "exit - |Exit from the shell and shut down the file system.";
		
		
		//Print the commands for the first time
		
		for (String str : commands)
			System.out.println(str+ "\n");
		
		//Now scan for input until shell is closed (exit).
		
		
				
		while (true)
		{
			String input = " ";
			Scanner scan = new Scanner (System.in);
			
					
			System.out.println("Enter TFS command: ");
								
			input = scan.nextLine();        //Read input.
			
						
			
			switch (input)
			{
				/**
				 * *********************************************************************
				 * 
				 * commands[0] - commands[5]
				 * File system management commands for opening and printing metadata.
				 * 
				 * **********************************************************************
				 */
				case "help":			//Command "help"
				{
					for (String s : commands)
						System.out.println(s+ "\n");
				}
				break;
				
				case "mkfs":   //Command mkfs, create the file system.
				{
					if(TFS.tfs_isMounted())                                 //Check if a TFS file system is already mounted.
					{
						System.out.println("Cannot create TFS file system. Existing file system already mounted.\n");
						break;
					}
					
					//First check to make completely sure, since this will overwrite any existing TFS file system.
					System.out.println("This will destroy any existing TFS file system.");
					System.out.println("Enter y to continue\nor any other key to stop and enter a different command instead:"  );
					input = scan.nextLine();
					
					if (input.equalsIgnoreCase("y"))  //If "y" or "Y" is entered then proceed.
					{
						System.out.println("Creating new TFS file system...");
						int result = TFS.tfs_mkfs(PNAME, SIZE, BLOCKSIZE);
						if (result != 0)
							System.out.println("Error occured. File system not created successfully.");
						else
							System.out.println("File system created OK");
					}
				}
				break;
				
				case "mount":   //Command mount, mount the file system.
				{
					if (TFS.tfs_isMounted())         //If it has already been mounted.
						System.out.println("File system has already been mounted. Cannot mount.");
					else                             //Else mount it.
					{ 
						System.out.println("Mounting file system...");
						int result = TFS.tfs_mount(PNAME, SIZE, BLOCKSIZE);
						if (result != 0)
							System.out.println("Error occured. File system not mounted successfully.");
						else
							System.out.println("File system mounted OK");
					}
										
				}
				break;
				
				case "sync":    //Command sync, which synchronizes file system by writing PCB and FAT to disk.
				{
					System.out.println("Synchronizing file system...");
					int result = TFS.tfs_sync();
					if (result != 0)
						System.out.println("Error occured. Synchronization failed.");
					else
						System.out.println("Synchronization complete.");
										
				}
				break;
				
				case "prrfs":   //Command prrfs, print TFS metadata from disk.
				{	
					  						  
					  try
					  {
					  System.out.println(TFS.tfs_prffs());
					  } catch(Exception e)
					  {
						  System.out.println("Could not print stored file system data: "+e.getMessage());
					  }
				}
				break;
				
				case "prmfs":   //Command prmfs, print TFS metadata from memory.
				{
					if (!(TFS.tfs_isMounted()))
					{
						System.out.println("Cannot print file system from memory. File system not mounted.\n");
						break;
					}
					 try
					  {
					  System.out.println(TFS.tfs_prmfs());
					  } catch(Exception e)
					  {
						  System.out.println("Could not print file system data in memory: "+e.getMessage());
					  }
				}
				break;
				
											
				/**
				 * *************************************************************************
				 * 
				 * commands[15] - commands[16]
				 * File system managements commands for closing.
				 * 
				 * *************************************************************************
				 */
				case "umount":  //Command umount. Unmounts the file system but does not close the session. Calls tfs_unmount();
				{
					if(!(TFS.tfs_isMounted()))      //Indicate if it has not been mounted.
						System.out.println("File system has not yet been mounted. Cannot unmount.");
					else
					{
						int result = TFS.tfs_unmount();
						if(result != 0)
							System.out.println("Error. Could not unmount file system");
						else
							System.out.println("Unmount successful.");
					}					
				}
				break;
				
				case "exit":   //Command exit. Unmounts file system and closes virtual disk session via tfs_exit().
				{
					int result = TFS.tfs_exit();
					if (result != 0)
					{
						System.out.println("Exit operation failed. Could not safely close file system. Aborting execution.");
						System.exit(-1);
						
					}
					else
					{
						System.out.println("Closing file system and exiting...\n");
						scan.close();
						System.exit(0);
					}
				}
				break;
							
				default:						             //Default case handles all other commands or invalid commands.
				{
					
					if (!(TFS.tfs_isMounted()))              //No point in going on if TFS is not mounted.
					{
						System.out.println("File system not mounted." );
						
					}
					else   
					{
						fileAndDirOptions(TFS, input);     //Otherwise another option is needed.
						
					}
					break;
				}
					
			} //End-switch
			
		} //End-while (infinite)
			
		
	} // End method main()
	
	
	/**
	 * Method for handling the other TFS functions.
	 * Handles possible errors and the case where
	 * no option matches the input.
	 * This method is called in main().
	 */
	public static void fileAndDirOptions(TFSFileSystem fileSys, String in)
	{
		/**
		 * ****************************************************
		 * 
		 * commands[6] - commands[8]
		 * Directory related commands.
		 * 
		 * ****************************************************
		 */
		
		//String regex for a path.
		//Pattern is: match exactly one '/' followed by any characters other than '/' or any spaces, one or more times.
		final String PATH_REGEX = "(/([^/|\\s])+)+"; 
		String path = "";
		 
		/*
		 * If statement block contains all commands taking 2 args.
		 * Make sure command follows general pattern "command fullPath".
		 * Eg. ls /folder1/folder2
		 * Eg. ls /
		 */
		
		if (in.matches("\\w+ /([^/\\s]+/?)*"))  
		{
			
			path = in.split(" ")[1];       //Get rid of first part of command on left side of space delimiter.
								
			//Command |mkdir fullpath/.../target| Creates a target directory down given path from root.
			if (in.matches("mkdir "+ PATH_REGEX))  
			{   try{
					if ( (fileSys.tfs_mkdir(path)) < 0 )           //Call tfs_mkdir, check if return value is not < 0.
					{
						System.out.println("Error. Could not create directory\n");
					}
				  
					else                                       //If creation success.
					{
						System.out.println("Directory created.\n");
					
					}
				} catch(Exception e)
				{
					   e.printStackTrace(System.err);
				}
				return;
			}
			
			//Command rmdir fullpath/.../target| Deletes target down given path from root, it it is empty.
			else if (in.matches("rmdir "+ PATH_REGEX)) 
			{
				int result = fileSys.tfs_rmdir(path);
				
			    if (result == 0)
			    {
			    	System.out.println("Directory deleted.\n");                  //Indicate successful creation.
			    }
			    
			    else if (result == -2)                                          //Directory not empty.
			    {
			    	System.out.println("Cannot delete directory. Directory is not empty.");
			    }
			    
			    else 
			    {
			    	System.out.println("Error. Directory could not be deleted\n");  //Or indicate general failure.
			    }
			    return;
				
			}
			
			//Command |ls fullpath/.../target| Lists contents of directory down given path from root.
			else if (in.matches("ls "+PATH_REGEX) || in.matches("ls /"))  
			{
											
				//Special case for printing root directory.
				if(in.matches("ls /"))
				{
					System.out.println("Printing root directory...\n");
					System.out.println(fileSys.tfs_ls("/").toString());             //Print the returned StringBuffer.
				}
				
				//General case for directory.
				else
				{
					System.out.println(fileSys.tfs_ls(path).toString());            //Print the returned StringBuffer.
				}
				return;
			}
			
			/**
			 * ****************************************************************
			 *  commands[9] - commands[14]
			 *  File related commands. 
			 * 
			 * ****************************************************************
			 **/
			
			/**
			 * Command "create /fullPath/fileName" - |Create a new file down the path with the given name,"
			 * if it doesn't exist in the specified parent directory.
			 **/ 
			else if (in.matches("create "+PATH_REGEX))
			{
				int result = fileSys.tfs_create(path);
				
				if (result >= 0)
					System.out.println("File created.\n");   //Success
				else if (result == -2)                       //File already exists
					System.out.println("Error. File already exists in directory.\n");
				else                                        //General error.
					System.out.println("Could not create file");
				
				return;
			}
			
			
			/**
			 * Command "rm /fullPath/fileName" - |Removes the file if it exists.
			 * 
			 */
			else if (in.matches("rm " +PATH_REGEX))
			{
				int result = fileSys.tfs_rm(path);
				
				if (result >= 0)
					System.out.println("File deleted.\n");   //Success
				else if (result == -2)                       //File already exists
					System.out.println("Error. File does not exist in directory.\n");
				else                                        //General error.
					System.out.println("Could not remove file");
				
				return;
											
			}
								
		} //End-if for all commands taking only two args, and nothing else.
		
		/**
		 * Following are all commands taking MORE than 2 args.
		 * These are all the rest of the file related commands.
		 **/
		       			
			/**
			 *  Command "print /fullPath/Filename position numberChars" - |Prints a number of 
			 *  characters from the position given in the file, if the file exists.
			 **/
			
			if( in.matches("print "+PATH_REGEX +"( \\d+){2}") )
			{
				String [] cm = in.split(" ");               //Split input into 4 Strings.
				
				StringBuffer buf = fileSys.print( cm[1], Integer.parseInt(cm[2]), Integer.parseInt(cm[3]) ); //Call tfs print()
				
				System.out.println("\nAttempt to read " + cm[3] +" characters... \n" + buf.toString() + "\n");        //Get string in returned buffer.
				
				return;
			}
			
			
			/** 
			 *  Command "append /fullPath/fileName numberChars" - |Adds the specified
			 *  string of characters to the end of the file.
			 *  
			 *  Prompts user to input a string.
			 **/
			
			else if( in.matches("append " +PATH_REGEX ) )
			{
				path = in.split(" ")[1];           //Get the path.
				String app = "y";
				StringBuffer buf = new StringBuffer(512);   //String buffer with initial capacity of 512.   
				
				@SuppressWarnings("resource")
				Scanner readChars = new Scanner(System.in);
								
				while (app.equalsIgnoreCase("y"))
				{
					System.out.println("Enter a string of characters to append to the end of the file:\n");
					if(readChars.hasNextLine())
					{
						buf.append(readChars.nextLine() + "\n");   //Add input to String Buffer buf.
						
					}
					if (buf.length() == 0)                      //No input.
					{
						System.out.println("No input given. File was not modified.");
						return;
					}
					
					System.out.println("Enter \'y\' to add more data to file, or any other key to enter a different command.");
					if(readChars.hasNextLine())                       //Read input to determine if more data will be appended.
						app = readChars.nextLine();
				}
				
				//Now append the contents of the StringBuffer to the file. 
				int result = fileSys.tfs_append(path, buf.toString());           
				
				if (result >= 0)
					System.out.println("\nWrite finished.\n");
				else if (result == -2)                               //File not found.
				{
					System.out.println("Error. File not found in parent directory\n");
					return;
				}
				else                                                //General read or write error.
				{
					System.out.println("Error. Could not write to file.");
					return;
				}
																
				return;
			}
			
			
			/**
			 *  Command "cp /fullPath/Source_File /fullpath/Destination_File" - |Copies
	         *  the contents of one file to a new file in a destination directory.
	         *  Checks if the Source_File exists and Destination_File does NOT exist.
			 */
			
			else if( in.matches("cp "+ PATH_REGEX + " " + PATH_REGEX) )
			{
				String [] paths = in.split(" ");          //Split input input into 3 String, to get the 2 paths.
				int result = fileSys.tfs_cp(paths[1], paths[2]);
				System.out.println("|"+paths[1]+"|");
				System.out.println("|"+paths[2]+"|");
				if (result == 0)
					System.out.println("Copy completed.\n");                   //Success
				
				else if (result == -2)
					System.out.println("Source file cannot be found.\n");     //Source file doesn't exist.
				
				else if (result == -3)											//Destination file already exists.
					System.out.println("File with target name already exists in parent directory.\n");     
				else
					System.out.println("Error. Source file could not be copied to destination");  //General error.
				
				return;
			}
			
			
			/**
			 * Command "rename /fullpath/fileName newName" - |Renames the file, if the
			 * file exists, and some other file with the new name doesn't already exist 
			 * in the file's directory.\n
			 * 
			 */
			
			else if( in.matches("rename " + PATH_REGEX + " \\w+") )
			{
				String[] cm = in.split(" ");    //Split by space delimeter.
				
				int result = fileSys.tfs_rename(cm[1], cm[2]);
				
				if (result == 0)
					System.out.println("File renamed.\n");   //Success
				else if (result == -2)                       //File with name already exists.
					System.out.println("Could not rename file. A file with the specified name already exists in "
							+ "the file's directory.\n");
				
				else                                        //General error.
					System.out.println("Error. Could not rename file\n");
				return;
			}
			
			
			 //If nothing matches, command must be invalid.
			System.out.println("Invalid command. Please try again.\n");  
		
				
	}  
	
}
