package jks.verify;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.Graphics.DisplayMode;

import jks.vinterface.Block_Resolution;

/**
 * What the resolution box in Options > Graphics offers, for monitors nobody here owns.
 *
 * It kept only the monitor's 16:9 modes at exactly 60 Hz. On a 59.94, 144 or 165 Hz screen,
 * or a 16:10 or ultrawide one, the box was empty and Apply threw a NullPointerException (r14).
 */
class ResolutionChoicesTest
{
	/** DisplayMode's constructor is protected: the backends make them, nothing else does. */
	private static DisplayMode mode(int width, int height, int refreshRate)
	{
		return new DisplayMode(width, height, refreshRate, 24) {};
	}

	private static List<String> choices(DisplayMode desktop, int savedWidth, int savedHeight, DisplayMode... modes)
	{
		return Block_Resolution.resolutionChoices(modes, desktop, savedWidth, savedHeight);
	}

	private static int widthOf(String size)
	{
		return Block_Resolution.parseResolution(size)[0];
	}

	@Test
	@DisplayName("a 144 Hz monitor gets a list, not an empty box (the r14 crash)")
	void highRefreshRate()
	{
		DisplayMode desktop = mode(1920, 1080, 144);
		List<String> list = choices(desktop, 1600, 900, desktop, mode(1920, 1080, 120), mode(1280, 720, 144));

		assertTrue(list.contains("1920x1080"), list.toString());
		assertTrue(list.contains("1280x720"), list.toString());
		assertTrue(list.contains("1600x900"), list.toString());
	}

	@Test
	@DisplayName("a 59.94 Hz monitor, which GLFW reports as 59, gets a list")
	void ntscRefreshRate()
	{
		DisplayMode desktop = mode(1920, 1080, 59);
		List<String> list = choices(desktop, 1600, 900, desktop, mode(1366, 768, 59));

		assertTrue(list.contains("1920x1080"), list.toString());
		assertTrue(list.contains("1366x768"), "1366x768 is 16:9 near enough: " + list);
	}

	@Test
	@DisplayName("a 16:10 monitor is offered 16:9 windows that fit on it")
	void sixteenByTen()
	{
		DisplayMode desktop = mode(1920, 1200, 60);
		List<String> list = choices(desktop, 1600, 900, desktop, mode(1680, 1050, 60), mode(1280, 800, 60));

		assertTrue(list.contains("1920x1080"), list.toString());
		assertFalse(list.contains("1920x1200"), "the game is drawn for 16:9: " + list);
		assertFalse(list.contains("2560x1440"), "must fit on the desktop: " + list);
	}

	@Test
	@DisplayName("an ultrawide monitor is offered 16:9 windows, not its own shape")
	void ultrawide()
	{
		DisplayMode desktop = mode(3440, 1440, 100);
		List<String> list = choices(desktop, 1600, 900, desktop);

		assertTrue(list.contains("2560x1440"), list.toString());
		assertFalse(list.contains("3440x1440"), list.toString());
		assertFalse(list.contains("3840x2160"), "taller than the desktop: " + list);
	}

	@Test
	@DisplayName("each size appears once, whatever the refresh rates, smallest first")
	void noDuplicatesAndSorted()
	{
		DisplayMode desktop = mode(2560, 1440, 165);
		List<String> list = choices(desktop, 1600, 900,
			desktop, mode(2560, 1440, 144), mode(2560, 1440, 60),
			mode(1920, 1080, 165), mode(1920, 1080, 60), mode(1920, 1080, 50),
			mode(1280, 720, 60), mode(1280, 720, 60));

		assertEquals(new HashSet<>(list).size(), list.size(), "duplicates: " + list);
		List<Integer> widths = new ArrayList<>();
		for (String size : list) widths.add(widthOf(size));
		List<Integer> sorted = new ArrayList<>(widths);
		sorted.sort(null);
		assertEquals(sorted, widths, "Full screen picks the last entry, so the largest must be last: " + list);
		assertEquals("2560x1440", list.get(list.size() - 1));
	}

	@Test
	@DisplayName("small modes and odd shapes are left out")
	void filters()
	{
		DisplayMode desktop = mode(1920, 1080, 60);
		List<String> list = choices(desktop, 1920, 1080,
			desktop, mode(640, 360, 60), mode(1024, 768, 60), mode(1280, 1024, 60));

		assertFalse(list.contains("640x360"), list.toString());
		assertFalse(list.contains("1024x768"), list.toString());
		assertFalse(list.contains("1280x1024"), list.toString());
	}

	@Test
	@DisplayName("the saved size is always there, so the box can show the setting in force")
	void savedSizeAlwaysOffered()
	{
		// Written by hand, not 16:9, and there are no modes at all.
		List<String> list = choices(null, 1024, 768);
		assertTrue(list.contains("1024x768"), list.toString());

		// A monitor that reports nothing at all still gets an entry to select.
		assertFalse(choices(mode(0, 0, 0), 1600, 900).isEmpty());
		assertFalse(Block_Resolution.resolutionChoices(null, null, 1600, 900).isEmpty());
	}

	@Test
	@DisplayName("resolutions parse, and anything else comes back null rather than throwing")
	void parsing()
	{
		assertArrayEquals(new int[] {1600, 900}, Block_Resolution.parseResolution("1600x900"));
		assertNull(Block_Resolution.parseResolution(null));
		assertNull(Block_Resolution.parseResolution(""));
		assertNull(Block_Resolution.parseResolution("x"));
		assertNull(Block_Resolution.parseResolution("1600"));
		assertNull(Block_Resolution.parseResolution("widexhigh"));
		assertNull(Block_Resolution.parseResolution("0x0"));
	}
}
