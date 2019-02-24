package org.tutske.lib.options;

public class OptionException extends RuntimeException {

	public OptionException () {}
	public OptionException (String message) { super (message); }
	public OptionException (Throwable cause) { super (cause); }
	public OptionException (String message, Throwable cause) { super (message, cause); }

}
