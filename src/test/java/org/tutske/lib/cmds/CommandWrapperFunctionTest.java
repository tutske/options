package org.tutske.lib.cmds;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.tutske.lib.options.Option;

import java.util.function.BiConsumer;


public class CommandWrapperFunctionTest {

	private Option<Boolean> verbose = new Option.BooleanOption ("verbose");
	private Option<String> name = new Option.StringOption ("name");
	private Option<Integer> age = new Option.IntegerOption ("age");

	CommandGroup store = new CommandGroup ();

	@Test
	public void it_should_run_some_method_before_the_sub_commands () throws Exception {
		BiConsumer<Command, String> fn = mock (BiConsumer.class);

		store.configure (Command.GLOBAL).before ((c, o, t) -> fn.accept (Command.GLOBAL, "before"));
		store.configure (Command.get ("run")).handle ((c, o, t) -> fn.accept (c, "exec"));

		store.run (new String [] { "run" });

		verify (fn).accept (Command.GLOBAL, "before");
		verify (fn).accept (Command.get ("run"), "exec");
	}

	@Test
	public void it_should_run_all_parrent_command_before_methods_in_order () throws Exception {
		BiConsumer<Command, String> fn = mock (BiConsumer.class);
		CommandGroup store = createCommand (fn);
		store.run (new String [] { "run", "sub", });

		InOrder ordered = inOrder (fn);

		ordered.verify (fn).accept (Command.GLOBAL, "before");
		ordered.verify (fn).accept (Command.get ("run"), "before");
		ordered.verify (fn).accept (Command.get ("sub"), "exec");
		ordered.verify (fn).accept (Command.get ("run"), "after");
		ordered.verify (fn).accept (Command.GLOBAL, "after");
	}

	@Test
	public void it_should_stop_executing_after_the_first_exception_in_a_before_method () {
		BiConsumer<Command, String> fn = mock (BiConsumer.class);
		CommandGroup store = createCommand (fn);

		RuntimeException failure = new RuntimeException ("intentionally fail");
		doThrow (failure).when (fn).accept (Command.GLOBAL, "before");

		Exception ex = assertThrows (Exception.class, () -> {
			store.run (new String [] { "run", "sub", });
		});

		assertThat (ex.getMessage (), containsString ("intentionally fail"));

		verify (fn).accept (Command.GLOBAL, "before");
		verify (fn, times (0)).accept (Command.get ("run"), "before");
		verify (fn, times (0)).accept (Command.get ("sub"), "before");
		verify (fn, times (0)).accept (Command.get ("sub"), "exec");
	}

	@Test
	public void it_should_propagate_exceptions_from_the_main_method_not_run_after_methods () {
		BiConsumer<Command, String> fn = mock (BiConsumer.class);
		CommandGroup store = createCommand (fn);

		RuntimeException failure = new RuntimeException ("intentionally fail");
		doThrow (failure).when (fn).accept (Command.get ("sub"), "exec");

		Exception ex = assertThrows (Exception.class, () -> {
			store.run (new String [] { "run", "sub", });
		});

		assertThat (ex.getMessage (), containsString ("intentionally fail"));

		verify (fn).accept (Command.GLOBAL, "before");
		verify (fn).accept (Command.get ("run"), "before");
		verify (fn).accept (Command.get ("sub"), "before");
		verify (fn).accept (Command.get ("sub"), "exec");
		verify (fn, times (0)).accept (Command.get ("sub"), "after");
		verify (fn, times (0)).accept (Command.get ("run"), "after");
		verify (fn, times (0)).accept (Command.GLOBAL, "after");
	}

	private CommandGroup createCommand (BiConsumer<Command, String> fn) {
		CommandGroup store = new CommandGroup ();

		store.configure (Command.GLOBAL)
			.before ((c, o, t) -> fn.accept (Command.GLOBAL, "before"))
			.after ((c, o, t) -> fn.accept (Command.GLOBAL, "after"))
			.subCommand (Command.get ("run"));
		;

		store.configure (Command.get ("run"))
			.before ((c, o, t) -> fn.accept (Command.get ("run"), "before"))
			.after ((c, o, t) -> fn.accept (Command.get ("run"), "after"))
			.subCommand (Command.get ("sub"))
		;

		store.configure (Command.get ("sub"))
			.before ((c, o, t) -> fn.accept (Command.get ("sub"), "before"))
			.after ((c, o, t) -> fn.accept (Command.get ("sub"), "after"))
			.handle ((c, o, t) -> fn.accept (Command.get ("sub"), "exec"))
		;

		return store;
	}
}
