package com.deskflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deskflow.user.Role;
import com.deskflow.user.User;
import com.deskflow.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Runs the real security filter chain, services and database schema through HTTP requests. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String customerToken;
    private String otherCustomerToken;
    private String agentToken;

    @BeforeEach
    void createUsers() throws Exception {
        customerToken = registerCustomer();
        otherCustomerToken = registerCustomer();
        agentToken = createAgentAndLogin();
    }

    private String registerCustomer() throws Exception {
        String body = """
                {"email":"%s","fullName":"Test Customer","password":"%s"}
                """.formatted(uniqueEmail(), PASSWORD);
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    private String createAgentAndLogin() throws Exception {
        String email = uniqueEmail();
        users.save(new User(email, "Test Agent", passwordEncoder.encode(PASSWORD), Role.AGENT));
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, PASSWORD);
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@deskflow.test";
    }

    private MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private int createTicket(String token) throws Exception {
        String body = """
                {"title":"Refund request","description":"I was charged twice, please refund me"}
                """;
        String response = mvc.perform(as(token, post("/api/tickets")).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.category").value("BILLING"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    void requestsWithoutATokenAreRejected() throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    }

    @Test
    void forgedTokensAreRejected() throws Exception {
        mvc.perform(as("not.a.real.token", get("/api/tickets"))).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        String body = """
                {"email":"nobody@deskflow.test","password":"wrong-password"}
                """;
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTicketsReturnFieldErrors() throws Exception {
        mvc.perform(as(customerToken, post("/api/tickets")).content("{\"title\":\"\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void customersCannotSeeEachOthersTickets() throws Exception {
        int ticketId = createTicket(customerToken);

        mvc.perform(as(otherCustomerToken, get("/api/tickets/" + ticketId))).andExpect(status().isNotFound());
        mvc.perform(as(customerToken, get("/api/tickets/" + ticketId))).andExpect(status().isOk());
        mvc.perform(as(agentToken, get("/api/tickets/" + ticketId))).andExpect(status().isOk());
    }

    @Test
    void customerListOnlyContainsTheirOwnTickets() throws Exception {
        int ticketId = createTicket(customerToken);
        createTicket(otherCustomerToken);

        String response = mvc.perform(as(customerToken, get("/api/tickets")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<java.util.List<Integer>>read(response, "$.items[*].id")).containsExactly(ticketId);
    }

    @Test
    void onlyStaffCanUpdateTickets() throws Exception {
        int ticketId = createTicket(customerToken);

        mvc.perform(as(customerToken, patch("/api/tickets/" + ticketId)).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(as(agentToken, patch("/api/tickets/" + ticketId)).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void invalidStatusChangesReturnConflict() throws Exception {
        int ticketId = createTicket(customerToken);

        mvc.perform(as(agentToken, patch("/api/tickets/" + ticketId)).content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void internalNotesAreHiddenFromCustomers() throws Exception {
        int ticketId = createTicket(customerToken);
        String url = "/api/tickets/" + ticketId + "/comments";

        mvc.perform(as(agentToken, post(url)).content("{\"body\":\"Looks like fraud\",\"internal\":true}"))
                .andExpect(status().isCreated());
        mvc.perform(as(agentToken, post(url)).content("{\"body\":\"We are on it\",\"internal\":false}"))
                .andExpect(status().isCreated());

        mvc.perform(as(customerToken, get(url)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].body").value("We are on it"));
        mvc.perform(as(agentToken, get(url))).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void customersCannotWriteInternalNotes() throws Exception {
        int ticketId = createTicket(customerToken);

        mvc.perform(as(customerToken, post("/api/tickets/" + ticketId + "/comments"))
                        .content("{\"body\":\"Sneaky\",\"internal\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ticketChangesAreRecordedInTheAuditTrail() throws Exception {
        int ticketId = createTicket(customerToken);
        mvc.perform(as(agentToken, patch("/api/tickets/" + ticketId)).content("{\"priority\":\"URGENT\"}"))
                .andExpect(status().isOk());

        mvc.perform(as(agentToken, get("/api/tickets/" + ticketId + "/events")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("PRIORITY_CHANGED"));
    }

    @Test
    void dashboardIsForStaffOnly() throws Exception {
        mvc.perform(as(customerToken, get("/api/dashboard"))).andExpect(status().isForbidden());
        mvc.perform(as(agentToken, get("/api/dashboard"))).andExpect(status().isOk());
    }

    @Test
    void searchMatchesTitleText() throws Exception {
        createTicket(customerToken);

        mvc.perform(as(customerToken, get("/api/tickets").param("q", "REFUND")))
                .andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(as(customerToken, get("/api/tickets").param("q", "no-such-text")))
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}
