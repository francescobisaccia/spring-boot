package hello;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.springframework.web.bind.annotation.RestController;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@RestController
public class HelloController {

	private static final String DEFAULT_REDIS_HOST = "172.30.206.236";

	@GET
	@Path("/users")
	public String getUserById(@PathParam("username") String username) {
		System.out.println("Request received for username: " + username);
		JedisPool pool = null;
		Jedis jedis = null;
		String result = "";
		try {
			String hostname = DEFAULT_REDIS_HOST;
			String redis_host = System.getenv("REDIS_HOST");
			if (redis_host != null && !"".equals(redis_host)) {
				hostname = System.getenv("REDIS_HOST");
			}

			System.out.println("Connecting to redis at host '" + hostname + "'");

			pool = new JedisPool(new JedisPoolConfig(), hostname, 6379, 10, "CpHe6TPt3ch1i8Gm");

			jedis = pool.getResource();
			long startTime = System.currentTimeMillis();

			result = jedis.get(username);

			if (result == null) {
				Map<String, String> pathParamsMap = new HashMap<String, String>();
				pathParamsMap.put("USERNAME", "test@mailinator.com");
				result = "{\"menu\": {\r\n  \"id\": \"file\",\r\n  \"value\": \"File\",\r\n  \"popup\": {\r\n    \"menuitem\": [\r\n      {\"value\": \"New\", \"onclick\": \"CreateNewDoc()\"},\r\n      {\"value\": \"Open\", \"onclick\": \"OpenDoc()\"},\r\n      {\"value\": \"Close\", \"onclick\": \"CloseDoc()\"}\r\n    ]\r\n  }\r\n}}";
				System.out.println("GetUser response: " + result);
				if (result != null && result.length() > 0) {
					jedis.set(username, result);
					System.out.println("Username: " + username + " cached");
				}

			}
			System.out.println("Execution time: " + (System.currentTimeMillis() - startTime));

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			pool.returnResource(jedis);
			pool.close();
		}

		return ("result: " + result);

	}

}