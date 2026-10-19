package org.isaas.buyercatalog.identity;

import org.isaas.buyercatalog.api.ApiException;
import org.isaas.buyercatalog.staff.StaffRole;
import org.isaas.buyercatalog.staff.StaffUser;
import org.isaas.buyercatalog.company.BuyerUser;
import org.springframework.http.HttpStatus;

/**
 * The caller of a request: their Auth0 identity, their company membership (if
 * any) and their staff role (if any). Membership and roles come only from the
 * database, never from token claims. The company a request acts on is always
 * the member's own company, never one named in the request.
 */
public final class CurrentUser {

	/** How the request was authenticated. */
	public enum Via {
		auth0, access_token
	}

	private final String subject;

	private final String email;

	private final boolean emailVerified;

	private final Via via;

	private final BuyerUser member;

	private final StaffUser staff;

	CurrentUser(String subject, String email, boolean emailVerified, Via via, BuyerUser member, StaffUser staff) {
		this.subject = subject;
		this.email = email;
		this.emailVerified = emailVerified;
		this.via = via;
		this.member = member;
		this.staff = staff;
	}

	public String subject() {
		return subject;
	}

	public String email() {
		return email;
	}

	public boolean emailVerified() {
		return emailVerified;
	}

	public Via via() {
		return via;
	}

	/** The company membership, active or not, or null for an unaffiliated user. */
	public BuyerUser member() {
		return member;
	}

	public StaffUser staff() {
		return staff;
	}

	/** An active member of some company. */
	public BuyerUser requireMember() {
		if (member == null) {
			throw ApiException.forbidden("NOT_A_MEMBER", "You are not a member of a company yet.");
		}
		if (!member.isActive()) {
			throw ApiException.forbidden("ACCOUNT_DISABLED", "Your company membership is disabled.");
		}
		return member;
	}

	/** An active admin of their company. */
	public BuyerUser requireAdmin() {
		BuyerUser user = requireMember();
		if (!user.isActiveAdmin()) {
			throw ApiException.forbidden("ADMIN_REQUIRED", "Only a company admin can do this.");
		}
		return user;
	}

	/** A person signed in through Auth0, not an agent using an access token. */
	public void requireLogin() {
		if (via != Via.auth0) {
			throw ApiException.forbidden("LOGIN_REQUIRED", "This needs a signed-in user, not an access token.");
		}
	}

	/** The verified email of a signed-in user, needed to create or join a company. */
	public String requireVerifiedEmail() {
		requireLogin();
		if (email == null || !emailVerified) {
			throw ApiException.forbidden("EMAIL_NOT_VERIFIED", "Verify your email address first.");
		}
		return email;
	}

	/** An active staff member, signed in through Auth0. */
	public StaffUser requireStaff() {
		requireLogin();
		if (staff == null || !staff.isActive()) {
			throw ApiException.forbidden("STAFF_REQUIRED", "Only marketplace staff can do this.");
		}
		return staff;
	}

	public StaffUser requireStaffAdmin() {
		StaffUser user = requireStaff();
		if (user.getRole() != StaffRole.admin) {
			throw new ApiException(HttpStatus.FORBIDDEN, "STAFF_ADMIN_REQUIRED", "Only a staff admin can do this.");
		}
		return user;
	}

}
