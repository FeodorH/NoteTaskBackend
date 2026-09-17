package org.example.notetaskbackend;

import org.example.notetaskbackend.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class NoteTaskBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(NoteTaskBackendApplication.class, args);
	}
}
