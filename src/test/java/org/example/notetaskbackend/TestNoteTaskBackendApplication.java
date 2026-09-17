package org.example.notetaskbackend;

import org.springframework.boot.SpringApplication;

public class TestNoteTaskBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(NoteTaskBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
