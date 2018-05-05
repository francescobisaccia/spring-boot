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
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "redis", 6379, 10, "CpHe6TPt3ch1i8Gm");

		Jedis jedis = pool.getResource();
		long startTime = System.currentTimeMillis();

		jedis.set("foo", "bar");
		String foobar = jedis.get("foo");
		System.out.println("Execution time: " + (System.currentTimeMillis() - startTime));

		pool.returnResource(jedis);
		pool.close();
		return ("foo: " + foobar);

	}

}