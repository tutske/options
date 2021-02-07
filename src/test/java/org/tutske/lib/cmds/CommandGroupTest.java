package org.tutske.lib.cmds;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.tutske.lib.options.Option;
import org.tutske.lib.options.SimpleOptionSource;
import org.junit.jupiter.api.Test;

import java.util.Arrays;


public class CommandGroupTest {

	CommandGroup store = new CommandGroup ();
	CmdFunction fn = mock (CmdFunction.class);

	@Test
	public void it_should_recognize_a_command () throws Exception {
		store.register ("test", config -> config.fn (fn));
		store.run (new String [] { "test" });
		verify (fn).run (any (), any (), any ());
	}

	@Test
	public void it_should_recognize_an_explicit_configured_command () throws Exception {
		store.configure ("test").fn (fn);
		store.run (new String [] { "test" });
		verify (fn).run (any (), any (), any ());
	}

	@Test
	public void it_should_run_commands_directly () {
		Option<Integer> err = new Option.IntegerOption ("err");

		store.register (Command.get ("run"), cfg -> cfg
			.options (err)
			.fn ((cmd, opts, tail) -> opts.get (err))
		);

		int code = store.run (Command.get ("run"), new String [] { "--err=5" });
		assertThat (code, is (5));
	}

	@Test
	public void it_should_not_do_anything_when_running_an_unknown_command () {
		Object result = store.run (Command.get ("does-not-exist"), new String [] {});
		assertThat (result, nullValue ());
	}

	@Test
	public void it_should_propagate_exceptions_from_the_command_function () {
		store.configure (Command.GLOBAL).fn ((cmd, opts, tail) -> {
			throw new RuntimeException ("Intentionally fail");
		});
		assertThrows (Exception.class, () -> {
			store.run (new String [] {});
		});
	}

	@Test
	public void it_should_know_the_options_of_a_command () throws Exception {
		Option<String> name = new Option.StringOption ("name");
		store.configure (Command.GLOBAL).options (name).fn (fn);

		store.run (new String [] { "--name=John" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (Command.GLOBAL), captor.capture (), any ());
		CommandStore store = captor.getValue ();

		assertThat (store.options (), hasItem (name));
	}

	@Test
	public void it_should_recognize_options_for_a_command () throws Exception {
		Option<String> name = new Option.StringOption ("name");
		store.register ("test", config -> config.options (name).fn (fn));

		store.run (new String [] { "test", "--name=John" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);

		verify (fn).run (any (), captor.capture (), any ());
		assertThat (captor.getValue ().get (name), is ("John"));
	}

	@Test
	public void it_should_recognize_deep_sub_commands () throws Exception {
		Option<String> first = new Option.StringOption ("first");
		Option<String> second = new Option.StringOption ("second");
		Option<String> third = new Option.StringOption ("third");

		Command start = Command.create ("start");
		Command middle = Command.create ("middle");
		Command deep = Command.create ("deep");

		store.register (start, config -> config.options (first).subCommand (middle));
		store.register (middle, config -> config.options (second).subCommand (deep));
		store.register (deep, config -> config.options (third).fn (fn));

		store.run (new String [] { "start", "--first=one", "middle", "--second=two", "deep", "--third=three" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);

		verify (fn).run (eq (deep), captor.capture (), any ());
		assertThat (captor.getValue ().get (third), is ("three"));
	}

	@Test
	public void it_should_use_the_cmd_fn_from_parent_commands () throws Exception {
		Option<String> first = new Option.StringOption ("first");
		Option<String> second = new Option.StringOption ("second");
		Option<String> third = new Option.StringOption ("third");

		Command start = Command.create ("start");
		Command middle = Command.create ("middle");
		Command deep = Command.create ("deep");

		store.register (start, config -> config.options (first).subCommand (middle));
		store.register (middle, config -> config.options (second).subCommand (deep).fn (fn));
		store.register (deep, config -> config.options (third));

		store.run (new String [] { "start", "--first=one", "middle", "--second=two", "deep", "--third=three" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (deep), captor.capture (), any ());

		CommandStore store = captor.getValue ();
		assertThat (store.get (third), is ("three"));
	}

	@Test
	public void it_should_call_the_fn_registered_on_the_global_command () throws Exception {
		Command cmd = Command.create ("run");
		store.configure (Command.GLOBAL).fn (fn);
		store.register (cmd);

		store.run (new String [] { "run" });

		verify (fn).run (eq (cmd), any (), any ());
	}

	@Test
	public void it_should_not_call_the_global_fn_when_a_sub_command_has_an_fn () throws Exception {
		store.configure (Command.GLOBAL).fn (fn).subCommand (Command.get ("run"));
		store.configure (Command.get ("run")).fn ((cmd, opts, tail) -> null);

		store.run (new String [] { "run" });

		verify (fn, times (0)).run (any (), any (), any ());
	}

	@Test
	public void it_should_provide_intermediate_parsed_options () throws Exception {
		Command cmd = Command.create ("run");
		Option<Boolean> verbose = new Option.BooleanOption ("verbose");
		Option<String> name = new Option.StringOption ("name");

		store.register (Command.GLOBAL, config -> config.options (verbose));
		store.register (cmd, fn, config -> config.options (name));

		store.run (new String [] { "--verbose", "run", "--name=John" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (cmd), captor.capture (), any ());

		assertThat (captor.getValue ().get (Command.GLOBAL, verbose), is (true));
	}

	@Test
	public void it_should_fail_when_adding_a_command_as_a_sub_twice () {
		Command start_one = Command.create ("start-one");
		Command start_two = Command.create ("start-two");
		Command sub = Command.create ("sub");

		store.register (start_one, config -> config.subCommand (sub));

		assertThrows (Exception.class, () -> {
			store.register (start_two, config -> config.subCommand (sub));
		});
	}

	@Test
	public void it_should_scan_all_options_when_specified () throws Exception {
		Command cmd = Command.create ("run");
		Option<Boolean> verbose = new Option.BooleanOption ("verbose");
		Option<String> name = new Option.StringOption ("name");

		store.register (cmd, fn, config -> config.options (verbose, name).fullScan (true));

		store.run (new String [] { "run", "--verbose", "tail", "--name=John"});

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (cmd), captor.capture (), any ());

		assertThat (captor.getValue ().get (name), is ("John"));
	}

	@Test
	public void it_should_scan_till_the_end_for_final_commands_by_default () throws Exception {
		Command cmd = Command.create ("run");
		Option<Boolean> verbose = new Option.BooleanOption ("verbose");
		Option<String> name = new Option.StringOption ("name");

		store.register (cmd, fn, config -> config.options (verbose, name));

		store.run (new String [] { "run", "--verbose", "tail", "--name=John"});

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (cmd), captor.capture (), any ());

		assertThat (captor.getValue ().get (name), is ("John"));
	}

	@Test
	public void it_should_not_scan_till_the_end_when_prevented () throws Exception {
		Command cmd = Command.create ("run");
		Option<Boolean> verbose = new Option.BooleanOption ("verbose");
		Option<String> name = new Option.StringOption ("name");

		store.register (cmd, fn, config -> config.options (verbose, name).fullScan (false));

		store.run (new String [] { "run", "--verbose", "tail", "--name=John"});

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (cmd), captor.capture (), any ());

		assertThat (captor.getValue ().has (name), is (false));
	}

	@Test
	public void it_should_complain_when_runing_without_a_registered_handler () {
		store.register ("start").register ("middle").register ("deep");
		assertThrows (Exception.class, () -> {
			store.run (new String[] { "start", "middle", "deep" });
		});
	}

	@Test
	public void it_should_run_parent_fn_when_no_sub_command_matches () throws Exception {
		Command cmd = Command.create ("run");

		store.register (Command.GLOBAL, fn, config -> {});
		store.register (cmd, (c, o, t) -> { throw new RuntimeException (); }, config -> {});

		store.run (new String [] { "not-run", "tail" });

		verify (fn).run (eq (Command.GLOBAL), any (), eq (new String [] { "not-run", "tail" }));
	}

	@Test
	public void it_should_run_parent_fn_when_no_deep_sub_command_matches () throws Exception {
		Command top = Command.create ("top");
		Command cmd = Command.create ("run");

		store.register (top, fn, config -> {});
		store.register (cmd, (c, o, t) -> { throw new RuntimeException (); }, config -> {});

		store.run (new String [] { "top", "not-run", "tail" });

		verify (fn).run (eq (top), any (), eq (new String [] { "not-run", "tail" }));
	}

	@Test
	public void it_should_allow_configuring_the_option_store_for_a_command () throws Exception {
		Option<Boolean> verbose = new Option.BooleanOption ("verbose");
		Option<String> name = new Option.StringOption ("name");

		store.register (Command.get ("run"), fn, config -> {
			config.options (verbose, name);
			config.configureStore (store -> store.bind (SimpleOptionSource.source (consumer -> {
				consumer.accept (name, Arrays.asList ("John"));
			})));
		});

		store.run (new String [] { "run", "--verbose" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (Command.get ("run")), captor.capture (), any ());

		assertThat (captor.getValue ().get (name), is ("John"));
	}

	@Test
	public void it_should_have_the_default_versions () throws Exception {
		Option<Boolean> verbose = new Option.BooleanOption ("verbose");
		Option<String> name = new Option.StringOption ("name", "John");

		store.register ("run", fn, config -> config.options (verbose, name));

		store.run (new String [] { "run", "--verbose" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (Command.get ("run")), captor.capture (), any ());

		assertThat (captor.getValue ().get (name), is ("John"));
	}

	@Test
	public void it_should_have_the_default_versions_for_parent_commands () throws Exception {
		Option<String> db = new Option.StringOption ("db", "mysql://sub.domain.org/names");
		Option<Boolean> verbose = new Option.BooleanOption ("verbose");
		Option<String> name = new Option.StringOption ("name");

		store.register ("run", config -> config.options (db, verbose).subCommand (Command.get ("rename")));
		store.register ("rename", fn, config -> config.options (name));

		store.run (new String [] { "run", "rename", "--name=John" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (Command.get ("rename")), captor.capture (), any ());

		assertThat (captor.getValue ().get (Command.get ("run"), db), is ("mysql://sub.domain.org/names"));
	}

	@Test
	public void it_should_not_need_explicit_register_of_sub_commands () throws Exception {
		Command top = Command.create ("top");
		Command middle = Command.create ("middle");
		Command leaf = Command.create ("leaf");

		store.register (top, config -> config.subCommand (middle));
		store.register (middle, config -> config.subCommand (leaf));
		store.configure (Command.GLOBAL).fn (fn);

		store.run (new String [] { "top", "middle", "leaf" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (leaf), captor.capture (), any ());
		assertThat (captor.getValue ().commands (), contains (Command.GLOBAL, top, middle, leaf));
	}

	@Test
	public void it_should_not_allow_running_a_sub_command_directly () throws Exception {
		Command top = Command.create ("top");
		Command middle = Command.create ("middle");
		Command leaf = Command.create ("leaf");

		store.register (top, config -> config.subCommand (middle));
		store.register (middle, config -> config.subCommand (leaf));
		store.register (leaf);

		store.configure (Command.GLOBAL).fn (fn);

		store.run (new String [] { "middle", "leaf" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (Command.GLOBAL), captor.capture (), argThat (arrayContaining ("middle", "leaf")));
		assertThat (captor.getValue ().commands (), contains (Command.GLOBAL));
	}

	@Test
	public void it_should_find_options_from_higher_commands () throws Exception {
		Option<String> key = new Option.StringOption ("key");

		Command top = Command.create ("top");
		Command middle = Command.create ("middle");
		Command leaf = Command.create ("leaf");

		store.register (top, config -> config.subCommand (middle).registerOptions (key));
		store.register (middle, config -> config.subCommand (leaf));
		store.register (leaf);

		store.configure (Command.GLOBAL).fn (fn);

		store.run (new String [] { "top", "--key=value", "middle", "leaf" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (leaf), captor.capture (), any ());
		assertThat (captor.getValue ().find (key), is ("value"));
	}

	@Test
	public void it_should_find_options_from_higher_commands_that_do_full_scans () throws Exception {
		Option<String> key = new Option.StringOption ("key");

		Command top = Command.create ("top");
		Command middle = Command.create ("middle");
		Command leaf = Command.create ("leaf");

		store.register (top, config -> config.subCommand (middle).registerOptions (key).fullScan (true));
		store.register (middle, config -> config.subCommand (leaf));
		store.register (leaf);

		store.configure (Command.GLOBAL).fn (fn);

		store.run (new String [] { "top", "middle", "leaf", "--key=value" });

		ArgumentCaptor<CommandStore> captor = ArgumentCaptor.forClass (CommandStore.class);
		verify (fn).run (eq (leaf), captor.capture (), any ());
		assertThat (captor.getValue ().find (key), is ("value"));
	}

}
