package com.example.tests;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class OrderValidationTest {

	private static String jsonData;
	
	@BeforeAll
	static void setup() throws IOException {
		File file = new File("orders.json"); // adjust path if needed
		jsonData = new String(Files.readAllBytes(file.toPath()));
	}
	
	@Test
	void testOrderIdNotEmpty() {
		List<String> ids = JsonPath.read(jsonData, "$[*].id");
		for (String id : ids) {
			assertNotNull(id, "Order id is null");
			assertFalse(id.trim().isEmpty(), "Order id is empty");
		}
	}
	
	@Test
	void testOrderStatusValid() {
		List<String> statuses = JsonPath.read(jsonData, "$[*].status");
		for (String status : statuses) {
			assertTrue(
			status.equals("PAID") || status.equals("PENDING") || status.equals("CANCELLED"),"Invalid status: " + status);
		}
	}
	
	@Test
	void testCustomerEmailFormat() {
		List<String> emails = JsonPath.read(jsonData, "$[*].customer.email");
		Pattern emailRegex = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
		for (String email : emails) {
			if (email != null) {
				assertTrue(emailRegex.matcher(email).matches(), "Invalid email: " + email);
			}
		}
	}
	
	@Test
	void testItemPriceIsPositive() {
		List<Double> prices = JsonPath.read(jsonData, "$[*].items[*].price");
		for (Double price : prices) {
			assertNotNull(price, "Price is null");
			assertTrue(price > 0, "Invalid price: " + price);
		}
	}
	
	@Test
	void testOrderTotalMatchesItems() {
	Object rawOrders = JsonPath.read(jsonData, "$[*]");

	if (rawOrders instanceof List<?>) {
		List<?> orders = (List<?>) rawOrders;
		for (Object obj : orders) {
			if (obj instanceof Map) {
				Map<String, Object> order = (Map<String, Object>) obj;

				// Check "items" is a list, then process each item safely
				if (order.containsKey("items") && order.get("items") instanceof List<?>) {
					List<?> items = (List<?>) order.get("items");
					double calcTotal = 0.0;

					for (Object itemObj : items) {
						if (itemObj instanceof Map) {
							Map<String, Object> item = (Map<String, Object>) itemObj;
							double price = item.containsKey("price") ? ((Number) item.get("price")).doubleValue() : 0.0;
							int quantity = item.containsKey("quantity") ? ((Number) item.get("quantity")).intValue() : 1;
							calcTotal += price * quantity;
						}
					}

					double actualTotal = order.containsKey("total") ? ((Number) order.get("total")).doubleValue() : 0.0;
					assertEquals(calcTotal, actualTotal, 0.01, "Total mismatch for order " + order.get("id"));
				}
			}
		}
	} else {
		fail("Orders JSON is not an array as expected.");
		}
	}

}