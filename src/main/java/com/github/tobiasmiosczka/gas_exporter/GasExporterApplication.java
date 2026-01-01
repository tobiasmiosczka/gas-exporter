package com.github.tobiasmiosczka.gas_exporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GasExporterApplication {

	public static void main(String[] args) {
		SpringApplication.run(GasExporterApplication.class, args);
	}

}
