package com.koreplan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KorePlanApplication {

	
	static {
		// Eclipse에서 .env 파일 강제 로드
		try {
			Path envPath = Paths.get(System.getProperty("user.dir"), ".env");
			if (Files.exists(envPath)) {
				Properties props = new Properties();
				props.load(Files.newBufferedReader(envPath));
				props.forEach((key, value) -> System.setProperty(key.toString(), value.toString()));
				System.out.println("Loaded .env file: " + envPath);
			}
		} catch (Exception e) {
			System.err.println("Failed to load .env file: " + e.getMessage());
		}
	}
	public static void main(String[] args) {
		SpringApplication.run(KorePlanApplication.class, args);
	}

}