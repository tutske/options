package org.tutske.options.sources;

import org.tutske.options.Option;
import org.tutske.options.OptionConsumer;
import org.tutske.options.OptionSource;
import org.tutske.utils.Exceptions;

import java.util.Collections;
import java.util.List;


public class DefaultsOptionSource implements OptionSource {

	@Override
	public void subscribe (List<Option> options, OptionConsumer consumer) {
		for ( Option option : options ) {
			Object value = option.getDefault ();
			if ( value == null ) { continue; }

			try { consumer.accept (option, Collections.singletonList (value)); }
			catch ( Exception e ) { throw Exceptions.wrap (e); }
		}
	}

	@Override
	public void unsubscribe (List<Option> options, OptionConsumer consumer) {
	}

}
