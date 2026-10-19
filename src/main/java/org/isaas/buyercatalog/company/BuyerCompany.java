package org.isaas.buyercatalog.company;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A buyer company. It starts 'pending' and can only be 'active' while
 * {@code verificationStatus} is 'verified' (CHECK
 * buyer_companies_active_requires_verified); only an active company can
 * subscribe or add payment methods. verified_at is stamped by a trigger.
 */
@Entity
@Table(schema = "buyer_companies", name = "buyer_companies")
public class BuyerCompany {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Column(name = "website_url")
	private String websiteUrl;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private CompanyStatus status = CompanyStatus.pending;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "verification_status", nullable = false)
	private VerificationStatus verificationStatus = VerificationStatus.unverified;

	@Column(name = "verified_by_staff_id")
	private UUID verifiedByStaffId;

	@Generated(event = { EventType.INSERT, EventType.UPDATE })
	@Column(name = "verified_at", insertable = false, updatable = false)
	private Instant verifiedAt;

	@Generated
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected BuyerCompany() {
	}

	public BuyerCompany(String name, String websiteUrl) {
		this.name = name;
		this.websiteUrl = websiteUrl;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public CompanyStatus getStatus() {
		return status;
	}

	public void setStatus(CompanyStatus status) {
		this.status = status;
	}

	public VerificationStatus getVerificationStatus() {
		return verificationStatus;
	}

	/** The first document submitted moves an unverified company into review. */
	public void markSubmittedForReview() {
		if (verificationStatus == VerificationStatus.unverified) {
			verificationStatus = VerificationStatus.pending;
		}
	}

	/** Records a staff decision; the trigger keeps verified_at in step. */
	public void decideVerification(VerificationStatus decision, UUID staffId) {
		this.verificationStatus = decision;
		this.verifiedByStaffId = decision == VerificationStatus.verified ? staffId : null;
	}

	public UUID getVerifiedByStaffId() {
		return verifiedByStaffId;
	}

	public Instant getVerifiedAt() {
		return verifiedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
