package org.isaas.buyercatalog;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.isaas.buyercatalog.TestData.email;
import static org.isaas.buyercatalog.TestData.login;
import static org.isaas.buyercatalog.TestData.unique;
import static org.isaas.buyercatalog.TestData.unverifiedLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Authentication and how a login becomes a member. Runs against the real schema. */
@SpringBootTest
@AutoConfigureMockMvc
class IdentityTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestData data;

	@Test
	void healthNeedsNoToken() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void everythingElseNeedsAToken() throws Exception {
		mvc.perform(get("/me")).andExpect(status().isUnauthorized());
		mvc.perform(get("/company/members")).andExpect(status().isUnauthorized());
	}

	@Test
	void unaffiliatedUserHasNoMembership() throws Exception {
		mvc.perform(get("/me").with(login(unique("auth0|new"), email("new"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticatedWith").value("auth0"))
			.andExpect(jsonPath("$.membership").value(nullValue()))
			.andExpect(jsonPath("$.staff").value(nullValue()));
		mvc.perform(get("/company").with(login(unique("auth0|new"), email("new"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("NOT_A_MEMBER"));
	}

	@Test
	void firstLoginBindsTheInvitedRowByVerifiedEmail() throws Exception {
		UUID company = data.activeCompany();
		String email = email("invited");
		UUID member = data.member(company, email, "purchaser", null);
		String subject = unique("auth0|invited");

		// Email case differs from the row; binding is case-insensitive.
		mvc.perform(get("/me").with(login(subject, email.toUpperCase())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.membership.userId").value(member.toString()))
			.andExpect(jsonPath("$.membership.role").value("purchaser"))
			.andExpect(jsonPath("$.membership.companyStatus").value("active"));
		assertThat(data.authProviderIdOf(member)).isEqualTo(subject);
	}

	@Test
	void anUnverifiedEmailNeverBinds() throws Exception {
		UUID company = data.activeCompany();
		String email = email("unverified");
		UUID member = data.member(company, email, "admin", null);

		mvc.perform(get("/me").with(unverifiedLogin(unique("auth0|attacker"), email)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.membership").value(nullValue()));
		assertThat(data.authProviderIdOf(member)).isNull();
	}

	@Test
	void aBoundRowIsNotTakenOverByAnotherLoginWithTheSameEmail() throws Exception {
		UUID company = data.activeCompany();
		String email = email("owner");
		data.member(company, email, "admin", unique("auth0|owner"));

		mvc.perform(get("/me").with(login(unique("google-oauth2|other"), email)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.membership").value(nullValue()));
	}

	@Test
	void aDisabledMemberIsRefused() throws Exception {
		UUID company = data.activeCompany();
		String subject = unique("auth0|gone");
		UUID member = data.member(company, email("gone"), "admin", subject);
		data.disableMember(member);

		mvc.perform(get("/company").with(login(subject, email("gone"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
	}

	@Test
	void staffAreRecognisedAndBoundLikeMembers() throws Exception {
		String email = email("staff");
		data.staff(email, null, "reviewer");

		mvc.perform(get("/me").with(login(unique("auth0|staff"), email)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.staff.role").value("reviewer"))
			.andExpect(jsonPath("$.membership").value(nullValue()));
	}

}
