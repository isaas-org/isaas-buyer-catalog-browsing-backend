package org.isaas.buyercatalog.accesstoken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BuyerAccessTokenRepository extends JpaRepository<BuyerAccessToken, UUID> {

	Optional<BuyerAccessToken> findByTokenHash(byte[] tokenHash);

	List<BuyerAccessToken> findByBuyerUserIdOrderByCreatedAtDesc(UUID buyerUserId);

	Optional<BuyerAccessToken> findByIdAndBuyerUserId(UUID id, UUID buyerUserId);

	/** Records use at most once a minute per token, so every request doesn't write. */
	@Modifying
	@Query(nativeQuery = true, value = """
			UPDATE buyer_companies.buyer_access_tokens
			SET last_used_at = now()
			WHERE id = :id AND (last_used_at IS NULL OR last_used_at < now() - interval '1 minute')
			""")
	int touch(@Param("id") UUID id);

}
