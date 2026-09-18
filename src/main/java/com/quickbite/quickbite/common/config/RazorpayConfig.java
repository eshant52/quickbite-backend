package com.quickbite.quickbite.common.config;

import com.quickbite.quickbite.common.config.property.RazorpayProperties;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Instantiates the Razorpay SDK client as a Spring-managed singleton bean.
 *
 * <p>The {@link RazorpayClient} is thread-safe and can be shared across
 * the entire application context. Credentials are sourced from
 * {@link RazorpayProperties} which reads environment variables via
 * {@code application.properties} (RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET).
 */
@Configuration
public class RazorpayConfig {

    @Bean
    public RazorpayClient razorpayClient(RazorpayProperties razorpayProperties) throws RazorpayException {
        return new RazorpayClient(razorpayProperties.keyId(), razorpayProperties.keySecret());
    }
}
