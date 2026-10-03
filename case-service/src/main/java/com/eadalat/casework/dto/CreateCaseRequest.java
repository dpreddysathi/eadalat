package com.eadalat.casework.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCaseRequest {

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    private String category;
}
