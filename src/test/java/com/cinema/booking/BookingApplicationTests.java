package com.cinema.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// profile "test" - separate database cinemadb_test
@SpringBootTest
@ActiveProfiles("test")
class BookingApplicationTests {

	@Test
	void contextLoads() {
	}

}
