package hr.algebra.photoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PhotoappApplication {
	public static void main(String[] args) {
		SpringApplication.run(PhotoappApplication.class, args);
	}
}
