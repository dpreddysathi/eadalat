package com.eadalat.hearing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignalMessage {
    private String from;
    private SignalType type;
    private String sdp;
    private String candidate;
}
