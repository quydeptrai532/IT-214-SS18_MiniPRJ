package com.rikkeibank.notification.security;

import com.rikkeibank.common.security.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** CUSTOMER chỉ xem thông báo của chính mình. */
@Component("notificationOwnershipChecker")
public class NotificationOwnershipChecker {

    public boolean isOwner(Authentication authentication, Long customerId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) {
            return false;
        }
        if (user.isAdmin() || user.isTeller()) {
            return true;
        }
        return customerId != null && customerId.equals(user.userId());
    }
}
