package jks.input;

import jks.vars.GVars_Heart;

public class Utils_Controller 
{
	static float minForceMoveX = 0.3f;
	static float minForceMoveY = 0.2f;
	
	public static boolean axisController(int axisCode, float value, Player_Inputs player)
	{
		if(GVars_Heart.inCinematic)
		{
			return true; 
		}
		
		if(axisCode == KeysXbox.AXIS_LEFT_X)
		{
			if(value > minForceMoveX)
			{
				player.leftPressed = false;
				player.rightPressed = true;
			}
			else if(value < -minForceMoveX)
			{
				player.leftPressed = true;
				player.rightPressed = false;
			}
			else
			{
				player.leftPressed = false;
				player.rightPressed = false;
			}
		}
		
		if(axisCode == KeysXbox.AXIS_LEFT_Y)
		{
			if(value > minForceMoveY)
			{
				player.upPressed = false;
				player.downPressed = true;
			}
			else if(value < -minForceMoveY)
			{
				player.upPressed = true;
				player.downPressed = false;
			}
			else
			{
				player.upPressed = false;
				player.downPressed = false;
			}
		}
	
		return false ; 
	}
	
}