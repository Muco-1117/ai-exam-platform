package com.aiexam;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class AiServiceTest {
 @Autowired MockMvc mvc;
 @Test void internalEndpointRequiresToken()throws Exception{mvc.perform(post("/internal/generate").contentType("application/json").content("{}")).andExpect(status().isForbidden());}
  @Test void demoProducesRequestedCount()throws Exception{mvc.perform(post("/internal/generate").header("X-Service-Token","test-service-token").contentType("application/json").content("{\"categoryId\":1,\"type\":\"SINGLE\",\"difficulty\":2,\"count\":3,\"knowledge\":\"微服务\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("DEMO")).andExpect(jsonPath("$.questions.length()").value(3)).andExpect(jsonPath("$.questions[0].answer").value("A"));}
  @Test void rejectsTooManyQuestions()throws Exception{mvc.perform(post("/internal/generate").header("X-Service-Token","test-service-token").contentType("application/json").content("{\"categoryId\":1,\"type\":\"FILL\",\"difficulty\":2,\"count\":11}")).andExpect(status().isBadRequest());}
}
