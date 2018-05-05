package hello;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@RestController
public class HelloController {

	@RequestMapping("/")
	public String index() {
		System.out.println("Hello Francesco, greetings from Spring Boot !");
		JedisPool pool = null;
		Jedis jedis = null;
		String result = "";
		try {
			pool = new JedisPool(new JedisPoolConfig(), "redis://redis", 6379, 10, "CpHe6TPt3ch1i8Gm");

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