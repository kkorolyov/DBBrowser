package dev.kkorolyov.sqlob;

import dev.kkorolyov.sqlob.cli.Options;
import dev.kkorolyov.sqlob.connection.DatabaseConnection;

/**
 * Launches the application.
 */
public class Launcher {

	/**
	 * Main entry point of the application
	 * @param args optional arguments
	 */
	public static void main(String[] args) {
		try {
			Options options = new Options(args);
			
			if (args.length < 1 || options.contains(Options.Option.HELP)) {
				printHelp();
				
				System.exit(0);
			}
			// DEBUG
			for (Options.Option option : options.getAllOptions()) {
				System.out.println(option + "=" + options.get(option));
			}
				
			String 	host = options.get(Options.Option.HOST),
							database = options.get(Options.Option.DATABASE),
							user = options.get(Options.Option.USER),
							password = options.get(Options.Option.PASSWORD);
			
			DatabaseConnection dbConn = new DatabaseConnection(host, database, user, password);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			
			System.exit(1);
		}
	}
	
	private static void printHelp() {
		System.out.println(Options.help());
	}
}
