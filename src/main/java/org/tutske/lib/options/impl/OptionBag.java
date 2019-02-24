package org.tutske.lib.options.impl;

import org.tutske.lib.options.Option;
import org.tutske.lib.utils.Bag;

import java.util.List;


public class OptionBag extends Bag {

	public <T> T get (Option<T> key) {
		return (T) super.get (key);
	}

	public <T> List<T> getAll (Option<T> key) {
		return (List) super.getAll (key);
	}

}
