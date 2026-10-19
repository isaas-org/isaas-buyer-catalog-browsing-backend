package org.isaas.buyercatalog.company;

import java.time.Instant;
import java.util.UUID;

/** Response bodies for the buyer company membership endpoints. */
public final class CompanyViews {

	private CompanyViews() {
	}

	public record CompanyView(UUID id, String name, String websiteUrl, CompanyStatus status,
			VerificationStatus verificationStatus, Instant verifiedAt) {

		public static CompanyView from(BuyerCompany company) {
			return new CompanyView(company.getId(), company.getName(), company.getWebsiteUrl(), company.getStatus(),
					company.getVerificationStatus(), company.getVerifiedAt());
		}

	}

	/** What an unaffiliated user sees when looking for a company to join. */
	public record CompanySummary(UUID id, String name) {

		public static CompanySummary from(BuyerCompany company) {
			return new CompanySummary(company.getId(), company.getName());
		}

	}

	/** signedIn is false for an invited member who hasn't logged in yet. */
	public record MemberView(UUID id, String name, String email, BuyerUserRole role, BuyerUserStatus status,
			boolean signedIn, Instant createdAt) {

		public static MemberView from(BuyerUser user) {
			return new MemberView(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus(),
					user.getAuthProviderId() != null, user.getCreatedAt());
		}

	}

	public record JoinRequestView(UUID id, UUID buyerCompanyId, String name, String email,
			BuyerUserRole requestedRole, JoinRequestStatus status, JoinGrantPath grantedVia, UUID reviewedByUserId,
			Instant reviewedAt, String reviewNotes, Instant createdAt) {

		public static JoinRequestView from(BuyerJoinRequest request) {
			return new JoinRequestView(request.getId(), request.getBuyerCompanyId(), request.getName(),
					request.getEmail(), request.getRequestedRole(), request.getStatus(), request.getGrantedVia(),
					request.getReviewedByUserId(), request.getReviewedAt(), request.getReviewNotes(),
					request.getCreatedAt());
		}

	}

	public record DocumentView(UUID id, ArtifactType artifactType, String fileUrl, ArtifactStatus status,
			UUID submittedByUserId, UUID reviewedByStaffId, Instant reviewedAt, String reviewNotes,
			Instant createdAt) {

		public static DocumentView from(VerificationArtifact artifact) {
			return new DocumentView(artifact.getId(), artifact.getArtifactType(), artifact.getFileUrl(),
					artifact.getStatus(), artifact.getSubmittedByUserId(), artifact.getReviewedByStaffId(),
					artifact.getReviewedAt(), artifact.getReviewNotes(), artifact.getCreatedAt());
		}

	}

}
