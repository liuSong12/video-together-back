package com.videotogether;

import com.videotogether.config.JwtConfig;
import com.videotogether.service.impl.UserServiceImpl;
import com.videotogether.utils.Utils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VideotogetherApplicationTests {
	@Autowired
	private UserServiceImpl userService;
	@Autowired
	private Utils utils;

	@Test
	public void test(){
		System.out.println(userService);
		System.out.println(utils);
	}



}
