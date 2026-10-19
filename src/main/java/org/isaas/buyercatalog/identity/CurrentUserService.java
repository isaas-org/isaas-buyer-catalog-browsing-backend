package org.isaas.buyercatalog.identity;

import org.isaas.buyercatalog.security.AccessTokenAuthentication;
import org.isaas.buyercatalog.security.Auth0Claims;
import org.isaas.buyercatalog.staff.StaffUser;
import org.isaas.buyercatalog.staff.StaffUserRepository;
import org.isaas.buyercatalog.company.BuyerUser;
import org.isaas.buyercatalog.company.BuyerUserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the caller from the authenticated request.
 *
 * For an Auth0 login: find the buyer_users and staff_users rows bound to the
 * token's subject. If there is none and the token says the email is verified,
 * bind the subject to an invited or seeded row with that email first. A row
 * is never created here, so signing up with an email grants nothing unless an
 * admin, a seed or an approved join request already put that email on a row.
 *
 * SSO auto-provisioning is not implemented yet: no enterprise connection
 * exists, and the access token doesn't carry the connection.
 */
@Service
public class CurrentUserService {

	private final BuyerUserRepository buyerUsers;

	private final StaffUserRepository staffUsers;

	public CurrentUserService(BuyerUserRepository buyerUsers, StaffUserRepository staffUsers) {
		this.buyerUsers = buyerUsers;
		this.staffUsers = staffUsers;
	}

	@Transactional
	public CurrentUser resolve(Authentication authentication) {
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			return fromJwt(jwtAuthentication.getToken());
		}
		if (authentication instanceof AccessTokenAuthentication accessToken) {
			return fromAccessToken(accessToken);
		}
		throw new AuthenticationCredentialsNotFoundException("No supported authentication on the request");
	}

	private CurrentUser fromJwt(Jwt jwt) {
		String subject = jwt.getSubject();
		String email = jwt.getClaimAsString(Auth0Claims.EMAIL);
		boolean emailVerified = Boolean.TRUE.equals(jwt.getClaimAsBoolean(Auth0Claims.EMAIL_VERIFIED));
		boolean canBind = emailVerified && email != null;

		BuyerUser member = buyerUsers.findByAuthProviderId(subject).orElse(null);
		if (member == null && canBind && buyerUsers.bindAuthProviderId(subject, email) > 0) {
			member = buyerUsers.findByAuthProviderId(subject).orElse(null);
		}
		StaffUser staff = staffUsers.findByAuthProviderId(subject).orElse(null);
		if (staff == null && canBind && staffUsers.bindAuthProviderId(subject, email) > 0) {
			staff = staffUsers.findByAuthProviderId(subject).orElse(null);
		}
		return new CurrentUser(subject, email, emailVerified, CurrentUser.Via.auth0, member, staff);
	}

	private CurrentUser fromAccessToken(AccessTokenAuthentication accessToken) {
		BuyerUser member = buyerUsers.findById(accessToken.getBuyerUserId())
			.orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Access token owner no longer exists"));
		// An agent acts only as the company member; staff rights never travel with a token.
		return new CurrentUser(member.getAuthProviderId(), member.getEmail(), true, CurrentUser.Via.access_token,
				member, null);
	}

}
