package org.tutske.options;

import org.tutske.utils.PrimitivesParser;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class Option<T> {

	public static abstract class BaseOption<T> extends Option<T> {
		protected final String name;
		protected final T fallback;
		protected final Function<String, T> converter;

		protected BaseOption (String name, T fallback, Function<String, T> converter) {
			this.name = name;
			this.fallback = fallback;
			this.converter = converter;
		}

		@Override public String getName () { return this.name; }
		@Override public T parseValue (String value) { return converter.apply (value); }
		@Override public T getDefault () { return fallback; }
	}

	public static class EnumOption<T extends Enum<T>> extends BaseOption<T> {
		private final Enum<T> [] values;
		public EnumOption (String name, Enum<T> [] values, T fallback) {
			super (name, fallback, buildConverter (name, values));
			this.values = values;
		}
		public EnumOption (String name, Enum<T> [] values) {
			this (name, values, null);
		}
		private static <T extends Enum<T>> Function<String, T> buildConverter (String name, Enum<T> [] values) {
			return (value) -> {
				value = value == null ? "" : value.toLowerCase ();

				for ( Enum<T> attempt : values ) {
					if ( attempt.name ().toLowerCase ().equals (value) ) {
						return (T) attempt;
					}
				}

				String fmt = "`%s` is not an acceptable value for option '%s', possible values are %s";
				throw new RuntimeException (String.format (fmt, value, name, Arrays.toString (values)));
			};
		}
	}

	public static class StringOption extends BaseOption<String> {
		public StringOption (String name, String fallback) {
			super (name, fallback, Function.identity ());
		}
		public StringOption (String name) {
			this (name, null);
		}
	}

	public static class BooleanOption extends BaseOption<Boolean> {
		public BooleanOption (String name, Boolean fallback) {
			super (name, fallback, BooleanOption::parseBoolean);
		}
		public BooleanOption (String name) {
			this (name, null);
		}
		private static boolean parseBoolean (String value) {
			if ( "".equals (value) || "yes".equals (value) || "on".equals (value) ) {
				return true;
			}
			if ( "no".equals (value) || "off".equals (value) ) {
				return false;
			}
			return Boolean.parseBoolean (value);
		}
	}

	public static class IntegerOption extends BaseOption<Integer> {
		public IntegerOption (String name, Integer fallback) {
			super (name, fallback, PrimitivesParser.getParser (String.class, Integer.class));
		}
		public IntegerOption (String name) {
			this (name, null);
		}
	}

	public static class LongOption extends BaseOption<Long> {
		public LongOption (String name, Long fallback) {
			super (name, fallback, PrimitivesParser.getParser (String.class, Long.class));
		}
		public LongOption (String name) {
			this (name, null);
		}
	}

	public static class FloatOption extends BaseOption<Float> {
		public FloatOption (String name, Float fallback) {
			super (name, fallback, PrimitivesParser.getParser (String.class, Float.class));
		}
		public FloatOption (String name) {
			this (name, null);
		}
	}

	public static class DoubleOption extends BaseOption<Double> {
		public DoubleOption (String name, Double fallback) {
			super (name, fallback, PrimitivesParser.getParser (String.class, Double.class));
		}
		public DoubleOption (String name) {
			this (name, null);
		}
	}

	public static class PathOption extends BaseOption<Path> {
		public PathOption (String name, Path fallback) {
			super (name, fallback, PrimitivesParser.getParser (String.class, Path.class));
		}
		public PathOption (String name, String fallback) {
			this (name, Paths.get (fallback));
		}
		public PathOption (String name) {
			this (name, (Path) null);
		}
	}

	public static class UriOption extends BaseOption<URI> {
		public UriOption (String name, URI fallback) {
			super (name, fallback, PrimitivesParser.getParser (String.class, URI.class));
		}
		public UriOption (String name, String fallback) {
			this (name, PrimitivesParser.parse (fallback, URI.class));
		}
		public UriOption (String name) {
			this (name, (URI) null);
		}
	}

	public static class DurationOption extends BaseOption<Duration> {
		public DurationOption (String name, Duration fallback) {
			super (name, fallback, DurationOption::parse);
		}

		public DurationOption (String name, String fallback) {
			this (name, DurationOption.parse (fallback));
		}

		public DurationOption (String name) {
			this (name, (Duration) null);
		}

		private static final Pattern pattern = Pattern.compile (
			"(\\d+)(ns|ms|s|seconds?|m|minutes?|h|hours?|d|days?|w|weeks?|y|years?)"
		);

		private static Duration parse (String representation) {
			Duration duration = Duration.ZERO;
			for ( String part : representation.split (" ") ) {
				duration = duration.plus (parseSingle (part.toLowerCase ()));
			}
			return duration;
		}

		private static Duration parseSingle (String representation) {
			if ( representation.contains (":") ) {
				String [] parts = representation.split (":");

				if ( parts.length == 2 ) {
					return Duration
						.ofHours (Integer.parseInt (parts[0]))
						.plusMinutes (Integer.parseInt (parts[1]));
				} else if ( parts.length == 3 ) {
					return Duration
						.ofHours (Integer.parseInt (parts[0]))
						.plusMinutes (Integer.parseInt (parts[1]))
						.plusSeconds (Integer.parseInt (parts[2]));
				} else {
					throw new RuntimeException (
						"Wrong number of parts: '" + representation + "'"
					);
				}
			}

			Matcher matcher = pattern.matcher (representation);
			if ( matcher.matches () ) {
				long amount = Long.parseLong (matcher.group (1));
				long nanos = toNanos (amount, matcher.group (2));
				return Duration.ofNanos (nanos);
			}

			throw new RuntimeException ("Could not covert part: '" + representation + "'");
		}

		private static long toNanos (long amount, String s) {
			switch ( s ) {
				case "ns": return TimeUnit.NANOSECONDS.toNanos (amount);
				case "ms": return TimeUnit.MILLISECONDS.toNanos (amount);
				case "s": case "second": case "seconds": return TimeUnit.SECONDS.toNanos (amount);
				case "m": case "minute": case "minutes": return TimeUnit.MINUTES.toNanos (amount);
				case "h": case "hour": case "hours": return TimeUnit.HOURS.toNanos (amount);
				case "d": case "day": case "days": return TimeUnit.DAYS.toNanos (amount);
				case "w": case "week": case "weeks": return TimeUnit.DAYS.toNanos (amount * 7);
				case "y": case "year": case "years": return TimeUnit.DAYS.toNanos (amount * 365);
				default: throw new RuntimeException ("Invalid duration unit: " + s);
			}
		}
	}

	public abstract T parseValue (String value);
	public abstract String getName ();
	public abstract T getDefault ();

}
