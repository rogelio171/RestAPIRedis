package com.roger.redis;

import com.roger.redis.config.TestContainersConfig;
import com.roger.redis.seeder.DataSeeder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestContainersConfig.class)
class RestApiRedisApplicationTests {

	/** Prevents the {@link DataSeeder} from calling the external REST Countries API. */
	@MockitoBean
	private DataSeeder dataSeeder;

	@Test
	void contextLoads() {
	}

}
