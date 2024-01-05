package com.videotogether.filter;

import com.videotogether.commen.Result;
import com.videotogether.config.JwtConfig;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.rmi.RemoteException;

@WebFilter
public class TokenFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/user/login")||requestURI.contains(".ts")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        String authorization = request.getParameter("authorization");
        if (authorization==null) {
            authorization = request.getHeader("authorization");
        }
        if(authorization==null){
            response.getOutputStream().write("token is null".getBytes());
        }else {
            try {
                String subject = JwtConfig.getSubject(authorization);
                String jwt = JwtConfig.createJWT(subject);
                response.setHeader("Authorization",jwt);
                filterChain.doFilter(servletRequest, servletResponse);
            }catch (Exception e){
                response.setStatus(401);
                response.getOutputStream().write("token is error".getBytes());
            }
        }
    }
}
