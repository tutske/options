package org.tutske.lib.options;

public class DuplicateOptionException extends OptionException {

	private static final String FORMAT = "Duplicate option `%s` found.";

	private Option option;

	public DuplicateOptionException () {
	}

	public DuplicateOptionException (Option option) {
		this (formatMessage (option));
		this.option = option;
	}

	public DuplicateOptionException (String message) {
		super (message);
	}

	public DuplicateOptionException (String message, Throwable cause) {
		super (message, cause);
	}

	public DuplicateOptionException (Throwable cause) {
		super (cause);
	}

	private static String formatMessage (Option option) {
		return String.format (FORMAT, option.getName ());
	}

}
