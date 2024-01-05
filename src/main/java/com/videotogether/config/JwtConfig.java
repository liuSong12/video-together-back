package com.videotogether.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
@Data
@Component
public class JwtConfig {
    private static final String secret = "privatekey";
    public static String createJWT(String subject){
        Date nowDate = new Date();
        Date expireDate = new Date(nowDate.getTime() + 60 * 60 * 1000); //配置文件是s  这里是一小时
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject(subject)
                .setIssuedAt(nowDate)
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public static String getSubject(String token){
        return Jwts.parser()
               .setSigningKey(secret)
               .parseClaimsJws(token)
               .getBody()
               .getSubject();
    }

    public static String getInfoByToken(HttpServletRequest request) {
        String authorization = request.getParameter("authorization");
        if (authorization==null) {
            authorization = request.getHeader("authorization");
        }
        if(authorization==null){
            return null;
        }else {
            return JwtConfig.getSubject(authorization);
        }
    }

}
