package com.eadalat.casework.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseSummaryResponse {

    private Long caseId;
    private String summary;
    private List<String> keywords;
}
