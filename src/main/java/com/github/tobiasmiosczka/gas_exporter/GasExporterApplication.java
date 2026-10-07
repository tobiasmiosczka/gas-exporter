package com.github.tobiasmiosczka.gas_exporter;

import com.github.tobiasmiosczka.gas_exporter.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class GasExporterApplication {

	public static void main(String[] args) {
		SpringApplication.run(GasExporterApplication.class, args);
	}

}
