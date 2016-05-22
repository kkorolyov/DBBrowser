package dev.kkorolyov.sqlob.cli;

import java.sql.SQLException;

import dev.kkorolyov.simpleopts.ArgParser;
import dev.kkorolyov.simpleopts.Option;
import dev.kkorolyov.simpleopts.Options;
import dev.kkorolyov.sqlob.assets.Assets;
import dev.kkorolyov.sqlob.connection.DatabaseConnection;

/**
 * Command line interface for a database.
 */
public class DatabaseCLI {
	private final Option	OPTION_HELP = new Option("h", "help", "Prints this help message", false),
												OPTION_LIST = new Option("l", "list", "Lists all tables in the database", false),
												OPTION_CONNECT = new Option("c", "connect", "Connects to a table in the database", true);
	
	private DatabaseConnection dbConn;
	private ArgParser parser;

	/**
	 * Launches the command line interface for a database.
	 * @param args arguments
	 */
	public DatabaseCLI(String[] args) {
		if (Assets.init()) {
			System.out.println(	"Initial application run detected" + System.lineSeparator()
												+ "Please set appropriate properties in '" + Assets.PROPERTIES_NAME + "', then re-run the application");
			
			System.exit(0);
		}
		try {
			setDatabaseConnection();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			
			System.exit(1);
		}
		try {
			setArgParser(args);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.out.println(	"Valid Options:" + System.lineSeparator()
												+ buildOptions());
			
			System.exit(1);
		}
		try {
			parse();
		} catch(Exception e) {
			System.err.println(e.getMessage());
			
			System.exit(1);
		}
	}
	private void setDatabaseConnection() throws SQLException {
		String 	host = Assets.hostname(),
				database = Assets.database(),
				user = Assets.user(),
				password = Assets.password();

		dbConn = new DatabaseConnection(host, database, user, password);
	}
	private void setArgParser(String[] args) {
		parser = new ArgParser(buildOptions(), args);
	}
	private Options buildOptions() {
		Option[] validOptions = {	OPTION_HELP,
															OPTION_LIST,
															OPTION_CONNECT};
		
		return new Options(validOptions);
	}
	
	private void parse() {
		if (parser.parsedOption(OPTION_HELP))
			help();
		else if (parser.parsedOption(OPTION_LIST))
			list();
		else if (parser.parsedOption(OPTION_CONNECT))
			connect(parser.getArg(OPTION_CONNECT));
	}
	private void help() {
		System.out.println(parser.getValidOptions());
	}
	private void list() {
		System.out.println(	"Available Tables in Database: " + dbConn.getDatabaseName());
		
		for (String table : dbConn.getTables())
			System.out.println(table);
	}
	private void connect(String connectTo) {
		new TableCLI(dbConn.connect(connectTo));
	}
}
