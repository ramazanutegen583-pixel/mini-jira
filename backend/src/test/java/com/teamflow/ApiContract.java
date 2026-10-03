package com.teamflow;

import com.fasterxml.jackson.databind.*;
import com.teamflow.task.*;
import com.teamflow.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.*;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.jwt.secret=integration-test-secret-at-least-32-bytes-long", "app.bootstrap.email=", "app.bootstrap.password="})
@AutoConfigureMockMvc
abstract class ApiContract {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired TaskRepository tasks;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    String adminToken; String userToken; AppUser member;
    @BeforeEach void setUp() throws Exception {
        tasks.deleteAll(); users.deleteAll();
        String hash = passwords.encode("Test-password-123!");
        users.save(new AppUser("Admin", "admin@example.com", hash, Role.ADMIN));
        member = users.save(new AppUser("Member", "member@example.com", hash, Role.USER));
        adminToken = login("admin@example.com"); userToken = login("member@example.com");
    }
    String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "password", "Test-password-123!"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.user.passwordHash").doesNotExist()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }
    JsonNode createTask() throws Exception {
        String body = mvc.perform(post("/api/tasks").header("Authorization", "Bearer " + userToken).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("title", "API task", "description", "Contract test", "priority", "HIGH", "assigneeId", member.getId()))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }
    @Test void anonymousAndMalformedTokensAreRejectedWithConsistentErrors() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.requestId").isNotEmpty()).andExpect(header().exists("X-Request-ID"));
        mvc.perform(get("/api/tasks").header("Authorization", "Bearer invalid.jwt.token")).andExpect(status().isUnauthorized());
    }
    @Test void expiredTokensAndWrongAudienceAreRejected() throws Exception {
        for (boolean expired : List.of(true, false)) {
            var claims = JwtClaimsSet.builder().issuer("teamflow").subject(member.getId().toString())
                .audience(List.of(expired ? "teamflow-api" : "wrong-api")).issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(expired ? Instant.now().minusSeconds(300) : Instant.now().plusSeconds(300))
                .claim("roles", List.of("USER")).build();
            String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
            mvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
    }
    @Test void loginDoesNotRevealWhetherAccountExists() throws Exception {
        for (String email : List.of("unknown@example.com", "member@example.com")) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", "Wrong-password!"))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Неверный email или пароль"));
        }
    }
    @Test void memberCanCreateAndMoveAcrossAllColumnsButCannotDelete() throws Exception {
        JsonNode task = createTask(); String id = task.get("id").asText(); long version = task.get("version").asLong();
        for (String target : List.of("IN_PROGRESS", "DONE", "TODO")) {
            String result = mvc.perform(patch("/api/tasks/" + id + "/status").header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status", target, "version", version))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(target)).andReturn().getResponse().getContentAsString();
            version = json.readTree(result).get("version").asLong();
        }
        mvc.perform(delete("/api/tasks/" + id).param("version", "" + version).header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/tasks/" + id).param("version", "" + version).header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());
        assertThat(tasks.count()).isZero();
    }
    @Test void staleVersionCannotOverwriteOrDeleteTask() throws Exception {
        JsonNode task = createTask(); String id = task.get("id").asText();
        mvc.perform(patch("/api/tasks/" + id + "/status").header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\",\"version\":0}")).andExpect(status().isOk());
        mvc.perform(patch("/api/tasks/" + id + "/status").header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"TODO\",\"version\":0}")).andExpect(status().isConflict());
        mvc.perform(delete("/api/tasks/" + id).param("version", "0").header("Authorization", "Bearer " + adminToken)).andExpect(status().isConflict());
        assertThat(tasks.findById(UUID.fromString(id)).orElseThrow().getStatus()).isEqualTo(TaskStatus.DONE);
    }
    @Test void updatesArePersistedAndUnknownAssigneeIsRejected() throws Exception {
        JsonNode task = createTask(); String id = task.get("id").asText();
        Map<String,Object> update = new HashMap<>(); update.put("title", "Changed title"); update.put("description", "Changed description");
        update.put("priority", "LOW"); update.put("version", 0); update.put("assigneeId", null); update.put("dueDate", "2027-01-15");
        mvc.perform(put("/api/tasks/" + id).header("Authorization", "Bearer " + userToken).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(update)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Changed title"))
            .andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.assignee").isEmpty());
        update.put("version", 1); update.put("assigneeId", UUID.randomUUID());
        mvc.perform(put("/api/tasks/" + id).header("Authorization", "Bearer " + userToken).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(update))).andExpect(status().isNotFound());
        assertThat(tasks.findById(UUID.fromString(id)).orElseThrow().getTitle()).isEqualTo("Changed title");
    }
    @Test void validatesInputAndRejectsMassAssignment() throws Exception {
        mvc.perform(post("/api/tasks").header("Authorization", "Bearer " + userToken).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\" \",\"description\":\"\",\"priority\":\"LOW\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.title").exists());
        mvc.perform(post("/api/tasks").header("Authorization", "Bearer " + userToken).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Task\",\"description\":\"\",\"priority\":\"LOW\",\"reporterId\":\"fake\"}"))
            .andExpect(status().isBadRequest());
        JsonNode task = createTask();
        mvc.perform(patch("/api/tasks/" + task.get("id").asText() + "/status").header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNKNOWN\",\"version\":0}")).andExpect(status().isBadRequest());
    }
    @Test void onlyAdminCanProvisionUsersAndDuplicateEmailIsConflict() throws Exception {
        String body = "{\"name\":\"New user\",\"email\":\"New@example.com\",\"password\":\"Valid-password-123!\",\"role\":\"USER\"}";
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + userToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("new@example.com")).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        assertThat(users.findByEmail("new@example.com").orElseThrow().getPasswordHash()).startsWith("$2");
    }
    @Test void listsTasksWithBoundedPaginationAndStatuses() throws Exception {
        createTask(); createTask();
        mvc.perform(get("/api/tasks").param("size", "1").param("page", "1").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/tasks").param("size", "101").header("Authorization", "Bearer " + userToken)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/statuses").header("Authorization", "Bearer " + userToken)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
    }
}
