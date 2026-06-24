package com.swedapp.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.swedapp.bank.config.ExchangeProperties;

@SpringBootApplication
@EnableConfigurationProperties(ExchangeProperties.class)
public class App {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

}
