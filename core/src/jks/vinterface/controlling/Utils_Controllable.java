package jks.vinterface.controlling;

import static jks.input.IKM_Game_Keyboard.pressingDown;
import static jks.input.IKM_Game_Keyboard.pressingLeft;
import static jks.input.IKM_Game_Keyboard.pressingRight;
import static jks.input.IKM_Game_Keyboard.pressingTop;
import static jks.vinterface.GVars_UI.buttonMap;
import static jks.vinterface.GVars_UI.cursorPos;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.scenes.scene2d.ui.Button;

import jks.vinterface.GVars_UI; 

public class Utils_Controllable 
{

	
	public static void decodeInterfaceKeybord(int keycode)
	{
		System.out.println("key " + keycode);
		
		if(pressingTop(keycode))
			moveY(true) ; 
		else if(pressingDown(keycode))
			moveY(false) ;
		else if(pressingRight(keycode))
			moveX(true) ;
		else if(pressingLeft(keycode))
			moveX(false) ;
	}
	
	static int neededTime = 100 ; 
	
	public static void decodeInterfaceController()
	{
		
		
		/*	
		if(leftPressed)
		{moveX(false) ;}
		else if(rightPressed)
		{moveX(true) ; }
		
		if(upPressed)
		{moveY(true) ; }
		else if(downPressed)
		{moveY(false) ; }
		//*/
	}
	
	public static boolean decodeInterfaceControllerButton(Controller controller, int buttonCode) 
	{
		ControllerMapping map = controller.getMapping() ;
		if(map == null)
			return false ; 
		
		if(buttonCode == map.buttonA)
		{
			GVars_UI.selectButton() ;
			return true ;
		}
		if(buttonCode == map.buttonStart)
			return true ;
		
		return false ; 
	}
	
	private static void moveY(boolean positif) 
	{
	}

	public static void moveX(boolean positif)
	{
//		if(positif)
//		{
//			if(cursorPos.x + 1 >= buttonMap.size())
//			{cursorPos.x = 0 ;}
//			else
//			{cursorPos.x ++ ;}		
//		}
//		else
//		{
//			if(cursorPos.x - 1 < 0 )
//			{cursorPos.x = buttonMap.size() - 1;}
//			else
//			{cursorPos.x -- ;}	
//		}
	}
	
	public static Button getCurrentButton()
	{return buttonMap.get(cursorPos.x).get(cursorPos.y);}
	
	private static void moveControleFairy()
	{
		
	}
	
	private static void checkForYCompatibility() 
	{
		// TODO Auto-generated method stub
	}
	
}