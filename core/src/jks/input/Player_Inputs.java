package jks.input;

import jks.vinterface.GVars_UI;
import jks.vinterface.controlling.Utils_Controllable;

public class Player_Inputs 
{
	public boolean 
		touched, 
		jumpPressed,
		upPressed, downPressed,
		leftPressed, rightPressed,
		powerLeft, powerRight,
		triggerPowerLeft, triggerPowerRight,
		jumpSupression
		;

	public boolean isAdmin = true ;
	
	public boolean blockActionForClick ; // Considere s'il faut annuler toute autre action de click

	public Player_Inputs() 
	{}
	
	public static void updateInput_ControllingInterface()
	{
		if(GVars_UI.currentControllable != null)
		{Utils_Controllable.decodeInterfaceController();}
	}

	public void resetInputs()
	{
		touched = false ;
		jumpPressed = false ;
		upPressed = false ;
		downPressed = false ;
		leftPressed  = false ;
		rightPressed = false ;
	}
}
