package com.bookflow.backend.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

	@Bean
	Clock businessClock(
			@Value("${bookflow.business-time-zone:UTC}") String businessTimeZone) {
		return Clock.system(ZoneId.of(businessTimeZone));
	}
}
