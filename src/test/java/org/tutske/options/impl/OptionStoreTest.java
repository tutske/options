package org.tutske.options.impl;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.tutske.options.Option;
import org.tutske.options.Option.*;
import org.tutske.options.OptionStore;
import org.tutske.options.OptionStoreFactory;
import org.tutske.options.SimpleOptionSource;
import org.tutske.options.StoreChangeConsumer;

import java.util.Arrays;
import java.util.List;


public class OptionStoreTest {

	IntegerOption count = new IntegerOption ("count", 0);

	Option [] options = new Option [] { count };

	@Test
	public void it_should_know_options_from_a_source () {
		OptionStore store = OptionStoreFactory.createNew (options, new SimpleOptionSource (
			consumer -> consumer.accept (count, Arrays.asList (9, 8))
		));

		assertThat (store.get (count), is (9));
	}

	@Test
	public void it_should_get_all_the_values_of_an_option () {
		OptionStore store = OptionStoreFactory.createNew (options, new SimpleOptionSource (
			consumer -> consumer.accept (count, Arrays.asList (9, 8))
		));

		assertThat (store.getAll (count), hasItems (9, 8));
	}

	@Test
	public void it_should_get_a_value_when_just_registered () {
		OptionStore store = OptionStoreFactory.createNew (options, new SimpleOptionSource (
			consumer -> consumer.accept (count, Arrays.asList (9, 8))
		));

		StoreChangeConsumer.Value<Integer> consumer = mock (StoreChangeConsumer.Value.class);
		store.onValue (count, consumer);

		verify (consumer, timeout (1000)).onValue (9);
	}

	@Test
	public void it_should_get_all_values_when_just_registered () {
		OptionStore store = OptionStoreFactory.createNew (options, new SimpleOptionSource (
			consumer -> consumer.accept (count, Arrays.asList (9, 8))
		));

		StoreChangeConsumer.ValueList<Integer> consumer = mock (StoreChangeConsumer.ValueList.class);
		ArgumentCaptor<List<Integer>> captor = (ArgumentCaptor) ArgumentCaptor.forClass (List.class);

		store.onValues (count, consumer);

		verify (consumer, timeout (1000)).onValues (captor.capture ());
		assertThat (captor.getValue (), hasItems (9, 8));
	}

	@Test
	public void it_should_get_values_when_recorded_after_listening () {
		SimpleOptionSource source = new SimpleOptionSource (consumer -> {});
		OptionStore store = OptionStoreFactory.createNew (options, source);

		StoreChangeConsumer.ValueList<Integer> consumer = mock (StoreChangeConsumer.ValueList.class);
		ArgumentCaptor<List<Integer>> captor = (ArgumentCaptor) ArgumentCaptor.forClass (List.class);

		store.onValues (count, consumer);
		source.source (count, 9, 8);

		verify (consumer, timeout (1000)).onValues (captor.capture ());
		assertThat (captor.getValue (), hasItems (9, 8));
	}

	@Test
	public void it_should_get_a_value_when_recorded_after_listening () {
		SimpleOptionSource source = new SimpleOptionSource (consumer -> {});
		OptionStore store = OptionStoreFactory.createNew (options, source);

		StoreChangeConsumer.Value<Integer> consumer = mock (StoreChangeConsumer.Value.class);
		store.onValue (count, consumer);

		source.source (count, 9);

		verify (consumer, timeout (1000)).onValue (9);
	}

	@Test
	public void it_should_know_when_an_option_has_a_value () {
		OptionStore store = OptionStoreFactory.createNew (options, new SimpleOptionSource (
			consumer -> consumer.accept (count, Arrays.asList (9, 8))
		));

		assertThat (store.has (count), is (true));
	}

	@Test
	public void it_should_know_it_has_an_option_that_was_sourced_later () {
		SimpleOptionSource source = new SimpleOptionSource (consumer -> {});
		OptionStore store = OptionStoreFactory.createNew (options, source);

		assertThat (store.has (count), is (false));

		source.source (count, 9);

		assertThat (store.has (count), is (true));
	}

	@Test
	public void it_should_know_all_the_options_that_it_was_created_with () {
		StringOption name = new StringOption ("name");
		IntegerOption age = new IntegerOption ("age");
		OptionStore store = OptionStoreFactory.createNew (name, age);

		assertThat (store.options (), hasItems (name, age));
		assertThat (store.options (), not (hasItems (count)));
	}

	@Test
	public void it_should_provide_dynamic_value_of_an_options () {
		OptionStore store = OptionStoreFactory.createNew (options, new SimpleOptionSource (
			consumer -> consumer.accept (count, Arrays.asList (9, 8))
		));

		StoreChangeConsumer.Value<Integer> consumer = mock (StoreChangeConsumer.Value.class);
		store.dynamicValue (count).onValue (consumer);

		verify (consumer, timeout (1000)).onValue (9);
	}

	@Test
	public void it_should_provide_dynamic_options () {
		OptionStore store = OptionStoreFactory.createNew (options, new SimpleOptionSource (
			consumer -> consumer.accept (count, Arrays.asList (9, 8))
		));

		StoreChangeConsumer<Integer> consumer = mock (StoreChangeConsumer.class);
		store.dynamic (count).onValue (consumer);

		verify (consumer, timeout (1000)).onValue (store, count, 9);
	}

	@Test (expected = RuntimeException.class)
	public void it_should_complain_when_getting_the_value_of_an_unknown_option () {
		OptionStore store = OptionStoreFactory.createNew (options);
		store.get (new StringOption ("unknown"));
	}

	@Test (expected = RuntimeException.class)
	public void it_should_complain_when_getting_all_the_values_of_an_unknown_option () {
		OptionStore store = OptionStoreFactory.createNew (options);
		store.getAll (new StringOption ("unknonw"));
	}

	@Test (expected = RuntimeException.class)
	public void it_should_complain_when_listening_for_values_on_an_unknown_option () {
		OptionStore store = OptionStoreFactory.createNew (options);
		store.onValue (new StringOption ("unknonw"), value -> {});
	}

	@Test (expected = RuntimeException.class)
	public void it_should_complain_when_listening_for_a_list_of_values_on_an_unknown_option () {
		OptionStore store = OptionStoreFactory.createNew (options);
		store.onValues (new StringOption ("unknonw"), values -> {});
	}

	@Test (expected = RuntimeException.class)
	public void it_should_complain_when_a_source_sets_an_option_that_does_not_exist () {
		OptionStoreFactory.createNew (options, new SimpleOptionSource (
			consumer -> consumer.accept (new StringOption ("unknown"), Arrays.asList ("unknown"))
		));
	}

	@Test (expected = RuntimeException.class)
	public void should_complain_when_multiple_options_have_the_same_name () {
		OptionStoreFactory.createNew (
			new StringOption ("test"),
			new StringOption ("test")
		);
	}

	@Test (expected = RuntimeException.class)
	public void should_complain_when_multiple_options_have_the_same_name_with_source () {
		OptionStoreFactory.createNew (
			new Option [] { new StringOption ("test"), new StringOption ("test") },
			new SimpleOptionSource (consumer -> {})
		);
	}

	@Test
	public void it_should_still_set_other_values_when_a_listener_crashes () {
		SimpleOptionSource source = new SimpleOptionSource (consumer -> {});
		OptionStore store = OptionStoreFactory.createNew (options, source);

		StoreChangeConsumer.Value<Integer> first = mock (StoreChangeConsumer.Value.class);
		StoreChangeConsumer.Value<Integer> second = mock (StoreChangeConsumer.Value.class);

		doThrow (new RuntimeException ("crash first")).when (first).onValue (any ());
		doThrow (new RuntimeException ("crash second")).when (second).onValue (any ());

		store.onValue (count, first);
		store.onValue (count, second);

		source.source (count, 9);

		verify (first, timeout (1000)).onValue (9);
		verify (second, timeout (1000)).onValue (9);
	}

}
