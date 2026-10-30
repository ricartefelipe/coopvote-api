package com.felipericarte.coopvote.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.JWKSet;

/**
 * Emite tokens assinados (HS256) com issuer, audience, sujeito, papéis e expiração.
 */
@Service
public class TokenService {

    static final String ROLES_CLAIM = "roles";

    private final AuthProperties properties;
    private final Clock clock;
    private final JwtEncoder encoder;

    public TokenService(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(secretKey(properties)).build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    public IssuedToken issue(Associate associate) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.tokenTtl());
        List<String> roles = associate.getRoles().stream().map(Enum::name).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .subject(associate.getAssociateId())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(ROLES_CLAIM, roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt, roles);
    }

    static SecretKey secretKey(AuthProperties properties) {
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public record IssuedToken(String token, Instant expiresAt, List<String> roles) {
    }
}
