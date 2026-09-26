package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jks.tools.Utils_Format;

/**
 * The labs' numbers read as they did (r137): Utils_Format stands in for String.format, which the
 * browser build cannot translate.
 */
class UtilsFormatTest
{
	@Test
	@DisplayName("fixed() writes what %.1f and %.2f wrote, and twoDigits() what %02d wrote")
	void writesWhatStringFormatWrote()
	{
		for (int i = -2000 ; i <= 2000 ; i++)
		{
			float value = i / 997f ;
			assertEquals(String.format(Locale.ROOT, "%.2f", value), Utils_Format.fixed(value, 2), "value " + value) ;
			assertEquals(String.format(Locale.ROOT, "%.1f", value * 7), Utils_Format.fixed(value * 7, 1), "value " + value * 7) ;
		}
		for (int seconds = 0 ; seconds < 60 ; seconds++)
			assertEquals(String.format(Locale.ROOT, "%02d", seconds), Utils_Format.twoDigits(seconds)) ;
	}
}
