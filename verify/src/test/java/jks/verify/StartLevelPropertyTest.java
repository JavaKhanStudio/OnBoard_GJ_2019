package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jks.amain.Main_Application;

/** What -Donboard.level accepts (r43). StartLevelTest (-PwithGl) starts a carriage with it. */
class StartLevelPropertyTest
{
	@Test
	@DisplayName("no onboard.level starts in carriage 1")
	void absentIsCarriageOne()
	{
		assertEquals(1, Main_Application.levelFrom(null));
	}

	@Test
	@DisplayName("carriages 1 to 4 are taken as given")
	void everyCarriage()
	{
		for (int n = 1; n <= 4; n++)
			assertEquals(n, Main_Application.levelFrom(" " + n + " "));
	}

	@Test
	@DisplayName("anything else warns and falls back to carriage 1, like onboard.start")
	void unknownFallsBack()
	{
		assertEquals(1, Main_Application.levelFrom("0"));
		assertEquals(1, Main_Application.levelFrom("5"));
		assertEquals(1, Main_Application.levelFrom("three"));
	}
}
