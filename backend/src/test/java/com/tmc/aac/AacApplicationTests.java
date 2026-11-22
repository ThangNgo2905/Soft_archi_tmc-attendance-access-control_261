package com.tmc.aac;

import com.tmc.aac.shared.api.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AacApplicationTests {

	@Test
	void apiResponseOk() {
		ApiResponse<String> response = ApiResponse.ok("scaffold");
		assertTrue(response.success());
	}
}
