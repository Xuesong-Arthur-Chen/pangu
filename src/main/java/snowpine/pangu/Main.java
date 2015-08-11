package snowpine.pangu;

import snowpine.pangu.rest.Api;
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
import snowpine.pangu.dao.DAOObjs;
import snowpine.pangu.dao.DAOWrapperException;
import snowpine.pangu.dao.TransactionDAOJdbcImpl;
import snowpine.pangu.dao.UserDAOJdbcImpl;

public class Main {

    private static final String dbDriver = "org.hsqldb.jdbc.JDBCDriver";
    private static final String dbConnStr = "jdbc:hsqldb:mem:testdb;hsqldb.tx=mvcc";

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
            DAOWrapperException.printSQLException(sqle);
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
                DAOWrapperException.printSQLException(sqle);
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
        BasicDataSource dataSource = null;

        try {
            System.out.println("Starting...\n");

            setupTestDb();
            dataSource = setupDataSource();
            DAOObjs.userDAO = new UserDAOJdbcImpl(dataSource);
            DAOObjs.transactionDAO = new TransactionDAOJdbcImpl(dataSource, DAOObjs.userDAO);
            server = startWebServer();

            System.out.println("\nStarted Successfully!\n");

            System.in.read();

        } catch (SQLException sqle) {
            DAOWrapperException.printSQLException(sqle);
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
}
