package com.arlias.quarkus_crudify.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class JWTGenerator {

    @ConfigProperty(name = "crudify.server.jwt.secret", defaultValue = "2e408df8-13a4-419b-813c-d2e326102dd7-2e408df8-13a4-419b-813c-d2e326102dd7-2e408df8-13a4-419b-813c-d2e326102dd7")
    String secret;

    public synchronized String getToken(Object data, String...excludedKeys){
        Date now = new Date();
        JwtBuilder jwtBuilder = Jwts.builder()
                .issuer("ArliasCrudify")
                .subject("arliasjwt")
                .issuedAt(Date.from(Instant.ofEpochSecond(now.getTime())))
                .expiration(Date.from(Instant.ofEpochSecond(now.getTime() + 1800000L)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512);

        LinkedHashMap<String, Object> dataParsed = new ObjectMapper().convertValue(data, LinkedHashMap.class);

        for (String key : dataParsed.keySet()) {
            if(!List.of(excludedKeys).contains(key))
                jwtBuilder.claim(key, dataParsed.get(key));
        }

        return jwtBuilder.compact();
    }

    public synchronized String getToken(Object data, Long expireAfter, String...excludedKeys){
        Date now = new Date();
        JwtBuilder jwtBuilder = Jwts.builder()
                .issuer("ArliasCrudify")
                .subject("arliasjwt")
                .issuedAt(Date.from(Instant.ofEpochSecond(now.getTime())))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512);

        if(expireAfter == null){
                jwtBuilder = jwtBuilder.expiration(Date.from(Instant.ofEpochSecond(now.getTime() + 1800000L)));
        } else if(expireAfter == -1){
            jwtBuilder = jwtBuilder.expiration(Date.from(Instant.ofEpochSecond(now.getTime() + Long.MAX_VALUE)));
        } else {
            jwtBuilder = jwtBuilder.expiration(Date.from(Instant.ofEpochSecond(now.getTime() + expireAfter)));
        }

        LinkedHashMap<String, Object> dataParsed = new ObjectMapper().convertValue(data, LinkedHashMap.class);

        for (String key : dataParsed.keySet()) {
            if(!List.of(excludedKeys).contains(key))
                jwtBuilder.claim(key, dataParsed.get(key));
        }

        return jwtBuilder.compact();
    }


    public synchronized String getGuestToken(){
        Date now = new Date();
        JwtBuilder jwtBuilder = Jwts.builder()
                .issuer("ArliasCrudify")
                .subject("arliasjwt")
                .issuedAt(Date.from(Instant.ofEpochSecond(now.getTime())))
                .expiration(Date.from(Instant.ofEpochSecond(now.getTime() + 31557600000L)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512);

        jwtBuilder.claim("metaType", "Guest");

        return jwtBuilder.compact();
    }


    public synchronized String getGuestToken(Object data, String...excludedKeys){
        Date now = new Date();
        JwtBuilder jwtBuilder = Jwts.builder()
                .issuer("ArliasCrudify")
                .subject("arliasjwt")
                .issuedAt(Date.from(Instant.ofEpochSecond(now.getTime())))
                .expiration(Date.from(Instant.ofEpochSecond(now.getTime() + 31557600000L)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512);

        LinkedHashMap<String, Object> dataParsed = new ObjectMapper().convertValue(data, LinkedHashMap.class);

        for (String key : dataParsed.keySet()) {
            if(!List.of(excludedKeys).contains(key))
                jwtBuilder.claim(key, dataParsed.get(key));
        }

        return jwtBuilder.compact();
    }


    public synchronized String getSimpleToken(){
        Date now = new Date();

        return Jwts.builder()
                .issuer("ArliasCrudify")
                .subject("msilverman")
                .issuedAt(now)
                .expiration(Date.from(Instant.ofEpochSecond(now.getTime() + 1800000L)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS512).compact();
    }

}
