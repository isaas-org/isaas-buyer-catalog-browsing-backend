package org.isaas.buyercatalog.security;

import java.time.Clock;
import java.time.Instant;

import org.isaas.buyercatalog.accesstoken.BuyerAccessToken;
import org.isaas.buyercatalog.accesstoken.BuyerAccessTokenRepository;
import org.isaas.buyercatalog.company.BuyerUserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates {@code isaas_bpat_} bearer tokens. A token works only while it
 * is unrevoked, unexpired and its owner is an active member, so disabling a
 * user stops their agents on the next request.
 */
@Component
public class AccessTokenAuthenticationProvider implements AuthenticationProvider {

	private final BuyerAccessTokenRepository tokens;

	private final BuyerUserRepository users;

	private final Clock clock;

	public AccessTokenAuthenticationProvider(BuyerAccessTokenRepository tokens, BuyerUserRepository users,
			Clock clock) {
		this.tokens = tokens;
		this.users = users;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Authentication authenticate(Authentication authentication) {
		String token = ((BearerTokenAuthenticationToken) authentication).getToken();
		Instant now = clock.instant();
		BuyerAccessToken stored = tokens.findByTokenHash(AccessTokens.hash(token))
			.filter((candidate) -> candidate.isUsableAt(now))
			.orElseThrow(() -> new InvalidBearerTokenException("Access token is unknown, revoked or expired"));
		boolean ownerActive = users.findById(stored.getBuyerUserId()).map((user) -> user.isActive()).orElse(false);
		if (!ownerActive) {
			throw new InvalidBearerTokenException("Access token owner is disabled");
		}
		tokens.touch(stored.getId());
		return new AccessTokenAuthentication(stored.getBuyerUserId(), stored.getId());
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
	}

}
