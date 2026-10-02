package jks.input;

import com.badlogic.gdx.Input.Keys;

import jks.camera.GVars_Camera;
import jks.vinterface.GVars_UI;
import jks.vinterface.controlling.Utils_Controllable;
import jks.vue.models.game.ClickMarker;
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
	/** The view pans at half his walking speed, as it did when his speed was per frame at 60 fps. */
	private static float camMulti = 0.5f ; 
	
	/**
	 * Where a click in the carriage sent Ross (r229): the position.x he walks to, or NaN when
	 * he was sent nowhere. An arrow key takes over from it, and reaching it clears it.
	 */
	public static float walkTarget = Float.NaN ; 
	
	/**
	 * Sends Ross to stand with his middle under worldX, kept to where he may walk. Only when
	 * the options' click-to-walk is on; the keyboard alone stays the default.
	 */
	public static void walkTo(float worldX)
	{
		float target = worldX - GVars_Game.ross.getFrameWidth() / 2f ; 
		walkTarget = Math.max(GVars_Personnage.minPositionX, Math.min(GVars_Personnage.maxPositionX, target)) ; 
	}
	
	public static boolean isWalkingToTarget()
	{
		return !Float.isNaN(walkTarget) ; 
	}
	
	public static void updateInput_Game(float delta) 
	{
		boolean goLeft = leftPressed, goRight = rightPressed ; 
		if(!goLeft && !goRight && isWalkingToTarget())
		{
			// Close enough to be there within this frame: put him on it rather than step past.
			float gap = walkTarget - GVars_Game.ross.position.x ; 
			if(Math.abs(gap) <= GVars_Game.ross.index.walkSpeed * delta)
			{
				GVars_Game.ross.position.x = walkTarget ; 
				walkTarget = Float.NaN ; 
			}
			else if(gap < 0)
				goLeft = true ; 
			else
				goRight = true ; 
		}
		
		if (goLeft)
		{GVars_Game.ross.velocity.x -= GVars_Personnage.velocityAccelerationX * delta ;} 
		else if (goRight)
		{GVars_Game.ross.velocity.x += GVars_Personnage.velocityAccelerationX * delta ;}
		else
		{
			GVars_Game.ross.velocity.x = 0 ; 
		}
		
		GVars_Game.ross.update(delta);
		float cameraMovePower = GVars_Game.ross.velocity.x * delta * camMulti ; 
		
		if(goRight)
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
		else if(goLeft)
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

	/**
	 * D/Right and Q/Left walk (the game) or pan (the editor): sets the flag for the key going
	 * down or up, and says whether it was one of them.
	 */
	public static boolean pressArrow(int keycode, boolean down)
	{
		if(Keys.D == keycode || Keys.RIGHT == keycode)
		{
			rightPressed = down ; 
			walkTarget = Float.NaN ; 
			return true ; 
		}
		else if(Keys.Q == keycode || Keys.LEFT == keycode)
		{
			leftPressed = down ; 
			walkTarget = Float.NaN ; 
			return true ; 
		}
		return false ; 
	}

	public static void resetInputs()
	{
		leftPressed  = false ;
		rightPressed = false ;
		walkTarget = Float.NaN ; 
		ClickMarker.clear() ; 
	}

	
	
}
