package com.eadalat.casework.dto;

import com.eadalat.casework.model.CaseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "status is required")
    private CaseStatus status;
}
