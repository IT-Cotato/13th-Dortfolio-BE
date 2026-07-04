package com.itcotato.dortfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class DortfolioApplication {

	public static void main(String[] args) {
		SpringApplication.run(DortfolioApplication.class, args);
	}

}
