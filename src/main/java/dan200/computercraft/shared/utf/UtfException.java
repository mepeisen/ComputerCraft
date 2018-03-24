/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.utf;

//Contributed by mepeisen

/**
 * Exception to be thrown on utf operations
 * @author mepeisen
 */
public class UtfException extends Exception
{

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = -242103694341279577L;

	public UtfException(String message, Throwable cause) {
		super(message, cause);
	}

	public UtfException(String message) {
		super(message);
	}

}
