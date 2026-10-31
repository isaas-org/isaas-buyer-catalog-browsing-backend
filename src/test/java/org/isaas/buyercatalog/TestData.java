package org.isaas.buyercatalog;

import java.util.UUID;

import org.isaas.buyercatalog.security.Auth0Claims;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Rows for integration tests, written straight to the real schema. Tests
 * don't share data: every row gets a unique suffix, so tests can run against
 * one database without cleanup.
 */
@Component
public class TestData {

	private final JdbcTemplate jdbc;

	public TestData(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public static String unique(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	public static String email(String name) {
		return unique(name) + "@test.test";
	}

	/** An Auth0 login with a verified email. */
	public static RequestPostProcessor login(String subject, String email) {
		return jwt().jwt((token) -> token.subject(subject)
			.claim(Auth0Claims.EMAIL, email)
			.claim(Auth0Claims.EMAIL_VERIFIED, true));
	}

	public static RequestPostProcessor unverifiedLogin(String subject, String email) {
		return jwt().jwt((token) -> token.subject(subject)
			.claim(Auth0Claims.EMAIL, email)
			.claim(Auth0Claims.EMAIL_VERIFIED, false));
	}

	public UUID activeCompany() {
		return company("active", "verified");
	}

	public UUID pendingCompany() {
		return company("pending", "pending");
	}

	public UUID company(String status, String verificationStatus) {
		return jdbc.queryForObject("""
				INSERT INTO buyer_companies.buyer_companies (name, status, verification_status)
				VALUES (?, ?::buyer_companies.company_status, ?::buyer_companies.verification_status)
				RETURNING id
				""", UUID.class, unique("Company"), status, verificationStatus);
	}

	public UUID member(UUID buyerCompanyId, String email, String role, String authProviderId) {
		return jdbc.queryForObject("""
				INSERT INTO buyer_companies.buyer_users (buyer_company_id, name, email, role, auth_provider_id)
				VALUES (?, 'Test Member', ?, ?::buyer_companies.buyer_user_role, ?)
				RETURNING id
				""", UUID.class, buyerCompanyId, email, role, authProviderId);
	}

	public UUID staff(String email, String authProviderId, String role) {
		return jdbc.queryForObject("""
				INSERT INTO platform.staff_users (name, email, role, auth_provider_id)
				VALUES ('Test Staff', ?, ?::platform.staff_role, ?)
				RETURNING id
				""", UUID.class, email, role, authProviderId);
	}

	public String authProviderIdOf(UUID buyerUserId) {
		return jdbc.queryForObject("SELECT auth_provider_id FROM buyer_companies.buyer_users WHERE id = ?",
				String.class, buyerUserId);
	}

	public String verificationStatusOf(UUID buyerCompanyId) {
		return jdbc.queryForObject(
				"SELECT verification_status::text FROM buyer_companies.buyer_companies WHERE id = ?", String.class,
				buyerCompanyId);
	}

	public void disableMember(UUID buyerUserId) {
		jdbc.update("UPDATE buyer_companies.buyer_users SET status = 'disabled' WHERE id = ?", buyerUserId);
	}

}
