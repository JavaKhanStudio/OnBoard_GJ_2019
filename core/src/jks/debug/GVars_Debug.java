package jks.debug;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class GVars_Debug 
{
	public static boolean debugMode = false ; 
	
	public static boolean soundDebug = false ;
	public static boolean collisionDebug = false ;
	/**
	 * Developer chatter and developer shortcuts. Off by default: with this on, the
	 * gamepad's BACK button restarts the level, which is a fine debugging aid and a
	 * terrible thing to hand a player.
	 */
	public static boolean coreInformationDebug = false ;
	
	public static ShapeRenderer shapeDebugRenderer;
	
	public static void activatedDebug()
	{
		collisionDebug = true ;
		soundDebug = true ; 
		coreInformationDebug = true ;
	}
	
	public static void desactivateDebug()
	{
		collisionDebug = false ;
		soundDebug = false ; 
		coreInformationDebug = false ;
	}
	
	public static void setInFullDebug(boolean b) 
	{
		debugMode = b ;
		
		if(debugMode)
			activatedDebug() ; 
		else
			desactivateDebug() ;
		
	}

	
	
	

}
