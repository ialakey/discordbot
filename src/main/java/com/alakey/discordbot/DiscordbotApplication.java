package com.alakey.discordbot;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Discord bot", version = "1.0", description = "Democracy"))
public class DiscordbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscordbotApplication.class, args);
	}

}
