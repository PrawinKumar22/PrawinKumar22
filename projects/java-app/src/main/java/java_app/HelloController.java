package java_app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

@Value("${APP_MESSAGE:Default message}")
private String appMessage;

@GetMapping("/hello")
public String hello() {
    return "Hello from my Kubernetes Java application - VERSION 1.1!";
}

@GetMapping("/message")
public String message() {
    return appMessage;
}

}
