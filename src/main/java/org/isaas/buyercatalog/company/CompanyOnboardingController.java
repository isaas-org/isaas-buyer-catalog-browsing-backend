package org.isaas.buyercatalog.company;

import java.util.List;
import java.util.UUID;

import org.isaas.buyercatalog.company.CompanyViews.CompanySummary;
import org.isaas.buyercatalog.company.CompanyViews.CompanyView;
import org.isaas.buyercatalog.company.CompanyViews.JoinRequestView;
import org.isaas.buyercatalog.identity.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
public class CompanyOnboardingController {

	private final CompanyOnboardingService onboarding;

	public CompanyOnboardingController(CompanyOnboardingService onboarding) {
		this.onboarding = onboarding;
	}

	@PostMapping("/companies")
	@ResponseStatus(HttpStatus.CREATED)
	public CompanyView createCompany(CurrentUser currentUser, @Valid @RequestBody CreateCompanyRequest body) {
		return onboarding.createCompany(currentUser, body.name(), body.websiteUrl(), body.adminName());
	}

	@GetMapping("/companies")
	public List<CompanySummary> searchJoinable(@RequestParam(name = "q", defaultValue = "") String query) {
		return onboarding.searchJoinable(query);
	}

	@PostMapping("/companies/{buyerCompanyId}/join-requests")
	@ResponseStatus(HttpStatus.CREATED)
	public JoinRequestView requestToJoin(CurrentUser currentUser, @PathVariable UUID buyerCompanyId,
			@Valid @RequestBody JoinCompanyRequest body) {
		return onboarding.requestToJoin(currentUser, buyerCompanyId, body.name(), body.requestedRole());
	}

	@GetMapping("/me/join-requests")
	public List<JoinRequestView> myJoinRequests(CurrentUser currentUser) {
		return onboarding.myJoinRequests(currentUser);
	}

	public record CreateCompanyRequest(@NotBlank @Size(max = 200) String name, @Size(max = 500) String websiteUrl,
			@NotBlank @Size(max = 200) String adminName) {
	}

	public record JoinCompanyRequest(@NotBlank @Size(max = 200) String name, @NotNull BuyerUserRole requestedRole) {
	}

}
