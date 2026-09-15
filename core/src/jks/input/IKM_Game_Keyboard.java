package jks.input;

import jks.debug.GVars_Debug;

import jks.tools.Utils_Debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;

import jks.camera.GVars_Camera;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vinterface.controlling.Utils_Controllable;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;
import jks.vue.models.game.Vue_Game;

public class IKM_Game_Keyboard extends InputAdapter 
{
	
		// PC input
		@Override
		public boolean keyDown (int keycode) 
		{
			
			if(GVars_Heart.inCinematic) {
				Utils_Debug.log("TO BE REMOVE");
				GVars_Heart.inCinematic_Click = true ;
				return true; 
			}
			
			Utils_Debug.log(Keys.toString(keycode));
			
			// While a menu is on screen it owns the arrows, Enter, Space and Escape.
			if(Utils_Controllable.decodeInterfaceKeybord(keycode))
				return true ;
			
			// Escape pauses a level. Once the pause screen is up, the menu above has Escape
			// and presses its Retour, which is what closes it again.
			if(Keys.ESCAPE == keycode && GVars_Heart.vue instanceof Vue_Game)
			{
				GVars_Heart.togglePauseMenu() ;
				return true ;
			}
			
			if(Keys.D == keycode || Keys.RIGHT == keycode)
			{
				GVars_Inputs.rightPressed = true ; 
				return true ; 
			}
			else if(Keys.Q == keycode || Keys.LEFT == keycode)
			{
				GVars_Inputs.leftPressed = true ; 
				return true ; 
			}
			// Skipping the level on SPACE is a development shortcut. It used to be live in
			// every build, and SPACE is about the first key anyone presses.
			else if(Keys.SPACE == keycode && GVars_Debug.debugMode)
			{
				GVars_Game.nextLevel() ; 
				return true ; 
			}
			
			return false ; 
		}

		
		@Override
		public boolean keyUp (int keycode) 
		{
			if(GVars_Heart.inCinematic) {
//				GVars_Game.inCinematic_Click = true ;
				return true; 
			}
			
			if(Keys.D == keycode || Keys.RIGHT == keycode)
			{
				GVars_Inputs.rightPressed = false ; 
				return true ; 
			}
			
			else if(Keys.Q == keycode || Keys.LEFT == keycode)
			{
				GVars_Inputs.leftPressed = false ; 
				return true ; 
			}
			
			return false ; 
		}
		
		private boolean debuggOptions(int keycode) 
		{
			switch(keycode)
			{
				case Keys.DEL :
					GVars_Heart.vue.restart(); return true ;
				case Keys.R :
				
				default :
					Utils_Debug.log("Nothing found for " + keycode);
					return false ; 
			}		
		}
		
		private boolean inputingControl(int keycode, Player_Inputs inputing, boolean pressingDown) 
		{
			if(inputing == null)
				return false; 
			
			if(inputing.isAdmin && pressingDown)	
			{
				Utils_Controllable.decodeInterfaceKeybord(keycode);
				return true; 
			}
				
			if(pressingTop(keycode))
			{
				inputing.upPressed = pressingDown ;
				return true ;
			}
			else if(pressingDown(keycode))
			{
				inputing.downPressed = pressingDown ; 
				return true ; 
			}		
			else if(pressingLeft(keycode))
			{
				inputing.leftPressed = pressingDown ;
				return true ;
			}
			else if(pressingRight(keycode))
			{
				inputing.rightPressed = pressingDown ;
				return true ;
			}
			
			return false ;
		}
		
		public static boolean pressingTop(int keycode) 
		{
			if(GVars_Heart.isAzerty && Keys.Z == keycode)
				return true ; 
			else if(Keys.W == keycode)
				return true ;
				
			if(Keys.UP == keycode)
				return true ;
			
			return false; 
		}
		
		public static boolean pressingDown(int keycode) 
		{
			if(Keys.S == keycode)
				return true ; 
				
			if(Keys.DOWN == keycode)
				return true ;
			
			return false; 
		}
		
		public static boolean pressingLeft(int keycode) 
		{
			if(GVars_Heart.isAzerty && Keys.Q == keycode)
				return true ; 
			else if(Keys.A == keycode)
				return true ;
				
			if(Keys.LEFT == keycode)
				return true ;
			
			return false; 
		}
		
		public static boolean pressingRight(int keycode) 
		{
			if(Keys.D == keycode)
				return true ; 
			
			if(Keys.RIGHT == keycode)
				return true ;
			
			return false; 
		}
		
		public static boolean pressingEnter(int keycode) 
		{
			if(Keys.ENTER == keycode)
				return true ; 
			
			if(Keys.SPACE == keycode)
				return true ;
			
			return false; 
		}
		
		@Override
		public boolean mouseMoved(int screenX, int screenY)
		{
			// The mouse is back in use: put the keyboard focus away so it does not sit
			// beside the hover.
			GVars_UI.showFocus(false) ;
			mouseSeen = true ; 
			mouseX = screenX ; 
			mouseY = screenY ; 
			return false ;
		}
		
		/** Where the mouse last moved to, in screen pixels. Nothing lights up before it has moved. */
		static boolean mouseSeen ; 
		static int mouseX, mouseY ; 
		
		/**
		 * The mouse in world coordinates, for the carriage items to light up under it (r40), or
		 * null whenever a click would not reach them: before the mouse has moved, in a
		 * cinematic, while paused, or over a stage widget that takes the click first.
		 */
		public static Vector3 pointerInCarriage()
		{
			if(!mouseSeen || GVars_Heart.inCinematic || GVars_Heart.isPaused)
				return null ; 
			
			if(overWidget(mouseX, mouseY))
				return null ; 
			
			return GVars_Camera.camera.unproject(new Vector3(mouseX, mouseY, 0)) ; 
		}
		
		/**
		 * The stage comes first in Vue_Game's multiplexer, and a widget that listens (the pause
		 * sign, the key, an inventory slot) keeps the click from the items under it.
		 */
		private static boolean overWidget(int screenX, int screenY)
		{
			if(GVars_UI.mainUi == null)
				return false ; 
			
			Vector2 stage = GVars_UI.mainUi.screenToStageCoordinates(new Vector2(screenX, screenY)) ; 
			for(Actor actor = GVars_UI.mainUi.hit(stage.x, stage.y, true) ; actor != null ; actor = actor.getParent())
			{
				if(actor.getListeners().size > 0)
					return true ; 
			}
			return false ; 
		}
		
		@Override
	    public boolean touchDown(int screenX, int screenY, int pointer, int button) 
		{
			if(GVars_Heart.inCinematic) {
				GVars_Heart.inCinematic_Click = true ;
				return true; 
			}
			
			// The pause screen leaves the carriage visible under it; it must not be playable.
			if(GVars_Heart.isPaused)
				return true ;
			
			Vector3 tmp= new Vector3(Gdx.input.getX(),Gdx.input.getY(),0);
			GVars_Camera.camera.unproject(tmp);
			
			for(GameItem item : GVars_Game.currentLevel.listItems)
			{
				item.tryTouch(tmp,GVars_Game.selectedItem, false);
			}
			
			return false ;
	    }
}