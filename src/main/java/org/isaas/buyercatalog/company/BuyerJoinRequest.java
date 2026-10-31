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
 * How a member after the first got access. The database checks that the
 * path, reviewer and decision agree (buyer_join_requests_path_rules) and
 * that only an active company receives peer or SSO requests (trigger
 * buyer_companies.enforce_join_request_gate).
 */
@Entity
@Table(schema = "buyer_companies", name = "buyer_join_requests")
public class BuyerJoinRequest {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(name = "buyer_company_id", nullable = false)
	private UUID buyerCompanyId;

	@Column(name = "auth_provider_id")
	private String authProviderId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String email;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "requested_role", nullable = false)
	private BuyerUserRole requestedRole;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private JoinRequestStatus status = JoinRequestStatus.pending;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "granted_via", nullable = false)
	private JoinGrantPath grantedVia;

	@Column(name = "reviewed_by_user_id")
	private UUID reviewedByUserId;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "review_notes")
	private String reviewNotes;

	@Generated
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected BuyerJoinRequest() {
	}

	private BuyerJoinRequest(UUID buyerCompanyId, String authProviderId, String name, String email,
			BuyerUserRole requestedRole, JoinGrantPath grantedVia) {
		this.buyerCompanyId = buyerCompanyId;
		this.authProviderId = authProviderId;
		this.name = name;
		this.email = email;
		this.requestedRole = requestedRole;
		this.grantedVia = grantedVia;
	}

	public static BuyerJoinRequest peerRequest(UUID buyerCompanyId, String authProviderId, String name, String email,
			BuyerUserRole requestedRole) {
		return new BuyerJoinRequest(buyerCompanyId, authProviderId, name, email, requestedRole, JoinGrantPath.peer_approval);
	}

	/** An invite is approved at once and names the inviting admin. */
	public static BuyerJoinRequest invite(UUID buyerCompanyId, String name, String email, BuyerUserRole role,
			UUID invitedByUserId) {
		BuyerJoinRequest request = new BuyerJoinRequest(buyerCompanyId, null, name, email, role, JoinGrantPath.admin_invite);
		request.decide(JoinRequestStatus.approved, invitedByUserId, null);
		return request;
	}

	public void decide(JoinRequestStatus decision, UUID reviewerId, String notes) {
		this.status = decision;
		this.reviewedByUserId = reviewerId;
		this.reviewedAt = Instant.now();
		this.reviewNotes = notes;
	}

	public UUID getId() {
		return id;
	}

	public UUID getBuyerCompanyId() {
		return buyerCompanyId;
	}

	public String getAuthProviderId() {
		return authProviderId;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public BuyerUserRole getRequestedRole() {
		return requestedRole;
	}

	public JoinRequestStatus getStatus() {
		return status;
	}

	public JoinGrantPath getGrantedVia() {
		return grantedVia;
	}

	public UUID getReviewedByUserId() {
		return reviewedByUserId;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public String getReviewNotes() {
		return reviewNotes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
