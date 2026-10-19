package org.isaas.buyercatalog.company;

import java.util.List;
import java.util.UUID;

import org.isaas.buyercatalog.api.ApiException;
import org.isaas.buyercatalog.company.CompanyViews.CompanySummary;
import org.isaas.buyercatalog.company.CompanyViews.CompanyView;
import org.isaas.buyercatalog.company.CompanyViews.JoinRequestView;
import org.isaas.buyercatalog.identity.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The two ways in for a signed-in person who isn't a member yet: create a new
 * buyer company and become its first admin, or ask to join an existing one.
 */
@Service
public class CompanyOnboardingService {

	private final BuyerCompanyRepository companies;

	private final BuyerUserRepository users;

	private final BuyerJoinRequestRepository joinRequests;

	public CompanyOnboardingService(BuyerCompanyRepository companies, BuyerUserRepository users,
			BuyerJoinRequestRepository joinRequests) {
		this.companies = companies;
		this.users = users;
		this.joinRequests = joinRequests;
	}

	/**
	 * The new company starts pending and unverified. Its members can browse,
	 * keep a watchlist and invite colleagues; buying waits for staff to verify
	 * and activate it.
	 */
	@Transactional
	public CompanyView createCompany(CurrentUser currentUser, String name, String websiteUrl, String adminName) {
		String email = currentUser.requireVerifiedEmail();
		requireUnaffiliated(currentUser);
		BuyerCompany company = companies.saveAndFlush(new BuyerCompany(name, websiteUrl));
		users.saveAndFlush(new BuyerUser(company.getId(), adminName, email, BuyerUserRole.admin,
				currentUser.subject()));
		return CompanyView.from(company);
	}

	@Transactional(readOnly = true)
	public List<CompanySummary> searchJoinable(String nameFragment) {
		return companies.findTop20ByStatusAndNameContainingIgnoreCaseOrderByName(CompanyStatus.active,
				nameFragment.strip())
			.stream()
			.map(CompanySummary::from)
			.toList();
	}

	/** The database rejects a request to a company that isn't active, and a second open request. */
	@Transactional
	public JoinRequestView requestToJoin(CurrentUser currentUser, UUID buyerCompanyId, String name,
			BuyerUserRole requestedRole) {
		String email = currentUser.requireVerifiedEmail();
		requireUnaffiliated(currentUser);
		if (!companies.existsById(buyerCompanyId)) {
			throw ApiException.notFound("Buyer company not found.");
		}
		BuyerJoinRequest request = joinRequests.saveAndFlush(
				BuyerJoinRequest.peerRequest(buyerCompanyId, currentUser.subject(), name, email, requestedRole));
		return JoinRequestView.from(request);
	}

	@Transactional(readOnly = true)
	public List<JoinRequestView> myJoinRequests(CurrentUser currentUser) {
		currentUser.requireLogin();
		return joinRequests.findByAuthProviderIdOrderByCreatedAtDesc(currentUser.subject())
			.stream()
			.map(JoinRequestView::from)
			.toList();
	}

	/** One email, one company: buyer_users.email is unique across companies. */
	private static void requireUnaffiliated(CurrentUser currentUser) {
		if (currentUser.member() != null) {
			throw ApiException.conflict("ALREADY_A_MEMBER", "You already belong to a buyer company.");
		}
	}

}
