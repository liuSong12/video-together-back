package com.videotogether;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan
@SpringBootApplication
@MapperScan("com.videotogether.mapper")
public class VideotogetherApplication {
	public static void main(String[] args) {
		SpringApplication.run(VideotogetherApplication.class, args);
	}

}
