package hello;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class HelloController {

	@RequestMapping("/")
	public String index() {
		System.out.println("Hello Francesco, greetings from Spring Boot !");
		return "Hello Francesco, greetings from Spring Boot !";
	}

}