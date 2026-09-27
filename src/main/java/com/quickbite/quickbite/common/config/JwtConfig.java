package com.quickbite.quickbite.common.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.quickbite.quickbite.common.config.property.AuthProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads the RSA key pair used to sign and verify JWTs.
 *
 * <p>Key values ({@code JWT_SECRET}, {@code JWT_PUBLIC_KEY}) are resolved in two ways:
 * <ul>
 *   <li><b>Resource URI</b> (local dev): {@code classpath:certs/private.pem} or
 *       {@code file:/path/to/private.pem}. Spring's {@link DefaultResourceLoader} handles these.</li>
 *   <li><b>Raw PEM string</b> (production / AWS Secrets Manager): The entire PEM content
 *       (including headers) is injected directly as an environment variable.
 *       Useful when secrets are mounted as env vars via ECS task definitions.</li>
 * </ul>
 */
@Configuration
public class JwtConfig {
    private final AuthProperties authProperties;

    public JwtConfig(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey() throws Exception {
        String pem = loadPemContent(authProperties.jwt().privateKey());
        String key = stripPemHeaders(pem);
        byte[] decodedKey = Base64.getDecoder().decode(key);
        return (RSAPrivateKey) KeyFactory
                .getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
    }

    @Bean
    public RSAPublicKey rsaPublicKey() throws Exception {
        String pem = loadPemContent(authProperties.jwt().publicKey());
        String key = stripPemHeaders(pem);
        byte[] decodedKey = Base64.getDecoder().decode(key);
        return (RSAPublicKey) KeyFactory
                .getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decodedKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAPublicKey rsaPublicKey, RSAPrivateKey rsaPrivateKey) {
        JWK jwk = new RSAKey.Builder(rsaPublicKey)
                .privateKey(rsaPrivateKey)
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey rsaPublicKey) {
        return NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
    }

    /**
     * Loads PEM content from either a Spring resource URI or a raw PEM string.
     *
     * <p>If the value starts with {@code classpath:}, {@code file:}, or {@code http},
     * it is treated as a resource URI and loaded via Spring's {@link DefaultResourceLoader}.
     * Otherwise, the value itself is returned as-is (raw PEM / env var injection).
     */
    private String loadPemContent(String value) throws Exception {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT key value must not be blank");
        }
        if (value.startsWith("classpath:") || value.startsWith("file:") || value.startsWith("http")) {
            Resource resource = new DefaultResourceLoader().getResource(value);
            return new String(resource.getInputStream().readAllBytes());
        }
        // Raw PEM string injected directly (e.g., from AWS Secrets Manager env var)
        return value;
    }

    /**
     * Strips PEM headers/footers and whitespace, returning a clean Base64-encoded string
     * ready for {@link Base64#getDecoder()}.
     */
    private String stripPemHeaders(String pem) {
        return pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }
}
