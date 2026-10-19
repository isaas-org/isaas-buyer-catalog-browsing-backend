package org.isaas.buyercatalog.company;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationArtifactRepository extends JpaRepository<VerificationArtifact, UUID> {

	List<VerificationArtifact> findByBuyerCompanyIdOrderByCreatedAt(UUID buyerCompanyId);

}
