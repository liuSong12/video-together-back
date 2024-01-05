package com.videotogether;

import com.videotogether.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VideotogetherApplicationTests {

	@Test
	public void test(){
		String jwt = JwtConfig.createJWT("111111");
		System.out.println(jwt);
	}



}
