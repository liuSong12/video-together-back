package com.videotogether;

import com.videotogether.config.JwtConfig;
import com.videotogether.service.impl.UserServiceImpl;
import com.videotogether.utils.Utils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class VideotogetherApplicationTests {
	@Autowired
	private UserServiceImpl userService;
	@Autowired
	private Utils utils;
	@Autowired
	private RedisTemplate redisTemplate;

	@Test
	public void test(){
		redisTemplate.opsForValue().set("小明", 123, 1000*60);
	}



}
