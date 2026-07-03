package com.itcotato.dortfolio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class DortfolioApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void swaggerApiDocsLoads() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk());
	}

	@Test
	void swaggerUiLoads() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk());
	}

}
