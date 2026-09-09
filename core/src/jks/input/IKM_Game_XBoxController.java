package jks.input;

import static jks.input.GVars_Controller.getPlayer;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;

import jks.debug.GVars_Debug;
import jks.vars.GVars_Heart; 

public class IKM_Game_XBoxController implements ControllerListener
{
	
	@Override
	public void connected(Controller controller) 
	{
		if(GVars_Debug.coreInformationDebug)
			System.out.println("Connecte controller");
	}

	@Override
	public void disconnected(Controller controller) 
	{
		if(GVars_Debug.coreInformationDebug)
			System.out.println("Disconnected controller");
	}

	@Override
	public boolean buttonDown(Controller controller, int buttonCode) 
	{
		if(GVars_Heart.inCinematic) {
			GVars_Heart.inCinematic_Click = true ;
			return true; 
		}
		
		
		Player_Inputs inputing = getPlayer(null) ; 
		if(inputing == null)
		{
			return false; 
		}

		switch (buttonCode) 
		{
			case KeysXbox.A :
				inputing.jumpPressed = true ; 
				return true ;
			case KeysXbox.B :
				inputing.powerLeft = true ; 
				return true ;
			case KeysXbox.X :
				inputing.powerRight = true ; 
				return true ;
			case KeysXbox.START :
				return true ;
			case KeysXbox.BACK :
				if(GVars_Debug.coreInformationDebug)
				{
					GVars_Heart.vue.restart();
				}
				return true ;
		}
		
		return false;
	}

	@Override
	public boolean buttonUp(Controller controller, int buttonCode) 
	{
		if(GVars_Heart.inCinematic)
			return true; 
		/*
		switch (buttonCode) 
		{
			case KeysXbox.A :
				jumpPressed = false ;
				jumpSupression = true ; 
				return true ;
			case KeysXbox.BACK :
				return true ;
		}
		*/
		return false;	
	}
	
	@Override
	public boolean axisMoved(Controller controller, int axisCode, float value) 
	{
		Player_Inputs inputing = getPlayer(null) ; 
		System.out.println(inputing);
		if(inputing == null)
		{return false;}
		
		return Utils_Controller.axisController(axisCode,value,inputing) ;
	}

	@Override
	public boolean povMoved(Controller controller, int povCode, PovDirection value) 
	{return false;}

	@Override
	public boolean xSliderMoved(Controller controller, int sliderCode, boolean value) 
	{return false;}

	@Override
	public boolean ySliderMoved(Controller controller, int sliderCode, boolean value) 
	{return false;}

	@Override
	public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) 
	{return false;}

}
