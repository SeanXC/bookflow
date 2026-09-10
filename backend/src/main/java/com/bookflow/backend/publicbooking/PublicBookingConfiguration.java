package com.bookflow.backend.publicbooking;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PublicBookingRateLimitProperties.class)
public class PublicBookingConfiguration {
}
