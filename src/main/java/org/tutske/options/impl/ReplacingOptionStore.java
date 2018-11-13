package org.tutske.options.impl;

import org.tutske.options.DynamicOption;
import org.tutske.options.Option;
import org.tutske.options.OptionSource;
import org.tutske.options.OptionStore;
import org.tutske.options.StoreChangeConsumer;
import org.tutske.options.UnknownOptionException;
import org.tutske.utils.Exceptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ReplacingOptionStore implements OptionStore {

	private final ExecutorService executor;
	private final Map<Option, Set<StoreChangeConsumer>> listeners = new HashMap<> ();
	private final Map<Option, Set<StoreChangeConsumer.Multi>> multiListeners = new HashMap<> ();
	private final Set<Option> options = new HashSet<> ();
	private final OptionBag bag = new OptionBag ();

	public ReplacingOptionStore (Option ... options) {
		this (Executors.newSingleThreadExecutor (), options);
	}

	public ReplacingOptionStore (ExecutorService executor, Option ... options) {
		Collections.addAll (this.options, options);
		this.executor = executor;
	}

	public ReplacingOptionStore (List<Option> options) {
		this (Executors.newSingleThreadExecutor (), options);
	}

	public ReplacingOptionStore (ExecutorService executor,  List<Option> options) {
		this.options.addAll (options);
		this.executor = executor;
	}

	@Override
	public void bind (OptionSource source) {
		try { source.subscribe ((List) options (), this::assign); }
		catch ( Exception e ) { throw Exceptions.wrap (e); }
	}

	private <T> void assign (Option<T> option, List<T> values) {
		assureKnown (option);
		bag.remove (option);
		bag.put (option, values.toArray ());

		if ( listeners.containsKey (option) ) {
			for ( StoreChangeConsumer consumer : listeners.get (option) ) {
				executor.submit (() -> consumer.onValue (this, option, bag.get (option)));
			}
		}

		if ( multiListeners.containsKey (option) ) {
			for ( StoreChangeConsumer.Multi consumer : multiListeners.get (option) ) {
				executor.submit (() -> consumer.onValue (this, option, bag.getAll (option)));
			}
		}
	}

	@Override
	public List<Option<?>> options () {
		return new LinkedList<Option<?>> ((Set) options);
	}

	@Override
	public boolean has (Option<?> option) {
		return bag.containsKey (option);
	}

	@Override
	public <T> T get (Option<T> option) {
		assureKnown (option);
		return bag.get (option);
	}

	@Override
	public <T> List<T> getAll (Option<T> option) {
		assureKnown (option);
		return bag.getAll (option);
	}

	@Override
	public <T> void onChange (Option<T> option, StoreChangeConsumer<T> consumer) {
		assureKnown (option);
		this.listeners.computeIfAbsent (option, key -> new HashSet<> ()).add (consumer);
		if ( bag.containsKey (option) ) {
			executor.submit (() -> consumer.onValue (this, option, bag.get (option)));
		}
	}

	@Override
	public <T> void onChanges (Option<T> option, StoreChangeConsumer.Multi<T> consumer) {
		assureKnown (option);
		this.multiListeners.computeIfAbsent (option, key -> new HashSet<> ()).add (consumer);
		if ( bag.containsKey (option) ) {
			executor.submit (() -> consumer.onValue (this, option, bag.getAll (option)));
		}
	}

	@Override
	public <T> DynamicOption<T> dynamic (Option<T> option) {
		return consumer -> onChange (option, consumer);
	}

	@Override
	public <T> DynamicOption.Value<T> dynamicValue (Option<T> option) {
		return consumer -> onValue (option, consumer);
	}

	private void assureKnown (Option<?> option) {
		if ( ! options.contains (option) ) {
			throw new UnknownOptionException (option);
		}
	}

}
