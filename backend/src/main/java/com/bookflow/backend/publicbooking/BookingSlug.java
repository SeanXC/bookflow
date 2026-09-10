package com.bookflow.backend.publicbooking;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class BookingSlug {

	public static final int MIN_LENGTH = 3;
	public static final int MAX_LENGTH = 80;
	public static final String PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";
	public static final String DEFAULT_SLUG = "studio";

	private static final Pattern NON_SLUG_CHARACTERS = Pattern.compile("[^a-z0-9]+");
	private static final Pattern VALID_SLUG = Pattern.compile(PATTERN);

	private BookingSlug() {
	}

	public static String from(String name) {
		if (name == null || name.isBlank()) {
			return DEFAULT_SLUG;
		}

		String slug = NON_SLUG_CHARACTERS
				.matcher(name.toLowerCase(Locale.ROOT).trim())
				.replaceAll("-")
				.replaceAll("^-+", "")
				.replaceAll("-+$", "");
		if (slug.length() < MIN_LENGTH) {
			return DEFAULT_SLUG;
		}
		return trimToMax(slug);
	}

	public static String allocate(String name, Predicate<String> taken) {
		String base = from(name);
		if (!taken.test(base)) {
			return base;
		}
		for (int suffix = 2; suffix < 10_000; suffix++) {
			String candidate = trimToMax(base + "-" + suffix);
			if (!taken.test(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Unable to allocate a unique booking slug");
	}

	public static boolean isValid(String slug) {
		return slug != null && VALID_SLUG.matcher(slug).matches();
	}

	private static String trimToMax(String slug) {
		if (slug.length() <= MAX_LENGTH) {
			return slug;
		}
		String trimmed = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
		return trimmed.length() < MIN_LENGTH ? DEFAULT_SLUG : trimmed;
	}
}
