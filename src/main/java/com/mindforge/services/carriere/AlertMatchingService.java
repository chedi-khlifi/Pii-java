package com.mindforge.services.carriere;

import com.mindforge.entities.carriere.JobAlertSubscription;
import com.mindforge.entities.carriere.OpportuniteCarriere;

import java.util.List;

public class AlertMatchingService {

    private final JobAlertSubscriptionService subscriptionService;
    private final JobAlertNotificationService notificationService;

    public AlertMatchingService() {
        this.subscriptionService = new JobAlertSubscriptionService();
        this.notificationService = new JobAlertNotificationService();
    }

    /**
     * Called whenever a new OpportuniteCarriere is created.
     * Mirrors OpportuniteCreatedListener + AlertMatchingService from Symfony.
     */
    public void matchAndNotify(OpportuniteCarriere opportunite) {
        List<JobAlertSubscription> subs = subscriptionService.getAllActive();
        for (JobAlertSubscription sub : subs) {
            if (matches(sub, opportunite)) {
                notificationService.add(sub.getId(), opportunite.getId());
            }
        }
    }

    private boolean matches(JobAlertSubscription sub, OpportuniteCarriere opp) {
        // Keywords filter
        if (sub.getKeywords() != null && !sub.getKeywords().isBlank()) {
            String kw    = sub.getKeywords().toLowerCase();
            String title = opp.getTitle() != null ? opp.getTitle().toLowerCase() : "";
            String desc  = opp.getDescription() != null ? opp.getDescription().toLowerCase() : "";
            if (!title.contains(kw) && !desc.contains(kw)) return false;
        }

        // Type filter
        if (sub.getType() != null && !sub.getType().isBlank()) {
            if (!sub.getType().equalsIgnoreCase(opp.getType())) return false;
        }

        // Location filter
        if (sub.getLocation() != null && !sub.getLocation().isBlank()) {
            String subLoc = sub.getLocation().toLowerCase();
            String oppLoc = opp.getLocation() != null ? opp.getLocation().toLowerCase() : "";
            if (!oppLoc.contains(subLoc)) return false;
        }

        return true;
    }
}
