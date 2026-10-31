package org.isaas.buyercatalog.security;

import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/** A request authenticated with a personal access token instead of an Auth0 login. */
public class AccessTokenAuthentication extends AbstractAuthenticationToken {

	private final UUID buyerUserId;

	private final UUID tokenId;

	public AccessTokenAuthentication(UUID buyerUserId, UUID tokenId) {
		super(List.of());
		this.buyerUserId = buyerUserId;
		this.tokenId = tokenId;
		setAuthenticated(true);
	}

	public UUID getBuyerUserId() {
		return buyerUserId;
	}

	public UUID getTokenId() {
		return tokenId;
	}

	@Override
	public Object getCredentials() {
		return "";
	}

	@Override
	public Object getPrincipal() {
		return buyerUserId;
	}

}
