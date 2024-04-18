package com.arlias.quarkus_crudify.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import io.jsonwebtoken.security.SignatureAlgorithm;
import io.vertx.core.MultiMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Slf4j
public class JWTGenerator {

    @ConfigProperty(name = "crudify.server.jwt.secret", defaultValue = "2e408df8-13a4-419b-813c-d2e326102dd7-2e408df8-13a4-419b-813c-d2e326102dd7-2e408df8-13a4-419b-813c-d2e326102dd7")
    String secret;

    public synchronized String getToken(Object data, String... excludedKeys) {
        LocalDateTime now = LocalDateTime.now();
        JwtBuilder jwtBuilder = Jwts.builder()
                .issuer("ArliasCrudify")
                .subject("arliasjwt")
                .issuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
                .expiration(Date.from(now.plus(30, ChronoUnit.MINUTES).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)));

        LinkedHashMap<String, Object> dataParsed = new ObjectMapper().convertValue(data, LinkedHashMap.class);

        for (String key : dataParsed.keySet()) {
            if (!List.of(excludedKeys).contains(key))
                jwtBuilder.claim(key, dataParsed.get(key));
        }

        return jwtBuilder.compact();
    }

    public synchronized String getToken(Object data, Integer expireAfter, ChronoUnit timeUnit, String... excludedKeys) {
        LocalDateTime now = LocalDateTime.now();
        JwtBuilder jwtBuilder = Jwts.builder()
                .issuer("ArliasCrudify")
                .subject("arliasjwt")
                .issuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)));

        if (expireAfter == null) {
            jwtBuilder = jwtBuilder.expiration(Date.from(now.plus(30, ChronoUnit.MINUTES).atZone(ZoneId.systemDefault()).toInstant()));
        } else if (expireAfter == -1) {
            jwtBuilder = jwtBuilder.expiration(Date.from(now.plus(1, ChronoUnit.YEARS).atZone(ZoneId.systemDefault()).toInstant()));
        } else {
            jwtBuilder = jwtBuilder.expiration(Date.from(now.plus(expireAfter, timeUnit).atZone(ZoneId.systemDefault()).toInstant()));
        }

        LinkedHashMap<String, Object> dataParsed = new ObjectMapper().convertValue(data, LinkedHashMap.class);
        for (String key : dataParsed.keySet()) {
            if (!List.of(excludedKeys).contains(key))
                jwtBuilder.claim(key, dataParsed.get(key));
        }

        return jwtBuilder.compact();
    }


    public synchronized String getGuestToken() {
        LocalDateTime now = LocalDateTime.now();
        JwtBuilder jwtBuilder = Jwts.builder()
                .issuer("ArliasCrudify")
                .subject("arliasjwt")
                .issuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
                .expiration(Date.from(now.plus(1, ChronoUnit.YEARS).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)));

        jwtBuilder.claim("metaType", "Guest");

        return jwtBuilder.compact();
    }


    public synchronized String getGuestToken(Object data, String... excludedKeys) {
        LocalDateTime now = LocalDateTime.now();
        JwtBuilder jwtBuilder = Jwts.builder()
                .issuer("ArliasCrudify")
                .subject("arliasjwt")
                .issuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
                .expiration(Date.from(now.plus(1, ChronoUnit.YEARS).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)));

        LinkedHashMap<String, Object> dataParsed = new ObjectMapper().convertValue(data, LinkedHashMap.class);

        for (String key : dataParsed.keySet()) {
            if (!List.of(excludedKeys).contains(key))
                jwtBuilder.claim(key, dataParsed.get(key));
        }

        return jwtBuilder.compact();
    }


    public synchronized String getSimpleToken() {
        LocalDateTime now = LocalDateTime.now();
        return Jwts.builder()
                .issuer("ArliasCrudify")
                .subject("msilverman")
                .issuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
                .expiration(Date.from(now.plus(1, ChronoUnit.YEARS).atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }


    public synchronized Jwt decode(String token) {
        JwtParser jwtParser = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build();
        try {
            return jwtParser.parse(token);
        } catch (ExpiredJwtException | MalformedJwtException | SecurityException | IllegalArgumentException e) {
            log.error("Token not valid", e);
            return null;
        }
    }

    public synchronized Jws<Claims> decodeClaims(String token) {
        JwtParser jwtParser = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build();
        try {
            return jwtParser.parseSignedClaims(token);
        } catch (ExpiredJwtException | MalformedJwtException | SecurityException | IllegalArgumentException e) {
            log.error("Token not valid", e);
            return null;
        }
    }

    public synchronized boolean isValidToken(String token) {
        JwtParser jwtParser = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build();
        try {
            Jws<Claims> decodedToken = jwtParser.parseSignedClaims(token);
            if (decodedToken != null) {
                Long exp = decodedToken.getPayload().get("exp", Long.class);
                if (exp > new Date().getTime()) {
                    return true;
                }
            }
        } catch (ExpiredJwtException | MalformedJwtException | SecurityException | IllegalArgumentException e) {
            log.error("Token not valid", e);
        }
        return false;
    }

}
