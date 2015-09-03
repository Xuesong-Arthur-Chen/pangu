package snowpine.pangu;

import snowpine.pangu.rest.Api;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.dbcp2.BasicDataSource;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.concurrent.ExecutionException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import snowpine.pangu.dao.DAOObjs;
import snowpine.pangu.dao.DAOWrapperException;
import snowpine.pangu.dao.TransactionDAOJdbcImpl;
import snowpine.pangu.dao.UserDAOJdbcImpl;
import snowpine.pangu.rest.CORSFilter;
import snowpine.pangu.rest.Login;

public class Main {

    //config info should be loaded from config file
    public static final String dbDriver = "org.postgresql.Driver";
    public static final String dbLocation = "jdbc:postgresql://localhost:5432/";
    public static final String dbName = "testdb";
    public static final String dbConnStr = dbLocation + dbName;
    public static final String dbUser = "postgres";
    public static final String dbPass = "";

    private static BasicDataSource setupDataSource() throws ClassNotFoundException {

        BasicDataSource ds = new BasicDataSource();

        ds.setDriverClassName(dbDriver);
        ds.setUrl(dbConnStr);
        ds.setUsername(dbUser);
        ds.setPassword(dbPass);

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
        ResourceConfig config = new ResourceConfig(CORSFilter.class, Login.class, Api.class);
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

            dataSource = setupDataSource();
            DAOObjs.userDAO = new UserDAOJdbcImpl(dataSource);
            DAOObjs.transactionDAO = new TransactionDAOJdbcImpl(dataSource, DAOObjs.userDAO);
            server = startWebServer();

            System.out.println("\nStarted Successfully!\n");

            System.in.read();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        } finally {
            System.out.println("\nshutting down...\n");

            shutdownWebServer(server);
            closeDataSource(dataSource);

            System.out.println("\nshut down successfully\n");
        }
    }
}
