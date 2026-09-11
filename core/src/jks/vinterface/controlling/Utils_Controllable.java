package jks.vinterface.controlling;

import jks.tools.Utils_Debug;

import static jks.input.IKM_Game_Keyboard.pressingDown;
import static jks.input.IKM_Game_Keyboard.pressingEnter;
import static jks.input.IKM_Game_Keyboard.pressingLeft;
import static jks.input.IKM_Game_Keyboard.pressingRight;
import static jks.input.IKM_Game_Keyboard.pressingTop;
import static jks.vinterface.GVars_UI.buttonMap;
import static jks.vinterface.GVars_UI.cursorPos;

import java.util.ArrayList;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;

import jks.vinterface.GVars_UI; 

/**
 * Drives the menu on screen from the keyboard: the arrows (or ZQSD / WASD) move the focus,
 * Enter or Space activates it, Escape goes back.
 *
 * Built in 2019 as empty stubs and never called, so every menu was mouse-only.
 */
public class Utils_Controllable 
{

	/** True when the key meant something to the menu on screen, so nothing else gets it. */
	public static boolean decodeInterfaceKeybord(int keycode)
	{
		if(GVars_UI.currentControllable == null || buttonMap == null)
			return false ;
	
		Utils_Debug.log("key " + keycode);
		
		if(keycode == Keys.ESCAPE)
			return GVars_UI.goBack() ;

		boolean up = pressingTop(keycode), down = pressingDown(keycode) ;
		boolean left = pressingLeft(keycode), right = pressingRight(keycode) ;
		boolean enter = pressingEnter(keycode) ;
		if(!(up || down || left || right || enter))
			return false ;

		// The first key only shows where the focus is. Acting on it would mean Enter starting
		// the game, or an arrow moving a cursor the player had not seen yet.
		if(!GVars_UI.focusShown)
		{
			GVars_UI.showFocus(true) ;
			return true ;
		}

		if(enter)
			GVars_UI.selectButton() ;
		else if(up)
			moveY(-1) ;
		else if(down)
			moveY(1) ;
		else if(!adjust(getFocused(), right ? 1 : -1))
			moveX(right ? 1 : -1) ;

		return true ;
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
	
	/** Left and right step a select box or a slider instead of leaving it. */
	private static boolean adjust(Actor focused, int step)
	{
		if(focused instanceof Slider)
		{
			Slider slider = (Slider)focused ;
			if(!slider.isDisabled())
				slider.setValue(slider.getValue() + step * slider.getStepSize()) ;
			return true ;
		}
		if(focused instanceof SelectBox)
		{
			stepSelectBox((SelectBox<?>)focused, step) ;
			return true ;
		}
		return false ;
	}

	/** Moves a select box to its next or previous entry, wrapping round. */
	public static void stepSelectBox(SelectBox<?> box, int step)
	{
		int size = box.getItems().size ;
		if(box.isDisabled() || size == 0)
			return ;
		box.setSelectedIndex(Math.floorMod(box.getSelectedIndex() + step, size)) ;
	}
	
	private static void moveY(int step)
	{
		ArrayList<Actor> column = buttonMap.get(cursorPos.x) ;
		GVars_UI.moveFocus(cursorPos.x, Math.floorMod(cursorPos.y + step, column.size())) ;
	}

	public static void moveX(int step)
	{
		int x = Math.floorMod(cursorPos.x + step, buttonMap.size()) ;
		int y = Math.min(cursorPos.y, buttonMap.get(x).size() - 1) ;
		GVars_UI.moveFocus(x, y) ;
	}

	/** The widget the keyboard is on, or null when no menu is being driven. */
	public static Actor getFocused()
	{
		if(buttonMap == null || cursorPos == null)
			return null ;
		return buttonMap.get(cursorPos.x).get(cursorPos.y);
	}
	
	private static void moveControleFairy()
	{
		
	}
	
	private static void checkForYCompatibility() 
	{
		// TODO Auto-generated method stub
	}
	
}
