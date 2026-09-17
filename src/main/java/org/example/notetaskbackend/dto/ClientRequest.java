package org.example.notetaskbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
    @NotBlank(message = "Prompt must been not null!")
    @Size(max = 4000, message = "Size must been < 4000")
    String prompt){
}
