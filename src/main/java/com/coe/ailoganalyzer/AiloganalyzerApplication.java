package com.coe.ailoganalyzer;

import com.coe.ailoganalyzer.loganalyzerv2.ai.OllamaConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(OllamaConfig.class)

public class AiloganalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiloganalyzerApplication.class, args);
	}

}
