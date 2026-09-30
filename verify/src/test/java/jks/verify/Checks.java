package jks.verify;

import java.util.ArrayList;

/** What a scenario checked, one line each: "ok   " or "FAIL " then what was checked. */
final class Checks extends ArrayList<String>
{
	void record(String what, boolean ok)
	{
		add((ok ? "ok   " : "FAIL ") + what);
	}
}
