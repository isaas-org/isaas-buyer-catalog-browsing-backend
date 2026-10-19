package org.isaas.buyercatalog.identity;

import java.util.UUID;

import org.isaas.buyercatalog.company.BuyerCompany;
import org.isaas.buyercatalog.company.BuyerCompanyRepository;
import org.isaas.buyercatalog.company.BuyerUser;
import org.isaas.buyercatalog.company.BuyerUserRole;
import org.isaas.buyercatalog.company.BuyerUserStatus;
import org.isaas.buyercatalog.company.CompanyStatus;
import org.isaas.buyercatalog.company.VerificationStatus;
import org.isaas.buyercatalog.staff.StaffRole;
import org.isaas.buyercatalog.staff.StaffUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Who the caller is. Frontends use it to route an unaffiliated user to
 * onboarding; agent apps use it for their whoami tool.
 */
@RestController
public class MeController {

	private final BuyerCompanyRepository companies;

	public MeController(BuyerCompanyRepository companies) {
		this.companies = companies;
	}

	@GetMapping("/me")
	public MeResponse me(CurrentUser currentUser) {
		return new MeResponse(currentUser.subject(), currentUser.email(), currentUser.emailVerified(),
				currentUser.via(), membership(currentUser.member()), staff(currentUser.staff()));
	}

	private Membership membership(BuyerUser member) {
		if (member == null) {
			return null;
		}
		BuyerCompany company = companies.findById(member.getBuyerCompanyId()).orElseThrow();
		return new Membership(member.getId(), member.getRole(), member.getStatus(), company.getId(),
				company.getName(), company.getStatus(), company.getVerificationStatus());
	}

	private static Staff staff(StaffUser staffUser) {
		return staffUser == null ? null : new Staff(staffUser.getId(), staffUser.getRole(), staffUser.isActive());
	}

	public record MeResponse(String subject, String email, boolean emailVerified, CurrentUser.Via authenticatedWith,
			Membership membership, Staff staff) {
	}

	public record Membership(UUID userId, BuyerUserRole role, BuyerUserStatus status, UUID companyId,
			String companyName, CompanyStatus companyStatus, VerificationStatus verificationStatus) {
	}

	public record Staff(UUID staffId, StaffRole role, boolean active) {
	}

}
