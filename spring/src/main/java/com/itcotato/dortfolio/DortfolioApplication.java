package com.itcotato.dortfolio;

import com.itcotato.dortfolio.global.lifecycle.EmbeddingBatchApplicationExit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class DortfolioApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context =
			SpringApplication.run(DortfolioApplication.class, args);

		EmbeddingBatchApplicationExit.exitIfEnabled(context, System::exit);
	}

}
