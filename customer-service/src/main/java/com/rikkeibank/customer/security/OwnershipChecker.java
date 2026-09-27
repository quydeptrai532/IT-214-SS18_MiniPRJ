package com.rikkeibank.customer.security;

import com.rikkeibank.common.security.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra quyền sở hữu dữ liệu: CUSTOMER chỉ được xem hồ sơ của chính mình.
 * Được gọi trong @PreAuthorize của controller.
 */
@Component("ownershipChecker")
public class OwnershipChecker {

    public boolean isOwner(Authentication authentication, Long customerId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) {
            return false;
        }
        if (user.isAdmin() || user.isTeller()) {
            return true;
        }
        return customerId != null && customerId.equals(user.userId());
    }

    /** Dùng cho tài khoản: CUSTOMER chỉ xem tài khoản thuộc customerId của mình. */
    public boolean isAccountOwner(Authentication authentication, Long ownerCustomerId) {
        return isOwner(authentication, ownerCustomerId);
    }
}
