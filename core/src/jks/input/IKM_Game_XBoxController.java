package jks.input;

import jks.tools.Utils_Debug;

import static jks.input.GVars_Controller.getPlayer;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.ControllerMapping;

import jks.debug.GVars_Debug;
import jks.vars.GVars_Heart;
import jks.vue.GVars_Fade;

/**
 * Gamepad input.
 *
 * gdx-controllers 2.x asks the device for its own button and axis numbering through
 * {@link ControllerMapping} instead of assuming the Xbox layout, so this now works with
 * whatever pad is plugged in rather than only the one that was on the desk in 2019.
 */
public class IKM_Game_XBoxController implements ControllerListener
{

	@Override
	public void connected(Controller controller)
	{
		if(GVars_Debug.coreInformationDebug)
			Utils_Debug.log("Controller connected: " + controller.getName());
	}

	@Override
	public void disconnected(Controller controller)
	{
		if(GVars_Debug.coreInformationDebug)
			Utils_Debug.log("Controller disconnected: " + controller.getName());
	}

	@Override
	public boolean buttonDown(Controller controller, int buttonCode)
	{
		// The keyboard and the mouse are held off during a fade by GVars_Fade; the pad is not.
		if(GVars_Fade.isFading())
			return true ;
		
		if(GVars_Heart.inCinematic)
		{
			GVars_Heart.inCinematic_Click = true ;
			return true;
		}

		Player_Inputs inputing = getPlayer(null) ;
		if(inputing == null)
			return false;

		ControllerMapping map = controller.getMapping() ;
		if(map == null)
			return false;

		if(buttonCode == map.buttonA)
		{
			inputing.jumpPressed = true ;
			return true ;
		}
		if(buttonCode == map.buttonB)
		{
			inputing.powerLeft = true ;
			return true ;
		}
		if(buttonCode == map.buttonX)
		{
			inputing.powerRight = true ;
			return true ;
		}
		if(buttonCode == map.buttonStart)
			return true ;
		if(buttonCode == map.buttonBack)
		{
			if(GVars_Debug.coreInformationDebug)
				GVars_Heart.vue.restart();
			return true ;
		}

		return false;
	}

	@Override
	public boolean buttonUp(Controller controller, int buttonCode)
	{
		return GVars_Heart.inCinematic;
	}

	@Override
	public boolean axisMoved(Controller controller, int axisCode, float value)
	{
		Player_Inputs inputing = getPlayer(null) ;
		if(inputing == null)
			return false;

		return Utils_Controller.axisController(controller, axisCode, value, inputing) ;
	}
}
