package org.example.notetaskbackend.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GigaChatService {

    public Mono<String> generateTask(String prompt){
        return Mono.just("STUB!");
    }

}
