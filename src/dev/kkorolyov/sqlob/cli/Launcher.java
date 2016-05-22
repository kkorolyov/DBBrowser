package dev.kkorolyov.sqlob.cli;

import dev.kkorolyov.simpleopts.ArgParser;
import dev.kkorolyov.simpleopts.Option;
import dev.kkorolyov.simpleopts.Options;
import dev.kkorolyov.sqlob.connection.DatabaseConnection;

/**
 * Launches the application.
 */
public class Launcher {
	private static final Option OPTION_HELP = new Option(null, "help", "Prints this help message", false),
															OPTION_HOST = new Option("h", "host", "IP address or hostname of the database host", true),
															OPTION_DATABASE = new Option("d", "database", "Database name", true),
															OPTION_USER = new Option("u", "user", "Database user name", true),
															OPTION_PASSWORD = new Option("p", "password", "Database user password", true);
	private static final String DEFAULT_USER = "postgres",
															DEFAULT_PASSWORD = "";
	
	private static DatabaseConnection dbConn;

	/**
	 * Main entry point of the application
	 * @param args optional arguments
	 */
	public static void main(String[] args) {
		setUp(args);
	}
	
	private static void setUp(String[] args) {
		try {
			ArgParser parser = new ArgParser(buildOptions(), args);
			
			if (args.length < 1 || parser.parsedOption(OPTION_HELP)) {
				System.out.println(parser.getValidOptions());
				
				System.exit(0);
			}
			// DEBUG
			for (Option option : parser.getParsedOptions()) {
				System.out.println(option.getLongName() + "=" + parser.getArg(option));
			}
			String 	host = parser.getArg(OPTION_HOST),
							database = parser.getArg(OPTION_DATABASE),
							user = parser.parsedOption(OPTION_USER) ? parser.getArg(OPTION_USER) : DEFAULT_USER,
							password = parser.parsedOption(OPTION_PASSWORD) ? parser.getArg(OPTION_PASSWORD) : DEFAULT_PASSWORD;
			
			dbConn = new DatabaseConnection(host, database, user, password);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			
			System.exit(1);
		}
	}
	private static Options buildOptions() {
		Option[] validOptions = {	OPTION_HELP,
															OPTION_HOST,
															OPTION_DATABASE,
															OPTION_USER,
															OPTION_PASSWORD};
		
		return new Options(validOptions);
	}
}
