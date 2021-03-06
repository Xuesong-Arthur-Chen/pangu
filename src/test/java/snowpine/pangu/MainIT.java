/**
 *
 */
package snowpine.pangu;

import com.ibatis.common.jdbc.ScriptRunner;
import snowpine.pangu.rest.TransferReq;
import snowpine.pangu.rest.TransferRes;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import snowpine.pangu.dao.DAOWrapperException;
import snowpine.pangu.dao.Transaction;
import snowpine.pangu.rest.BalanceRes;
import snowpine.pangu.rest.LoginReq;
import snowpine.pangu.rest.LoginRes;

/**
 * @author xuesong
 *
 */
public class MainIT {

    class AuthRequestFilter implements ClientRequestFilter {

        private final String token;

        public AuthRequestFilter(String token) {
            this.token = token;
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
    }

    private static final Client defaultClient = ClientBuilder.newClient();
    private static Process server;

    private static void setupTestDb() throws SQLException, IOException,
            ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException {
        Class.forName(Main.dbDriver);
        try (Connection conn = DriverManager.getConnection(Main.dbLocation, Main.dbUser, Main.dbPass);
                Statement st = conn.createStatement();) {

            st.executeUpdate("DROP DATABASE IF EXISTS testdb;");
            st.executeUpdate("CREATE DATABASE testdb;");

        }

        try (Connection conn = DriverManager.getConnection(Main.dbConnStr, Main.dbUser, Main.dbPass);
                Reader schema_script = new InputStreamReader(MainIT.class.getResourceAsStream("/db_schema.sql"));
                PreparedStatement st = conn.prepareStatement("INSERT INTO users (salt, passhash, balance) VALUES (?, ?, ?);");) {

            ScriptRunner runner = new ScriptRunner(conn, false, true);

            runner.runScript(schema_script);

            byte[] salt = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
            PBEKeySpec ks = new PBEKeySpec("password".toCharArray(), salt, 1000, 16);
            SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] passhash = kf.generateSecret(ks).getEncoded();

            Base64.Encoder enc = Base64.getEncoder();

            st.setString(1, enc.encodeToString(salt));
            st.setString(2, enc.encodeToString(passhash));
            st.setLong(3, 100);
            st.executeUpdate();

            st.setString(1, enc.encodeToString(salt));
            st.setString(2, enc.encodeToString(passhash));
            st.setLong(3, 200);
            st.executeUpdate();

            st.setString(1, enc.encodeToString(salt));
            st.setString(2, enc.encodeToString(passhash));
            st.setLong(3, 300);
            st.executeUpdate();
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.out.println("setup test database...\n");
        try {
            setupTestDb();
        } catch (SQLException sqle) {
            DAOWrapperException.printSQLException(sqle);
            throw sqle;
        }

        System.out.println("done\n");

        System.out.println("starting REST API server...\n");
        ProcessBuilder pb = new ProcessBuilder("java", "-jar",
                "./target/pangu-1.0.0.jar");
        pb.redirectError(new File("./error.log"));
        pb.redirectOutput(new File("./output.log"));
        server = pb.start();

        Thread.sleep(10 * 1000);
        System.out.println("done\n");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (server == null) {
            return;
        }

        System.out.println("\nshutting down REST API server...\n");
        Writer w = new OutputStreamWriter(server.getOutputStream());
        w.write("\n");
        w.flush();
        server.waitFor();
        System.out.println("done\n");
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception {

        testLogin(new LoginReq(1, "password"), 200);
        testLogin(new LoginReq(2, "password"), 200);
        testLogin(new LoginReq(3, "password"), 200);
        testLogin(new LoginReq(1, "password1"), 400);
        
        String t1 = login(new LoginReq(1, "password"));
        final Client c1 = ClientBuilder.newClient(new ClientConfig().register(new AuthRequestFilter(t1)));

        String t2 = login(new LoginReq(2, "password"));
        final Client c2 = ClientBuilder.newClient(new ClientConfig().register(new AuthRequestFilter(t2)));
        
        String t3 = login(new LoginReq(3, "password"));
        final Client c3 = ClientBuilder.newClient(new ClientConfig().register(new AuthRequestFilter(t3)));

        testGetBalance(c1, 1, 200, 100);
        testGetBalance(c2, 2, 200, 200);
        testGetBalance(c3, 3, 200, 300);

        testTransfer(c3, new TransferReq(3, 1, 100), 200, 1);
        testTransfer(c3, new TransferReq(3, 4, 100), 400, 2);
        testTransfer(c1, new TransferReq(4, 1, 100), 403, 3);
        testTransfer(c3, new TransferReq(3, 1, 1000), 400, 4);
        testTransfer(c3, new TransferReq(3, 1, -1000), 400, 5);
        testTransfer(c1, new TransferReq(1, 1, 100), 400, 6);

        testGetBalance(c1, 1, 200, 200);
        testGetBalance(c2, 2, 200, 200);
        testGetBalance(c3, 3, 200, 200);

        testGetTransaction(c3, 1, 200, new TransferReq(3, 1, 100));
        testGetTransaction(c3, 2, 404, null);

        int n = Runtime.getRuntime().availableProcessors() * 3;
        ExecutorService executor = Executors.newFixedThreadPool(n);
        class Task implements Runnable {

            private final TransferReq req;
            private final Client c;

            Task(Client c, TransferReq req) {
                this.c = c;
                this.req = req;
            }

            @Override
            public void run() {
                transfer(c, req);
            }
        }

        for (int i = 0; i < 15; i++) {
            int j = i % 3;
            Client c;
            if(j == 0) c = c1;
            else if(j == 1) c = c2;
            else c = c3;
            executor.submit(new Task(c, new TransferReq(j + 1, (j + 1) % 3 + 1, i + 1)));
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        testGetBalance(c1, 1, 200, 210);
        testGetBalance(c2, 2, 200, 195);
        testGetBalance(c3, 3, 200, 195);

    }

    private void testGetBalance(Client c, long userId, int httpStatus, long balance) {
        System.out.println("testing user " + userId + " balance");

        WebTarget target = c.target("http://localhost:9997/api/balance/"
                + userId);
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(httpStatus, response.getStatus());
        if (response.getStatus() == 200) {
            BalanceRes ret = response.readEntity(BalanceRes.class);
            assertEquals(balance, ret.getBalance());
        } else {
            System.out.println(response.readEntity(String.class));
        }

    }

    private void transfer(Client c, TransferReq req) {
        System.out.println("making transfer from " + req.getFrom()
                + " to " + req.getTo() + ": amount " + req.getAmount());

        WebTarget target = c.target("http://localhost:9997/api/transfer");
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(req, MediaType.APPLICATION_JSON_TYPE));

        System.out.println(response.getStatus());
        System.out.println(response.readEntity(String.class));
    }

    private void testTransfer(Client c, TransferReq req, int httpStatus,
            long transactionId) {
        System.out.println("testing transfer " + transactionId);

        WebTarget target = c.target("http://localhost:9997/api/transfer");
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(req, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(httpStatus, response.getStatus());
        if (response.getStatus() == 200) {
            TransferRes ret = response.readEntity(TransferRes.class);
            assertEquals(transactionId, ret.getTransactionId());
        } else {
            System.out.println(response.readEntity(String.class));
        }
    }

    private void testGetTransaction(Client c, long transactionId, int httpStatus, TransferReq req) {
        System.out.println("testing transaction " + transactionId);

        WebTarget target = c.target("http://localhost:9997/api/transaction/"
                + transactionId);
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(httpStatus, response.getStatus());
        if (response.getStatus() == 200) {
            Transaction ret = response.readEntity(Transaction.class);
            assertEquals(req.getFrom(), ret.getFromUser());
            assertEquals(req.getTo(), ret.getToUser());
            assertEquals(req.getAmount(), ret.getAmount());
        } else {
            System.out.println(response.readEntity(String.class));
        }
    }
    
    private String login(LoginReq req) {
        
        WebTarget target = defaultClient.target("http://localhost:9997/login");
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(req, MediaType.APPLICATION_JSON_TYPE));
        
        return response.readEntity(LoginRes.class).getAccess_token();
    }

    private void testLogin(LoginReq req, int httpStatus) {
        System.out.println("testing login user: " + req.getUserId());

        WebTarget target = defaultClient.target("http://localhost:9997/login");
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(req, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(httpStatus, response.getStatus());

        System.out.println(response.readEntity(String.class));

    }

}
