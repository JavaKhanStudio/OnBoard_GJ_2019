package jks.tools;

import com.badlogic.gdx.math.Rectangle;

import jks.debug.GVars_Debug;

public class Utils_Debug 
{

	public static void drawIm(Rectangle rec)
	{
		GVars_Debug.shapeDebugRenderer.rect(rec.x, rec.y, rec.width, rec.height);
	}
	
}
