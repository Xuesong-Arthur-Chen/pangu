package revoluttest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.dbcp2.BasicDataSource;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.ibatis.common.jdbc.ScriptRunner;
import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
public class Main {

	public static BasicDataSource ds = null;

	private static void setupTestDb() throws SQLException, IOException, ClassNotFoundException {
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		try (Connection conn = DriverManager
				.getConnection("jdbc:derby:revoluttest;create=true");
				Reader init_script = new InputStreamReader(
						Main.class.getResourceAsStream("/db_init.sql"));
				Reader test_data_script = new InputStreamReader(
						Main.class.getResourceAsStream("/db_test_data.sql"));) {
			ScriptRunner runner = new ScriptRunner(conn, false, false);
			runner.runScript(init_script);
			runner.runScript(test_data_script);
		}
	}

	private static void shutdownTestDb() {
		try {
			DriverManager.getConnection("jdbc:derby:revoluttest;shutdown=true");
		} catch (SQLException sqle) {
			// database shutdown properly
		}
	}

	private static BasicDataSource setupDataSource() {

		BasicDataSource ds = new BasicDataSource();
		// ds.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
		ds.setUrl("jdbc:derby:revoluttest");
		ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		ds.setDefaultAutoCommit(true);
		ds.setPoolPreparedStatements(true);

		return ds;
	}

	private static HttpServer startWebServer() {
		URI baseUri = UriBuilder.fromUri("http://localhost/").port(9997)
				.build();
		ResourceConfig config = new ResourceConfig(Api.class);
		return JdkHttpServerFactory.createHttpServer(baseUri, config);
	}

	public static void main(String[] args) {

		HttpServer server = null;

		try {
			System.out.println("Starting...\n");

			setupTestDb();
			ds = setupDataSource();
			server = startWebServer();

			System.out.println("\nStarted Successfully!\n");

			System.in.read();

		} catch (SQLException sqle) {
			printSQLException(sqle);
		} catch (IOException | ClassNotFoundException e) {
			System.err.println(e);
		} finally {
			System.out.println("\nshutting down...\n");

			if (server != null)
				server.stop(0);
			if (ds != null) {
				try {
					ds.close();
				} catch (SQLException sqle) {
					printSQLException(sqle);
				}
			}
			shutdownTestDb();

			System.out.println("\nshut down successfully\n");
		}

	}

	public static void printSQLException(SQLException e) {
		// Unwraps the entire exception chain to unveil the real cause of the
		// Exception.
		while (e != null) {
			System.err.println("\n----- SQLException -----");
			System.err.println("  SQL State:  " + e.getSQLState());
			System.err.println("  Error Code: " + e.getErrorCode());
			System.err.println("  Message:    " + e.getMessage());
			// for stack traces, refer to derby.log or uncomment this:
			// e.printStackTrace(System.err);
			e = e.getNextException();
		}
	}

}
