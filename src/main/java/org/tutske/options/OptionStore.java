package org.tutske.options;

import java.util.List;


public interface OptionStore {

	public void bind (OptionSource source);

	public List<Option<?>> options ();

	public boolean has (Option<?> option);
	public <T> T get (Option<T> option);
	public <T> List<T> getAll (Option<T> option);

	default public <T> void onValue (Option<T> option, StoreChangeConsumer.Value<T> consumer) {
		onChange (option, (store, opt, value) -> consumer.onValue (value));
	}
	default public <T> void onValues (Option<T> option, StoreChangeConsumer.ValueList<T> consumer) {
		onChanges (option, (store, opt, values) -> consumer.onValues (values));
	}

	public <T> void onChange (Option<T> option, StoreChangeConsumer<T> consumer);
	public <T> void onChanges (Option<T> option, StoreChangeConsumer.Multi<T> consumer);

	public <T> DynamicOption<T> dynamic (Option<T> option);
	public <T> DynamicOption.Value<T> dynamicValue (Option<T> option);

}
