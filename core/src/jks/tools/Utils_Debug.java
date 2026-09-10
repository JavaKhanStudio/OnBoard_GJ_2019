package jks.tools;

import com.badlogic.gdx.math.Rectangle;

import jks.debug.GVars_Debug;

public class Utils_Debug 
{

	public static void drawIm(Rectangle rec)
	{
		GVars_Debug.shapeDebugRenderer.rect(rec.x, rec.y, rec.width, rec.height);
	}
	
	/**
	 * Developer chatter - which key was pressed, which sound was asked for, how long a
	 * step took. Silent unless {@link GVars_Debug#coreInformationDebug} is on, so a player
	 * running the game does not get a running commentary in their console.
	 */
	public static void log(String message)
	{
		if(GVars_Debug.coreInformationDebug)
			System.out.println(message);
	}
	
	/**
	 * Something is actually wrong - a missing animation, a view that should exist and does
	 * not. Always printed: if this fires in front of a player, we want it in their report.
	 */
	public static void warn(String message)
	{
		System.err.println(message);
	}
	
}
