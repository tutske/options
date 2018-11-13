package org.tutske.options;


@FunctionalInterface
public interface DynamicOption<T> {

	public static interface Value<T> {
		public void onValue (StoreChangeConsumer.Value<T> consumer);
	}

	public void onValue (StoreChangeConsumer<T> consumer);

}
