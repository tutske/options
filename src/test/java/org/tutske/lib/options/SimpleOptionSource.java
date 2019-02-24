package org.tutske.lib.options;

import org.tutske.lib.utils.Exceptions;
import org.tutske.lib.utils.Functions.RiskyConsumer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;


public class SimpleOptionSource implements OptionSource {

	public static OptionSource source (RiskyConsumer<OptionConsumer> consumer) {
		return new SimpleOptionSource (consumer);
	}

	private final Consumer<OptionConsumer> populator;
	private final Set<OptionConsumer> consumers = new HashSet<> ();

	public SimpleOptionSource (RiskyConsumer<OptionConsumer> populator) {
		this.populator = populator;
	}

	public <T> void source (Option<T> option, T ... values) {
		for ( OptionConsumer consumer : consumers ) {
			try { consumer.accept (option, Arrays.asList (values)); }
			catch ( Exception e ) { throw Exceptions.wrap (e); }
		}
	}

	@Override public void subscribe (List<Option> options, OptionConsumer consumer) {
		this.consumers.add (consumer);
		populator.accept (consumer);
	}

	@Override public void unsubscribe (List<Option> options, OptionConsumer consumer) {
		this.consumers.remove (consumer);
	}
}
