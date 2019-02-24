package org.tutske.lib.options;


public class UnknownOptionException extends OptionException {

	private static final String FORMAT = "Option `%s` is not known by this store.";

	private Option option;

	public UnknownOptionException () {
	}

	public UnknownOptionException (Option option) {
		this (formatMessage (option));
		this.option = option;
	}

	public UnknownOptionException (String message) {
		super (message);
	}

	public UnknownOptionException (String message, Throwable cause) {
		super (message, cause);
	}

	public UnknownOptionException (Throwable cause) {
		super (cause);
	}

	private static String formatMessage (Option option) {
		return String.format (FORMAT, option.getName ());
	}

}
