package org.tutske.options;


import java.util.List;


@FunctionalInterface
public interface StoreChangeConsumer<T> {

	@FunctionalInterface
	public static interface Multi<T> {
		public void onValue (OptionStore store, Option<T> option, List<T> value);
	}

	@FunctionalInterface
	public static interface Value<T> {
		public void onValue (T value);
	}

	@FunctionalInterface
	public static interface ValueList<T> {
		public void onValues (List<T> value);
	}

	public void onValue (OptionStore store, Option<T> option, T value);

}
