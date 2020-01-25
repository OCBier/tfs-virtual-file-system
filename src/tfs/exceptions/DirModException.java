package tfs.exceptions;


/**
 * DirModException.java
 * 
 * Class for representing exception which occurs when attempt is made
 * to modify a directory in an invalid way.
 * 
 * @author Oloff Biermann
 * @version 8.77
 */
public class DirModException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DirModException(String message)
	{
		super(message);
		
	}
	
			
}
