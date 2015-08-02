/**
 * 
 */
package revoluttest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author xuesong
 *
 */
public class MainIT {

	private static ObjectMapper om = new ObjectMapper();
	private static CloseableHttpClient httpclient = HttpClients.createDefault();
	private static Process server;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		server = new ProcessBuilder("java", "-jar",
				"./target/revolut-test-1.0.0.jar")
				.start();

		Thread.sleep(10 * 1000);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		Writer w = new OutputStreamWriter(server.getOutputStream());
		w.write("\n");
		w.flush();
		server.waitFor();
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
		getBalance(1, 200, 100);
		getBalance(2, 200, 200);
		getBalance(3, 200, 300);
		//getBalance(4, 404, 0);
	}

	private void getBalance(long userId, int httpStatus, long balance)
			throws ClientProtocolException, IOException {
		HttpGet httpGet = new HttpGet("http://localhost:9997/api/balance/"
				+ userId);
		System.out.println("1");
		CloseableHttpResponse response = httpclient.execute(httpGet);
		System.out.println("2");
		try {
			assertEquals(response.getStatusLine().getStatusCode(), httpStatus);
			if(httpStatus == 200) {
				BalanceBean ret = om.readValue(response.getEntity().getContent(),
						BalanceBean.class);

				System.out.println("3");
				assertEquals(ret.getBalance(), balance);
			}
			//EntityUtils.consume(response.getEntity());
		} finally {
			response.close();
		}
	}

}
