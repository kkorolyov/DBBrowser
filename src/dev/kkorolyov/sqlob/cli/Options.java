package dev.kkorolyov.sqlob.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Translates arguments into program functions.
 */
public class Options {
	private static final Map<Option, String> defaults;
	
	private String[] args;
	private Map<Option, String> options;
	
	static {
		defaults = new HashMap<>();
		
		defaults.put(Option.USER, "postgres");
		defaults.put(Option.PASSWORD, "");
	}
	
	/**
	 * Constructs a new {@code Options} for the specified arguments.
	 * @param args arguments to use
	 */
	public Options(String[] args) {
		this.args = args;
		this.options = new HashMap<>();
		
		loadDefaults();
		parse();
	}
	private void loadDefaults() {
		options.putAll(defaults);
	}
	private void parse() {
		int counter = 0;
		while (counter < args.length) {
			Option currentOption = Option.getOption(args[counter]);
			
			if (currentOption.requiresArg()) {
				options.put(currentOption, args[counter + 1]);
				counter += 2;
			}
			else {
				options.put(currentOption, null);
				counter += 1;
			}
		}
	}
	
	/** @return	help string */
	public static String help() {
		StringBuilder toStringBuilder = new StringBuilder("USAGE" + System.lineSeparator());
		
		for (Option option : Option.values()) {
			toStringBuilder.append(option.description()).append(System.lineSeparator());
		}
		return toStringBuilder.toString();
	}
	
	/**
	 * Checks if this {@code Options} contains the specified option.
	 * @param toCheck option to check
	 * @return {@code true} if this {@code Options} contains the specified option
	 */
	public boolean contains(Option toCheck) {
		return options.containsKey(toCheck);
	}
	/**
	 * Retrieves the argument for a specific option.
	 * @param key option to get argument for
	 * @return option's argument, or {@code null} if does not exist
	 */
	public String get(Option key) {
		return options.get(key);
	}
	
	/** @return	all used options */
	public Option[] getAllOptions() {
		return options.keySet().toArray(new Option[options.size()]);
	}
	
	/**
	 * Possible options.
	 */
	@SuppressWarnings("javadoc")
	public static enum Option {
		HELP(null, "--help", false, "Prints this help message"),
		HOST("-h", "--host", true, "IP address or hostname of the database host"),
		DATABASE("-d", "--database", true, "Database name"),
		USER("-u", "--user", true, "User to read database"),
		PASSWORD("-p", "--password", true, "User's password");
		
		private String 	shortName,
										longName;
		private boolean requiresArg;
		private String description;
		
		private Option(String shortName, String longName, boolean requiresArg, String description) {
			this.shortName = shortName;
			this.longName = longName;
			this.requiresArg = requiresArg;
			this.description = description;
		}
		static Option getOption(String optionName) {			
			for (Option option : Option.values()) {
				if (option.matches(optionName))
					return option;
			}
			return null;
		}
		
		boolean requiresArg() {
			return requiresArg;
		}
		
		boolean matches(String optionName) {
			return Objects.equals(shortName, optionName) || Objects.equals(longName, optionName);
		}
		
		String description() {
			return (shortName == null ? "" : shortName + ",") + '\t' + longName + System.lineSeparator() + '\t' + description;
		}
	}
}
