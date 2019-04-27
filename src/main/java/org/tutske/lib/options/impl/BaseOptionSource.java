package org.tutske.lib.options.impl;

import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionConsumer;
import org.tutske.lib.options.OptionSource;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public abstract class BaseOptionSource implements OptionSource {

	protected final Map<OptionConsumer, List<Option>> listeners = new HashMap<> ();

	@Override
	public void subscribe (List<Option> options, OptionConsumer consumer) {
		List<Option> copy = new LinkedList<> (options);
		listeners.put (consumer, copy);
	}

	@Override
	public void unsubscribe (List<Option> options, OptionConsumer consumer) {
		if ( ! listeners.containsKey (consumer) ) { return; }

		List<Option> current = listeners.get (consumer);
		current.removeAll (options);

		if ( current.isEmpty () ) { listeners.remove (consumer); }
	}

}
