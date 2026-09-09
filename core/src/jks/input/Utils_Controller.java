package jks.input;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;

import jks.vars.GVars_Heart;

public class Utils_Controller 
{
	static float minForceMoveX = 0.3f;
	static float minForceMoveY = 0.2f;
	
	public static boolean axisController(Controller controller, int axisCode, float value, Player_Inputs player)
	{
		if(GVars_Heart.inCinematic)
		{
			return true; 
		}
		
		// The stick axes used to be hard-coded as 1 and 0 - swapped relative to convention,
		// because that is how the old backend happened to number them. The device now tells us.
		ControllerMapping map = controller.getMapping() ;
		if(map == null)
			return false ;
		
		if(axisCode == map.axisLeftX)
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
		
		if(axisCode == map.axisLeftY)
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