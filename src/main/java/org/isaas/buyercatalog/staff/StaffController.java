package org.isaas.buyercatalog.staff;

import java.util.List;
import java.util.UUID;

import org.isaas.buyercatalog.identity.CurrentUser;
import org.isaas.buyercatalog.staff.StaffReviewService.CompanyReview;
import org.isaas.buyercatalog.company.ArtifactStatus;
import org.isaas.buyercatalog.company.CompanyStatus;
import org.isaas.buyercatalog.company.CompanyViews.DocumentView;
import org.isaas.buyercatalog.company.CompanyViews.CompanyView;
import org.isaas.buyercatalog.company.VerificationStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Staff endpoints. Used from an API client for now; there is no staff UI yet. */
@RestController
@RequestMapping("/staff")
public class StaffController {

	private final StaffReviewService reviews;

	public StaffController(StaffReviewService reviews) {
		this.reviews = reviews;
	}

	@GetMapping("/companies")
	public List<CompanyView> companies(CurrentUser currentUser,
			@RequestParam(defaultValue = "pending") VerificationStatus verificationStatus) {
		return reviews.companiesByVerification(currentUser, verificationStatus);
	}

	@GetMapping("/companies/{buyerCompanyId}")
	public CompanyReview company(CurrentUser currentUser, @PathVariable UUID buyerCompanyId) {
		return reviews.company(currentUser, buyerCompanyId);
	}

	@PostMapping("/verification-documents/{documentId}/review")
	public DocumentView reviewDocument(CurrentUser currentUser, @PathVariable UUID documentId,
			@Valid @RequestBody DocumentReviewRequest body) {
		return reviews.reviewDocument(currentUser, documentId, body.decision(), body.notes());
	}

	@PostMapping("/companies/{buyerCompanyId}/verification")
	public CompanyView decideVerification(CurrentUser currentUser, @PathVariable UUID buyerCompanyId,
			@Valid @RequestBody VerificationDecisionRequest body) {
		return reviews.decideVerification(currentUser, buyerCompanyId, body.decision());
	}

	@PostMapping("/companies/{buyerCompanyId}/status")
	public CompanyView setStatus(CurrentUser currentUser, @PathVariable UUID buyerCompanyId,
			@Valid @RequestBody StatusRequest body) {
		return reviews.setStatus(currentUser, buyerCompanyId, body.status());
	}

	public record DocumentReviewRequest(@NotNull ArtifactStatus decision, @Size(max = 2000) String notes) {
	}

	public record VerificationDecisionRequest(@NotNull VerificationStatus decision) {
	}

	public record StatusRequest(@NotNull CompanyStatus status) {
	}

}
