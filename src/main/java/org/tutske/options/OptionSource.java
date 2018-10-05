package org.tutske.options;

import java.util.List;


public interface OptionSource {

	public void subscribe (List<Option> options, OptionConsumer consumer);
	public void unsubscribe (List<Option> options, OptionConsumer consumer);

}
