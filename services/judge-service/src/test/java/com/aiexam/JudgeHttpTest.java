package com.aiexam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class JudgeHttpTest {
 @Autowired MockMvc mvc;
 @Test void internalEndpointRejectsMissingToken()throws Exception{mvc.perform(post("/internal/judge").contentType("application/json").content("{\"items\":[]}")).andExpect(status().isForbidden());}
  @Test void authenticatedBatchIsGraded()throws Exception{mvc.perform(post("/internal/judge").header("X-Service-Token","test-service-token").contentType("application/json").content("{\"items\":[{\"type\":\"MULTI\",\"answer\":\"A,C\",\"userAnswer\":\"C,A\"}]}")).andExpect(status().isOk()).andExpect(jsonPath("$.results[0].correct").value(true));}
 @Test void healthIsPublic()throws Exception{mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.service").value("judge-service"));}
}
