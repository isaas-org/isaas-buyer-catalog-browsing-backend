package org.isaas.buyercatalog.company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BuyerUserRepository extends JpaRepository<BuyerUser, UUID> {

	Optional<BuyerUser> findByAuthProviderId(String authProviderId);

	/**
	 * Binds an Auth0 identity to an invited or seeded row that has none yet.
	 * Callers must only pass an email the token marks as verified.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(nativeQuery = true, value = """
			UPDATE buyer_companies.buyer_users
			SET auth_provider_id = :authProviderId
			WHERE lower(email) = lower(:email) AND auth_provider_id IS NULL
			""")
	int bindAuthProviderId(@Param("authProviderId") String authProviderId, @Param("email") String email);

	List<BuyerUser> findByBuyerCompanyIdOrderByCreatedAt(UUID buyerCompanyId);

	Optional<BuyerUser> findByIdAndBuyerCompanyId(UUID id, UUID buyerCompanyId);

	long countByBuyerCompanyIdAndRoleAndStatus(UUID buyerCompanyId, BuyerUserRole role, BuyerUserStatus status);

}
