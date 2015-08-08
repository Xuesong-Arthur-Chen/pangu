package pinetree.pangu;

import pinetree.pangu.rest.Api;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.dbcp2.BasicDataSource;
import org.glassfish.jersey.server.ResourceConfig;

import com.ibatis.common.jdbc.ScriptRunner;
import java.util.concurrent.ExecutionException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

public class Main {

    public static BasicDataSource dataSource = null;

    public static final String dbDriver = "org.hsqldb.jdbc.JDBCDriver";
    public static final String dbConnStr = "jdbc:hsqldb:mem:testdb;hsqldb.tx=mvcc";

    private static void setupTestDb() throws SQLException, IOException,
            ClassNotFoundException {
        Class.forName(dbDriver);
        try (Connection conn = DriverManager.getConnection(dbConnStr);
                Reader init_script = new InputStreamReader(
                        Main.class.getResourceAsStream("/db_init.sql"));
                Reader test_data_script = new InputStreamReader(
                        Main.class.getResourceAsStream("/db_test_data.sql"));) {

            ScriptRunner runner = new ScriptRunner(conn, false, true);

            runner.runScript(init_script);
            runner.runScript(test_data_script);

        }
    }

    private static void shutdownTestDb() {
        try {
            DriverManager.getConnection(dbConnStr + ";shutdown=true");
        } catch (SQLException sqle) {
            printSQLException(sqle);
        }
    }

    private static BasicDataSource setupDataSource() throws ClassNotFoundException {

        BasicDataSource ds = new BasicDataSource();

        ds.setDriverClassName(dbDriver);
        ds.setUrl(dbConnStr);

        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        ds.setDefaultAutoCommit(true);
        ds.setPoolPreparedStatements(true);

        return ds;
    }

    private static void closeDataSource(BasicDataSource ds) {
        if (ds != null) {
            try {
                ds.close();
            } catch (SQLException sqle) {
                printSQLException(sqle);
            }
        }
    }

    private static HttpServer startWebServer() {
        URI baseUri = UriBuilder.fromUri("http://localhost/").port(9997)
                .build();
        ResourceConfig config = new ResourceConfig(Api.class);
        return GrizzlyHttpServerFactory.createHttpServer(baseUri, config);
    }

    private static void shutdownWebServer(HttpServer hs) {
        if (hs != null) {
            try {
                hs.shutdown().get();
            } catch (InterruptedException | ExecutionException ex) {
                hs.shutdownNow();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        HttpServer server = null;

        try {
            System.out.println("Starting...\n");

            setupTestDb();
            dataSource = setupDataSource();
            server = startWebServer();

            System.out.println("\nStarted Successfully!\n");

            System.in.read();

        } catch (SQLException sqle) {
            printSQLException(sqle);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            System.out.println("\nshutting down...\n");

            shutdownWebServer(server);
            closeDataSource(dataSource);
            shutdownTestDb();

            System.out.println("\nshut down successfully\n");
        }

    }

    public static void printSQLException(SQLException e) {
        // Unwraps the entire exception chain to unveil the real cause of the Exception.
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
