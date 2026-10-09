package service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Card approvals arrive in UTC; settlement and partner statements use the KST calendar date. */
public class BusinessDateResolver {
    private final ZoneId zone;

    public BusinessDateResolver(ZoneId zone) {
        this.zone = zone;
    }

    public LocalDate dateOf(Instant approvedAt) {
        return approvedAt.atZone(zone).toLocalDate();
    }
}
