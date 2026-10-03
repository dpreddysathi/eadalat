package com.eadalat.casework.model;

/**
 * Lifecycle of a case in the e-Adalat system. Legal forward transitions:
 * FILED -> ASSIGNED -> HEARING_SCHEDULED -> IN_HEARING -> VERDICT_PENDING -> CLOSED
 */
public enum CaseStatus {
    FILED,
    ASSIGNED,
    HEARING_SCHEDULED,
    IN_HEARING,
    VERDICT_PENDING,
    CLOSED;

    /**
     * Returns true if this status may legally move directly to {@code next}.
     */
    public boolean canTransitionTo(CaseStatus next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
            case FILED -> next == ASSIGNED;
            case ASSIGNED -> next == HEARING_SCHEDULED;
            case HEARING_SCHEDULED -> next == IN_HEARING;
            case IN_HEARING -> next == VERDICT_PENDING;
            case VERDICT_PENDING -> next == CLOSED;
            case CLOSED -> false;
        };
    }
}
