package com.videotogether.config;

import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;


@Configuration
public class ConfigBean {
    @Bean
    public ServerEndpointExporter myServerSocket()  {
        return new ServerEndpointExporter();
    }

    @Bean
    public Gson gson() {
        return new Gson();
    }

}
