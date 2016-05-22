package dev.kkorolyov.sqlob;

import dev.kkorolyov.sqlob.cli.DatabaseCLI;

/**
 * Launches the application.
 */
public class Launcher {
	/**
	 * Main entry point of application.
	 * @param args application arguments
	 */
	public static void main(String[] args) {
		if (args.length > 0)
			new DatabaseCLI(args);
	}
}
