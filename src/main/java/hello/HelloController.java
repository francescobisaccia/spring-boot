package hello;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@RestController
public class HelloController {

	private static final String DEFAULT_REDIS_HOST = "redis";

	@RequestMapping("/")
	public String index() {
		System.out.println("Hello Francesco, greetings from Spring Boot !");
		JedisPool pool = null;
		Jedis jedis = null;
		String result = "";
		try {

			String hostname = DEFAULT_REDIS_HOST;
			String redis_host = System.getenv("REDIS_HOST");
			if (!"".equals(redis_host)) {
				hostname = System.getenv("REDIS_HOST");
			}

			System.out.println("Connecting to redis at host '" + hostname + "'");

			InetAddress address;
			try {
				address = InetAddress.getByName(hostname);
				System.out.println("IP : " + address.getHostAddress());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}

			pool = new JedisPool(new JedisPoolConfig(), hostname, 6379, 10, "CpHe6TPt3ch1i8Gm");

			jedis = pool.getResource();
			long startTime = System.currentTimeMillis();

			jedis.set("foo", "bar");
			result = jedis.get("foo");
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