package org.tutske.options;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.tutske.options. Option.*;


public class Utils {

	public static List<Option> options (String ... names) {
		return Arrays.stream (names).map (StringOption::new).collect(Collectors.toList());
	}

	public static List<Option> options (Option<?> ... options) {
		return Arrays.asList (options);
	}

	public static OptionConsumer createConsumer () {
		return new OptionConsumer () {
			@Override public <T> void accept (Option<T> option, List<T> values) {
			}
		};
	}

}
