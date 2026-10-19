package org.isaas.buyercatalog.company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerJoinRequestRepository extends JpaRepository<BuyerJoinRequest, UUID> {

	List<BuyerJoinRequest> findByBuyerCompanyIdAndStatusOrderByCreatedAt(UUID buyerCompanyId, JoinRequestStatus status);

	Optional<BuyerJoinRequest> findByIdAndBuyerCompanyId(UUID id, UUID buyerCompanyId);

	List<BuyerJoinRequest> findByAuthProviderIdOrderByCreatedAtDesc(String authProviderId);

}
