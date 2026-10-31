package org.isaas.buyercatalog.staff;

import java.util.List;
import java.util.UUID;

import org.isaas.buyercatalog.api.ApiException;
import org.isaas.buyercatalog.identity.CurrentUser;
import org.isaas.buyercatalog.company.ArtifactStatus;
import org.isaas.buyercatalog.company.BuyerCompany;
import org.isaas.buyercatalog.company.BuyerCompanyRepository;
import org.isaas.buyercatalog.company.CompanyStatus;
import org.isaas.buyercatalog.company.CompanyViews.DocumentView;
import org.isaas.buyercatalog.company.CompanyViews.CompanyView;
import org.isaas.buyercatalog.company.VerificationArtifact;
import org.isaas.buyercatalog.company.VerificationArtifactRepository;
import org.isaas.buyercatalog.company.VerificationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff review of buyer company verification: check each document, decide
 * verification, then activate. Verification doesn't activate a company by itself, and the
 * database refuses an active company that isn't verified.
 */
@Service
public class StaffReviewService {

	private final BuyerCompanyRepository companies;

	private final VerificationArtifactRepository documents;

	public StaffReviewService(BuyerCompanyRepository companies, VerificationArtifactRepository documents) {
		this.companies = companies;
		this.documents = documents;
	}

	@Transactional(readOnly = true)
	public List<CompanyView> companiesByVerification(CurrentUser currentUser, VerificationStatus status) {
		currentUser.requireStaff();
		return companies.findByVerificationStatusOrderByCreatedAt(status).stream().map(CompanyView::from).toList();
	}

	@Transactional(readOnly = true)
	public CompanyReview company(CurrentUser currentUser, UUID buyerCompanyId) {
		currentUser.requireStaff();
		BuyerCompany company = findCompany(buyerCompanyId);
		List<DocumentView> companyDocuments = documents.findByBuyerCompanyIdOrderByCreatedAt(buyerCompanyId)
			.stream()
			.map(DocumentView::from)
			.toList();
		return new CompanyReview(CompanyView.from(company), companyDocuments);
	}

	@Transactional
	public DocumentView reviewDocument(CurrentUser currentUser, UUID documentId, ArtifactStatus decision,
			String notes) {
		StaffUser staff = currentUser.requireStaff();
		if (decision == ArtifactStatus.pending) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DECISION", "Decide approved or rejected.");
		}
		VerificationArtifact document = documents.findById(documentId)
			.orElseThrow(() -> ApiException.notFound("Document not found."));
		document.review(decision, staff.getId(), notes);
		return DocumentView.from(documents.saveAndFlush(document));
	}

	@Transactional
	public CompanyView decideVerification(CurrentUser currentUser, UUID buyerCompanyId, VerificationStatus decision) {
		StaffUser staff = currentUser.requireStaff();
		BuyerCompany company = findCompany(buyerCompanyId);
		company.decideVerification(decision, staff.getId());
		return CompanyView.from(companies.saveAndFlush(company));
	}

	@Transactional
	public CompanyView setStatus(CurrentUser currentUser, UUID buyerCompanyId, CompanyStatus status) {
		currentUser.requireStaff();
		BuyerCompany company = findCompany(buyerCompanyId);
		company.setStatus(status);
		return CompanyView.from(companies.saveAndFlush(company));
	}

	private BuyerCompany findCompany(UUID buyerCompanyId) {
		return companies.findById(buyerCompanyId)
			.orElseThrow(() -> ApiException.notFound("Buyer company not found."));
	}

	public record CompanyReview(CompanyView company, List<DocumentView> documents) {
	}

}
