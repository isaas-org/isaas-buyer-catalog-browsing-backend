package org.isaas.buyercatalog.company;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerCompanyRepository extends JpaRepository<BuyerCompany, UUID> {

	/** Join targets: only active (so verified) companies are listed. */
	List<BuyerCompany> findTop20ByStatusAndNameContainingIgnoreCaseOrderByName(CompanyStatus status,
			String nameFragment);

	List<BuyerCompany> findByVerificationStatusOrderByCreatedAt(VerificationStatus verificationStatus);

}
