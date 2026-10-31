package org.isaas.buyercatalog.company;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A member of a company. The row is the only authority on which company a
 * person belongs to and with what role. auth_provider_id (the Auth0 subject)
 * is NULL until the person's first login binds it by verified email.
 */
@Entity
@Table(schema = "buyer_companies", name = "buyer_users")
public class BuyerUser {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(name = "buyer_company_id", nullable = false)
	private UUID buyerCompanyId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String email;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private BuyerUserRole role;

	@Column(name = "auth_provider_id")
	private String authProviderId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private BuyerUserStatus status = BuyerUserStatus.active;

	@Generated
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected BuyerUser() {
	}

	public BuyerUser(UUID buyerCompanyId, String name, String email, BuyerUserRole role, String authProviderId) {
		this.buyerCompanyId = buyerCompanyId;
		this.name = name;
		this.email = email;
		this.role = role;
		this.authProviderId = authProviderId;
	}

	public UUID getId() {
		return id;
	}

	public UUID getBuyerCompanyId() {
		return buyerCompanyId;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public BuyerUserRole getRole() {
		return role;
	}

	public void setRole(BuyerUserRole role) {
		this.role = role;
	}

	public String getAuthProviderId() {
		return authProviderId;
	}

	public BuyerUserStatus getStatus() {
		return status;
	}

	public void setStatus(BuyerUserStatus status) {
		this.status = status;
	}

	public boolean isActive() {
		return status == BuyerUserStatus.active;
	}

	public boolean isActiveAdmin() {
		return isActive() && role == BuyerUserRole.admin;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
