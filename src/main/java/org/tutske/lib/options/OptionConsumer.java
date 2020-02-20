package org.tutske.lib.options;

import java.util.List;


public interface OptionConsumer {

	public <T> void accept (Option<T> option, List<T> values) throws Exception;

}
