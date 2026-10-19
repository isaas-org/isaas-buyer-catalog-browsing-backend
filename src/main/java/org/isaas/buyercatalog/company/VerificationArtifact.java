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
 * A verification document submitted by a buyer company admin. Documents are checked outside
 * the system for now, so file_url points at wherever the document lives.
 */
@Entity
@Table(schema = "buyer_companies", name = "buyer_verification_artifacts")
public class VerificationArtifact {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(name = "buyer_company_id", nullable = false)
	private UUID buyerCompanyId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "artifact_type", nullable = false)
	private ArtifactType artifactType;

	@Column(name = "file_url", nullable = false)
	private String fileUrl;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private ArtifactStatus status = ArtifactStatus.pending;

	@Column(name = "submitted_by_user_id", nullable = false)
	private UUID submittedByUserId;

	@Column(name = "reviewed_by_staff_id")
	private UUID reviewedByStaffId;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "review_notes")
	private String reviewNotes;

	@Generated
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected VerificationArtifact() {
	}

	public VerificationArtifact(UUID buyerCompanyId, ArtifactType artifactType, String fileUrl, UUID submittedByUserId) {
		this.buyerCompanyId = buyerCompanyId;
		this.artifactType = artifactType;
		this.fileUrl = fileUrl;
		this.submittedByUserId = submittedByUserId;
	}

	public void review(ArtifactStatus decision, UUID staffId, String notes) {
		this.status = decision;
		this.reviewedByStaffId = staffId;
		this.reviewedAt = Instant.now();
		this.reviewNotes = notes;
	}

	public UUID getId() {
		return id;
	}

	public UUID getBuyerCompanyId() {
		return buyerCompanyId;
	}

	public ArtifactType getArtifactType() {
		return artifactType;
	}

	public String getFileUrl() {
		return fileUrl;
	}

	public ArtifactStatus getStatus() {
		return status;
	}

	public UUID getSubmittedByUserId() {
		return submittedByUserId;
	}

	public UUID getReviewedByStaffId() {
		return reviewedByStaffId;
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
