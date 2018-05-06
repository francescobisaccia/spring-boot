package hello;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hello.HttpClientAdapter.HttpClientAdapterConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@RestController
public class HelloController {

	private static final String DEFAULT_REDIS_HOST = "172.30.222.214";
	private static final String targetUrl = "http://userprofile-ms-it-63.openshift.avs-accenture.com/avsbe-userprofile-ms/v1/users/USERNAME";
	private static HttpClientAdapterConfiguration httpClientAdapterConfiguration = new HttpClientAdapterConfiguration();

	@RequestMapping("/")
	public String index() {
		System.out.println("Hello Francesco, greetings from Spring Boot !");
		JedisPool pool = null;
		Jedis jedis = null;
		String result = "";
		httpClientAdapterConfiguration.setTenantName("tenant1");
		httpClientAdapterConfiguration.setConnectionTimeout(1000);
		httpClientAdapterConfiguration.setSocketTimeout(1000);
		String username = "test@mailinator.com";

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
				result = HttpClientAdapter.doGet(httpClientAdapterConfiguration, targetUrl, null, pathParamsMap, null);
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