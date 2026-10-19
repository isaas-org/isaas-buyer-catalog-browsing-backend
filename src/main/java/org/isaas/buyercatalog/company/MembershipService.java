package org.isaas.buyercatalog.company;

import java.util.List;
import java.util.UUID;

import org.isaas.buyercatalog.api.ApiException;
import org.isaas.buyercatalog.identity.CurrentUser;
import org.isaas.buyercatalog.company.CompanyViews.DocumentView;
import org.isaas.buyercatalog.company.CompanyViews.JoinRequestView;
import org.isaas.buyercatalog.company.CompanyViews.MemberView;
import org.isaas.buyercatalog.company.CompanyViews.CompanyView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything a member does within their own company. The company is always the
 * caller's own (from their buyer_users row); a request never names it.
 */
@Service
public class MembershipService {

	private final BuyerCompanyRepository companies;

	private final BuyerUserRepository users;

	private final BuyerJoinRequestRepository joinRequests;

	private final VerificationArtifactRepository documents;

	public MembershipService(BuyerCompanyRepository companies, BuyerUserRepository users,
			BuyerJoinRequestRepository joinRequests, VerificationArtifactRepository documents) {
		this.companies = companies;
		this.users = users;
		this.joinRequests = joinRequests;
		this.documents = documents;
	}

	@Transactional(readOnly = true)
	public CompanyView myCompany(CurrentUser currentUser) {
		return CompanyView.from(companies.findById(currentUser.requireMember().getBuyerCompanyId()).orElseThrow());
	}

	@Transactional(readOnly = true)
	public List<MemberView> members(CurrentUser currentUser) {
		return users.findByBuyerCompanyIdOrderByCreatedAt(currentUser.requireMember().getBuyerCompanyId())
			.stream()
			.map(MemberView::from)
			.toList();
	}

	/**
	 * Adds a member by email. Works while the company is pending, so the person
	 * holding the verification documents can be brought in. They get access
	 * when they first log in with that email, verified.
	 */
	@Transactional
	public MemberView invite(CurrentUser currentUser, String name, String email, BuyerUserRole role) {
		BuyerUser admin = currentUser.requireAdmin();
		BuyerUser invited = users.saveAndFlush(new BuyerUser(admin.getBuyerCompanyId(), name, email, role, null));
		joinRequests.saveAndFlush(BuyerJoinRequest.invite(admin.getBuyerCompanyId(), name, email, role, admin.getId()));
		return MemberView.from(invited);
	}

	/** Changes a member's role or status. A company always keeps at least one active admin. */
	@Transactional
	public MemberView updateMember(CurrentUser currentUser, UUID memberId, BuyerUserRole role,
			BuyerUserStatus status) {
		BuyerUser admin = currentUser.requireAdmin();
		BuyerUser member = users.findByIdAndBuyerCompanyId(memberId, admin.getBuyerCompanyId())
			.orElseThrow(() -> ApiException.notFound("Member not found."));
		boolean losesAdmin = member.isActiveAdmin() && ((role != null && role != BuyerUserRole.admin)
				|| (status != null && status != BuyerUserStatus.active));
		if (losesAdmin && users.countByBuyerCompanyIdAndRoleAndStatus(admin.getBuyerCompanyId(), BuyerUserRole.admin,
				BuyerUserStatus.active) <= 1) {
			throw ApiException.conflict("LAST_ADMIN", "A company must keep at least one active admin.");
		}
		if (role != null) {
			member.setRole(role);
		}
		if (status != null) {
			member.setStatus(status);
		}
		return MemberView.from(users.saveAndFlush(member));
	}

	@Transactional(readOnly = true)
	public List<JoinRequestView> joinRequests(CurrentUser currentUser, JoinRequestStatus status) {
		UUID buyerCompanyId = currentUser.requireAdmin().getBuyerCompanyId();
		return joinRequests.findByBuyerCompanyIdAndStatusOrderByCreatedAt(buyerCompanyId, status)
			.stream()
			.map(JoinRequestView::from)
			.toList();
	}

	/** Approval creates the member row; role defaults to the requested one. */
	@Transactional
	public MemberView approve(CurrentUser currentUser, UUID requestId, BuyerUserRole role) {
		BuyerUser admin = currentUser.requireAdmin();
		BuyerJoinRequest request = pendingRequest(admin, requestId);
		BuyerUserRole granted = role != null ? role : request.getRequestedRole();
		BuyerUser member = users.saveAndFlush(new BuyerUser(admin.getBuyerCompanyId(), request.getName(),
				request.getEmail(), granted, request.getAuthProviderId()));
		request.decide(JoinRequestStatus.approved, admin.getId(), null);
		joinRequests.saveAndFlush(request);
		return MemberView.from(member);
	}

	@Transactional
	public JoinRequestView reject(CurrentUser currentUser, UUID requestId, String notes) {
		BuyerUser admin = currentUser.requireAdmin();
		BuyerJoinRequest request = pendingRequest(admin, requestId);
		request.decide(JoinRequestStatus.rejected, admin.getId(), notes);
		return JoinRequestView.from(joinRequests.saveAndFlush(request));
	}

	@Transactional(readOnly = true)
	public List<DocumentView> documents(CurrentUser currentUser) {
		return documents.findByBuyerCompanyIdOrderByCreatedAt(currentUser.requireMember().getBuyerCompanyId())
			.stream()
			.map(DocumentView::from)
			.toList();
	}

	/** The first document moves an unverified company to 'pending', so staff see it in their queue. */
	@Transactional
	public DocumentView submitDocument(CurrentUser currentUser, ArtifactType type, String fileUrl) {
		BuyerUser admin = currentUser.requireAdmin();
		VerificationArtifact document = documents
			.saveAndFlush(new VerificationArtifact(admin.getBuyerCompanyId(), type, fileUrl, admin.getId()));
		BuyerCompany company = companies.findById(admin.getBuyerCompanyId()).orElseThrow();
		company.markSubmittedForReview();
		companies.saveAndFlush(company);
		return DocumentView.from(document);
	}

	private BuyerJoinRequest pendingRequest(BuyerUser admin, UUID requestId) {
		BuyerJoinRequest request = joinRequests.findByIdAndBuyerCompanyId(requestId, admin.getBuyerCompanyId())
			.orElseThrow(() -> ApiException.notFound("Join request not found."));
		if (request.getStatus() != JoinRequestStatus.pending) {
			throw ApiException.conflict("NOT_PENDING", "This join request has already been decided.");
		}
		return request;
	}

}
