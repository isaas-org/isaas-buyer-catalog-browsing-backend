package org.isaas.buyercatalog;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.isaas.buyercatalog.TestData.email;
import static org.isaas.buyercatalog.TestData.login;
import static org.isaas.buyercatalog.TestData.unique;
import static org.isaas.buyercatalog.TestData.unverifiedLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Creating a buyer company, joining one, invites and member management. */
@SpringBootTest
@AutoConfigureMockMvc
class MembershipTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestData data;

	@Test
	void creatingACompanyMakesYouItsAdminAndStartsItPendingAndUnverified() throws Exception {
		String subject = unique("auth0|founder");
		String email = email("founder");
		String name = unique("Acme");

		mvc.perform(post("/companies").with(login(subject, email))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "%s", "adminName": "Ada Founder"}
					""".formatted(name)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("pending"))
			.andExpect(jsonPath("$.verificationStatus").value("unverified"));

		mvc.perform(get("/me").with(login(subject, email)))
			.andExpect(jsonPath("$.membership.role").value("admin"))
			.andExpect(jsonPath("$.membership.companyName").value(name));

		mvc.perform(post("/companies").with(login(subject, email))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Second", "adminName": "Ada"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_A_MEMBER"));
	}

	@Test
	void creatingACompanyNeedsAVerifiedEmail() throws Exception {
		mvc.perform(post("/companies").with(unverifiedLogin(unique("auth0|x"), email("x")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Acme", "adminName": "Ada"}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
	}

	@Test
	void aPendingCompanyCantBeAskedToJoinAndIsntListed() throws Exception {
		UUID pending = data.pendingCompany();
		String subject = unique("auth0|joiner");

		mvc.perform(post("/companies/{id}/join-requests", pending).with(login(subject, email("joiner")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Jo Joiner", "requestedRole": "purchaser"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RULE_VIOLATION"))
			.andExpect(jsonPath("$.detail").value(containsString("join request not allowed")));

		mvc.perform(get("/companies").param("q", "Company").with(login(subject, email("joiner"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", not(hasItem(pending.toString()))));
	}

	@Test
	void anAdminApprovesAJoinRequestAndTheRequesterBecomesAMember() throws Exception {
		UUID company = data.activeCompany();
		String adminSubject = unique("auth0|admin");
		String adminEmail = email("admin");
		data.member(company, adminEmail, "admin", adminSubject);
		String joinerSubject = unique("auth0|joiner");
		String joinerEmail = email("joiner");

		String created = mvc.perform(post("/companies/{id}/join-requests", company)
			.with(login(joinerSubject, joinerEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Jo Joiner", "requestedRole": "admin"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("pending"))
			.andReturn().getResponse().getContentAsString();
		String requestId = JsonPath.read(created, "$.id");

		// The second open request from the same person is refused by the database.
		mvc.perform(post("/companies/{id}/join-requests", company).with(login(joinerSubject, joinerEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Jo Joiner", "requestedRole": "purchaser"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.constraint").value("buyer_join_requests_one_pending"));

		// The admin grants a lower role than requested.
		mvc.perform(post("/company/join-requests/{id}/approve", requestId).with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"role": "viewer"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("viewer"))
			.andExpect(jsonPath("$.signedIn").value(true));

		mvc.perform(get("/me").with(login(joinerSubject, joinerEmail)))
			.andExpect(jsonPath("$.membership.companyId").value(company.toString()))
			.andExpect(jsonPath("$.membership.role").value("viewer"));

		mvc.perform(post("/company/join-requests/{id}/reject", requestId).with(login(adminSubject, adminEmail)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("NOT_PENDING"));
	}

	@Test
	void aPendingCompanysAdminCanInviteAndTheInviteeGetsInOnFirstLogin() throws Exception {
		UUID company = data.pendingCompany();
		String adminSubject = unique("auth0|admin");
		String adminEmail = email("admin");
		data.member(company, adminEmail, "admin", adminSubject);
		String inviteeEmail = email("finance");

		mvc.perform(post("/company/invitations").with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Fin Ance", "email": "%s", "role": "viewer"}
					""".formatted(inviteeEmail)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.signedIn").value(false));

		mvc.perform(get("/me").with(login(unique("auth0|finance"), inviteeEmail)))
			.andExpect(jsonPath("$.membership.companyId").value(company.toString()))
			.andExpect(jsonPath("$.membership.role").value("viewer"));

		// One email, one company.
		mvc.perform(post("/company/invitations").with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Again", "email": "%s", "role": "purchaser"}
					""".formatted(inviteeEmail)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.constraint").value("buyer_users_email_key"));
	}

	@Test
	void onlyAnAdminManagesMembers() throws Exception {
		UUID company = data.activeCompany();
		String salesSubject = unique("auth0|purchaser");
		String salesEmail = email("purchaser");
		data.member(company, salesEmail, "purchaser", salesSubject);

		mvc.perform(post("/company/invitations").with(login(salesSubject, salesEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "X", "email": "%s", "role": "admin"}
					""".formatted(email("x"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ADMIN_REQUIRED"));
	}

	@Test
	void theLastActiveAdminCantBeDemotedOrDisabled() throws Exception {
		UUID company = data.activeCompany();
		String adminSubject = unique("auth0|admin");
		String adminEmail = email("admin");
		UUID admin = data.member(company, adminEmail, "admin", adminSubject);

		mvc.perform(patch("/company/members/{id}", admin).with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"role": "purchaser"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("LAST_ADMIN"));

		UUID second = data.member(company, email("second"), "admin", null);
		mvc.perform(patch("/company/members/{id}", second).with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "disabled"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("disabled"));
	}

	@Test
	void anAdminCantTouchAnotherCompanysMembers() throws Exception {
		UUID companyA = data.activeCompany();
		String adminSubject = unique("auth0|admin");
		String adminEmail = email("admin");
		data.member(companyA, adminEmail, "admin", adminSubject);
		UUID otherMember = data.member(data.activeCompany(), email("other"), "purchaser", null);

		mvc.perform(patch("/company/members/{id}", otherMember).with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "disabled"}
					"""))
			.andExpect(status().isNotFound());
	}

}
