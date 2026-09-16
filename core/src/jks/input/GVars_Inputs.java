package jks.input;

import jks.camera.GVars_Camera;
import jks.vinterface.GVars_UI;
import jks.vinterface.controlling.Utils_Controllable;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GVars_Personnage; 
import jks.vue.models.game.WagonLevel;

public class GVars_Inputs 
{
	public static boolean 
		leftPressed, rightPressed ;
	
	public static int startSelection_X ;
	public static int startSelection_Y ;
	
	public static boolean blockActionForClick ; // Considere s'il faut annuler toute autre action de click

	private static float speedX = 300 ;
	private static float camMulti = 30 ; 
	
	public static void updateInput_Game(float delta) 
	{

		if (leftPressed)
		{GVars_Game.ross.velocity.x -= GVars_Personnage.velocityAccelerationX * delta ;} 
		else if (rightPressed)
		{GVars_Game.ross.velocity.x += GVars_Personnage.velocityAccelerationX * delta ;}
		else
		{
			GVars_Game.ross.velocity.x = 0 ; 
		}
		
		GVars_Game.ross.update(delta);
		float cameraMovePower = GVars_Game.ross.velocity.x * delta * camMulti ; 
		
		if(GVars_Inputs.rightPressed)
		{
			
			if(cameraMovePower == 0)
				cameraMovePower = speedX * delta ; 
			
			if(GVars_Camera.camera.position.x + (cameraMovePower) > maxCameraX())
			{
				GVars_Camera.camera.position.x = maxCameraX() ; 
			}
			else
			{
				GVars_Camera.camera.translate(cameraMovePower, 0);
			}
			
		}
		else if(GVars_Inputs.leftPressed)
		{
			if(cameraMovePower == 0)
				cameraMovePower = -speedX * delta ; 
			
			if(GVars_Camera.camera.position.x + (cameraMovePower) < minCameraX())
			{
				GVars_Camera.camera.position.x = minCameraX() ; 
			}
			else
			{					
				GVars_Camera.camera.translate(cameraMovePower, 0);
			}
			
		}	
	}
	
	/** The view's centre when its left edge is on the start of the carriage. */
	static float minCameraX()
	{
		return GVars_Camera.WORLD_WIDTH / 2 ; 
	}
	
	/** The view's centre when its right edge is on the end of the carriage. */
	static float maxCameraX()
	{
		return WagonLevel.WIDTH - GVars_Camera.WORLD_WIDTH / 2 ; 
	}
	
	public static void updateInput_ControllingInterface()
	{
		if(GVars_UI.currentControllable != null)
		{Utils_Controllable.decodeInterfaceController();}
	}

	public static void resetInputs()
	{
		leftPressed  = false ;
		rightPressed = false ;
	}

	
	
}
