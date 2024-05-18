package com.arlias.quarkus_crudify.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

@ApplicationScoped
@Slf4j
public class JWTGenerator {

    @ConfigProperty(name = "crudify.server.jwt.secret", defaultValue = "13a4-419b-813c-d2e326102dd7-2e408df8-13a4-419b-813c-d2e326102dd7")
    String secret;


    public synchronized <T> T getClaim(RoutingContext rc, String claim, Class<T> clazz){
        String token = rc.request().getHeader("Auth");
        Jwe<Claims> decodedToken = decodeClaims(token);
        return decodedToken.getPayload().get(claim, clazz);
    }

    public synchronized <T> T getClaim(RoutingExchange ex, String claim, Class<T> clazz){
        String token = ex.request().getHeader("Auth");
        Jwe<Claims> decodedToken = decodeClaims(token);
        return decodedToken.getPayload().get(claim, clazz);
    }

    public synchronized <T> T getClaim(RoutingContext rc, String tokenHeaderName, String claim, Class<T> clazz){
        String token = rc.request().getHeader(tokenHeaderName);
        Jwe<Claims> decodedToken = decodeClaims(token);
        return decodedToken.getPayload().get(claim, clazz);
    }

    public synchronized <T> T getClaim(RoutingExchange ex, String tokenHeaderName, String claim, Class<T> clazz){
        String token = ex.request().getHeader(tokenHeaderName);
        Jwe<Claims> decodedToken = decodeClaims(token);
        return decodedToken.getPayload().get(claim, clazz);
    }

    public synchronized String getToken(Object data, String... excludedKeys) {
        LocalDateTime now = LocalDateTime.now();
        JwtBuilder jwtBuilder = Jwts.builder()
                .issuer("ArliasCrudify")
                .subject("arliasjwt")
                .issuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
                .expiration(Date.from(now.plus(30, ChronoUnit.MINUTES).atZone(ZoneId.systemDefault()).toInstant()))
                .encryptWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.ENC.A256CBC_HS512);

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
                .encryptWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.ENC.A256CBC_HS512);

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
                .encryptWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.ENC.A256CBC_HS512);

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
                .encryptWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.ENC.A256CBC_HS512);

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
                .encryptWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.ENC.A256CBC_HS512).compact();
    }


    public synchronized Jwt decode(String token) {
        JwtParser jwtParser = Jwts.parser()
                .decryptWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build();
        try {
            return jwtParser.parseEncryptedContent(token);
        } catch (ExpiredJwtException | MalformedJwtException | SecurityException | IllegalArgumentException e) {
            log.error("Token not valid", e);
            return null;
        }
    }

    public synchronized Jwe<Claims> decodeClaims(String token) {
        JwtParser jwtParser = Jwts.parser()
                .decryptWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build();
        try {
            return jwtParser.parseEncryptedClaims(token);
        } catch (ExpiredJwtException | MalformedJwtException | SecurityException | IllegalArgumentException e) {
            log.error("Token not valid", e);
            return null;
        }
    }

    public synchronized boolean isValidToken(String token) {
        JwtParser jwtParser = Jwts.parser()
                .decryptWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build();
        try {
            Jwe<Claims> decodedToken = jwtParser.parseEncryptedClaims(token);
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
