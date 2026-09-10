package com.bookflow.backend.publicbooking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class BookingSlugTest {

	@Test
	void fromNormalizesBusinessNames() {
		assertEquals("glow-studio", BookingSlug.from("  Glow Studio  "));
		assertEquals("dublin-beauty", BookingSlug.from("Dublin Beauty!!!"));
		assertEquals("studio", BookingSlug.from("A"));
		assertEquals("studio", BookingSlug.from("   "));
	}

	@Test
	void allocateAppendsASuffixWhenTheBaseSlugIsTaken() {
		Set<String> taken = new HashSet<>();
		taken.add("glow-studio");
		taken.add("glow-studio-2");

		assertEquals(
				"glow-studio-3",
				BookingSlug.allocate("Glow Studio", taken::contains));
	}

	@Test
	void isValidAcceptsOnlyLowercaseHyphenatedSlugs() {
		assertTrue(BookingSlug.isValid("demo-salon"));
		assertFalse(BookingSlug.isValid("Demo-Salon"));
		assertFalse(BookingSlug.isValid("-leading"));
		assertFalse(BookingSlug.isValid("trailing-"));
	}
}
