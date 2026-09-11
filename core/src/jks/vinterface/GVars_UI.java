package jks.vinterface;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox;

import jks.tools.Vector2Int;
import jks.vinterface.controlling.Controllable_Interface;
import jks.vinterface.controlling.FocusMarker;
import jks.vinterface.controlling.Utils_Controllable;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.overlay.OverlayPause;
import jks.vue.Utils_View;

public class GVars_UI implements Runnable 
{

	public static Skin baseSkin;
	public static Stage mainUi;
	public static Table interaction;
	
	public static Vector2Int cursorPos ; 
	
	public static Controllable_Interface currentControllable;
	public static ArrayList<ArrayList<Actor>> buttonMap ;
	
	/** Whether the keyboard focus is on show. Off until a menu key is pressed, off again when the mouse moves. */
	public static boolean focusShown ;
	private static final FocusMarker focusMarker = new FocusMarker() ;
	
	public static Table bottomScore ; 
	
	

	
	public static void init() 
	{
		baseSkin = new Skin(Gdx.files.internal("ui/skins/basic/uiskin.json"));
		VisUI.load(GVars_UI.baseSkin);
		mainUi = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(mainUi);
		
		GVars_Font.prebuild() ;
		GVars_Font.resize();
	}
	
	
	public static void massResize(Array<Actor> actorList)
	{
		for(int i = 0 ; i < mainUi.getActors().size; i++)
		{
			Actor actor = mainUi.getActors().get(i) ; 
			workOnActor(actor) ;			
		}
	}
	
	public static void workOnActor(Actor actor)
	{
		if(actor instanceof Label)
		{
			Label label = (Label)actor ;
			label.setStyle(label.getStyle());
		}
		else if(actor instanceof TextButton)
		{
			TextButton button = (TextButton)actor ;
			button.getLabel().setStyle(button.getLabel().getStyle());
			button.invalidate();
		}
		else if(actor instanceof SelectBox)
		{
			SelectBox box = (SelectBox)actor ;
			box.getStyle().font = GVars_Font.fontont_SelectBox ; 
			box.invalidate();
		}
		else if(actor instanceof VisCheckBox)
		{
			VisCheckBox checkBox = (VisCheckBox)actor ;
			checkBox.getLabel().setStyle(checkBox.getLabel().getStyle());
			checkBox.setSize(300, 300);
			checkBox.invalidate();
		}
		else if(actor instanceof WidgetGroup)
		{
			WidgetGroup group = (WidgetGroup)actor ;
			Array<Actor> subGroup = group.getChildren() ; 
			for(int i = 0 ; i < subGroup.size; i++)
				workOnActor(subGroup.get(i)) ; 
		}
	}
	
	public static void reset()
	{
		// A new screen starts with no menu. Without this the start menu stayed in charge
		// after Jouer, and would have eaten the arrow keys that walk Ross.
		currentControllable = null ;
		buttonMap = null ;
		cursorPos = null ;
		
		mainUi = new Stage();
		Gdx.input.setInputProcessor(mainUi);
	}

	@Override
	public void run() 
	{init();}
	
	public static void setPause(boolean setPause) 
	{
		if(setPause) 
		{
			Utils_View.setOverlay(new OverlayPause());
		}
		else
		{
			Utils_View.removeCurrentOverlay() ;
			Utils_View.removeFilter() ;
		}
	}
	
	public static void activedInterface(Controllable_Interface newInterface)
	{
		currentControllable = newInterface ;
		buttonMap = newInterface.mapInterface() ; 
		cursorPos = newInterface.startAt() ;
		
		mainUi.addActor(focusMarker) ;
		focusMarker.toFront() ;
		if(focusShown)
			hover(Utils_Controllable.getFocused(), true) ;
	}

	/** Enter or Space: does exactly what a click on the focused widget does. */
	public static void selectButton()
	{
		Actor focused = Utils_Controllable.getFocused() ;
		if(focused instanceof Button)
		{
			// Button's own ClickListener does this on a click, which is what fires the
			// ChangeListener every menu button now reacts to.
			Button button = (Button)focused ;
			if(!button.isDisabled())
				button.toggle() ;
		}
		else if(focused instanceof SelectBox)
			Utils_Controllable.stepSelectBox((SelectBox<?>)focused, 1) ;
	}
	
	/** Escape: presses the screen's way back, if it has one. */
	public static boolean goBack()
	{
		Button back = currentControllable == null ? null : currentControllable.getBackButton() ;
		if(back == null || back.isDisabled())
			return false ;
		back.toggle() ;
		return true ;
	}
	
	public static void moveFocus(int x, int y)
	{
		hover(Utils_Controllable.getFocused(), false) ;
		cursorPos.x = x ;
		cursorPos.y = y ;
		hover(Utils_Controllable.getFocused(), true) ;
	}
	
	public static void showFocus(boolean show)
	{
		if(focusShown == show)
			return ;
		focusShown = show ;
		hover(Utils_Controllable.getFocused(), show) ;
	}
	
	/**
	 * Tells a widget the pointer came in or left, exactly as the stage does for the mouse.
	 * That is what lights up a start menu entry, a checkbox or the Apply button, so the
	 * keyboard focus looks like the mouse hover each widget already has.
	 */
	private static void hover(Actor actor, boolean entering)
	{
		if(actor == null || actor.getStage() == null)
			return ;
		InputEvent event = new InputEvent() ;
		event.setType(entering ? InputEvent.Type.enter : InputEvent.Type.exit) ;
		event.setStage(actor.getStage()) ;
		event.setPointer(-1) ;
		actor.fire(event) ;
	}
	
	public static void resetInterface()
	{
		if(currentControllable == null) return ;
		
		currentControllable = currentControllable.getClosingLink() ;
		if(currentControllable != null)
		{
			activedInterface(currentControllable) ;
		}
		else
		{
			hover(Utils_Controllable.getFocused(), false) ;
			buttonMap = null ; 
			cursorPos = null ; 
		}	
	}
}