package cl.udec.cocinavecina.components;

import cl.udec.cocinavecina.dto.Auth;
import cl.udec.cocinavecina.model.AccountStatus;
import cl.udec.cocinavecina.model.AppUser;
import cl.udec.cocinavecina.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class TokenIssuer {
    private final JwtEncoder encoder;
    private final Duration ttl;

    public TokenIssuer(JwtEncoder encoder, @Value("${cocinavecina.jwt.access-token-ttl}") Duration ttl) {
        this.encoder = encoder;
        this.ttl = ttl;
    }

    public Auth.TokenResponse issue(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("cocinavecina")
            .issuedAt(now)
            .expiresAt(now.plus(ttl))
            .subject(String.valueOf(user.getId()))
            .claim("roles", roles(user.getRole()))
            .build();
        String token = encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new Auth.TokenResponse(
            token, ttl.toSeconds(), user.getId(), user.getRole(), user.getStatus() == AccountStatus.ACTIVE);
    }

    private static List<String> roles(Role role) {
        return role == Role.SELLER ? List.of(Role.SELLER.name(), Role.BUYER.name()) : List.of(role.name());
    }
}
