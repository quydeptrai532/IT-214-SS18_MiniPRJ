package com.rikkeibank.account.security;

import com.rikkeibank.common.security.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** CUSTOMER chỉ xem được tài khoản thuộc về mình. */
@Component("accountOwnershipChecker")
public class AccountOwnershipChecker {

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
