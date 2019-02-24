package org.tutske.lib.options.cmd;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.tutske.lib.options.Option.*;
import static org.tutske.lib.options.SimpleOptionSource.source;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.tutske.lib.options.Option;
import org.tutske.lib.options.OptionStore;
import org.tutske.lib.options.OptionStoreFactory;

import java.util.Arrays;


public class CommandStoreTest {

	private Option<Boolean> verbose = new BooleanOption ("verbose");
	private Option<String> name = new StringOption ("name");
	private Option<Integer> age = new IntegerOption ("age");

	private CommandStore store = new CommandStore ();

	@Test
	public void it_should_register_option_stores () {
		Command cmd = Command.create ("run");
		OptionStore opts = OptionStoreFactory.createNew ();

		store.addStore (cmd, opts);

		assertThat (store.optionStore (cmd), is (opts));
	}

	@Test (expected = Exception.class)
	public void it_should_complain_when_a_command_already_has_a_store_associated () {
		Command cmd = Command.create ("run");

		store.addStore (cmd, OptionStoreFactory.createNew ());
		store.addStore (cmd, OptionStoreFactory.createNew ());
	}

	@Test (expected = Exception.class)
	public void it_should_complain_when_asking_for_a_store_with_an_unknown_command () {
		Command cmd = Command.create ("run");
		store.optionStore (cmd);
	}

	@Test
	public void it_should_know_about_command_with_option_stores () {
		Command first = Command.create ("first");
		Command second = Command.create ("second");

		store.addStore (first, OptionStoreFactory.createNew ());
		store.addStore (second, OptionStoreFactory.createNew ());

		assertThat (store.commands (), Matchers.containsInAnyOrder (first, second));
	}

	@Test
	public void it_should_list_all_the_options_in_the_command_store () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John")))
		));

		assertThat (store.options (), containsInAnyOrder (verbose, name));
	}

	@Test
	public void it_should_list_options_for_a_single_command () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John")))
		));

		assertThat (store.options (Command.GLOBAL), contains (verbose));
		assertThat (store.options (Command.get ("run")), contains (name));
	}

	@Test
	public void it_should_know_when_any_of_the_stores_has_an_option () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John")))
		));

		assertThat (store.has (verbose), is (true));
		assertThat (store.has (name), is (true));
		assertThat (store.has (age), is (false));
	}

	/* -- getting single value -- */

	@Test
	public void it_should_get_the_value_of_an_option () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John")))
		));

		store.setMain (Command.get ("run"));

		assertThat (store.get (name), is ("John"));
	}

	@Test (expected = Exception.class)
	public void it_should_complain_when_getting_an_option_not_known_by_the_main_command () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John")))
		));

		store.setMain (Command.get ("run"));

		store.get (verbose);
	}

	@Test
	public void it_should_get_the_value_of_an_option_for_a_specific_command () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John")))
		));

		store.setMain (Command.get ("run"));

		assertThat (store.get (Command.GLOBAL, verbose), is (true));
	}

	@Test (expected = Exception.class)
	public void it_should_complain_when_getting_an_option_not_known_by_the_specified_command () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John")))
		));

		store.setMain (Command.get ("run"));

		store.get (Command.GLOBAL, name);
	}

	@Test
	public void it_should_find_the_value_of_options () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John")))
		));

		store.setMain (Command.get ("run"));

		assertThat (store.find (verbose), is (true));
		assertThat (store.find (name), is ("John"));
	}

	@Test (expected = Exception.class)
	public void it_should_complain_when_finding_the_values_of_an_option_that_is_not_known_by_any_associated_store () {
		store.addStore (Command.get ("run"), OptionStoreFactory.createNew (name));
		store.find (age);
	}

	/* -- getting all -- */

	@Test
	public void it_should_get_all_values_of_an_option () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true, false)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John", "Jane")))
		));

		store.setMain (Command.get ("run"));

		assertThat (store.getAll (name), contains ("John", "Jane"));
	}

	@Test (expected = Exception.class)
	public void it_should_complain_when_getting_all_values_for_an_option_not_known_by_the_main_command () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true, false)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John", "Jane")))
		));

		store.setMain (Command.get ("run"));

		store.getAll (verbose);
	}

	@Test
	public void it_should_get_all_values_of_an_option_for_a_specific_command () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true, false)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John", "Jane")))
		));

		store.setMain (Command.get ("run"));

		assertThat (store.getAll (Command.GLOBAL, verbose), contains (true, false));
	}

	@Test (expected = Exception.class)
	public void it_should_complain_when_getting_all_values_of_an_option_not_known_by_the_specified_command () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true, false)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John", "Jane")))
		));

		store.setMain (Command.get ("run"));

		store.getAll (Command.GLOBAL, name);
	}

	@Test
	public void it_should_find_all_values_of_options () {
		store.addStore (Command.GLOBAL, OptionStoreFactory.createNew (new Option [] { verbose }, source (
			consumer -> consumer.accept (verbose, Arrays.asList (true, false)))
		));

		store.addStore (Command.create ("run"), OptionStoreFactory.createNew (new Option [] { name }, source (
			consumer -> consumer.accept (name, Arrays.asList ("John", "Jane")))
		));

		store.setMain (Command.get ("run"));

		assertThat (store.findAll (verbose), contains (true, false));
		assertThat (store.findAll (name), contains ("John", "Jane"));
	}

	@Test (expected = Exception.class)
	public void it_should_complain_when_finding_all_values_of_an_option_that_is_not_known_by_any_associated_store () {
		store.addStore (Command.get ("run"), OptionStoreFactory.createNew (name));
		store.findAll (age);
	}

}
