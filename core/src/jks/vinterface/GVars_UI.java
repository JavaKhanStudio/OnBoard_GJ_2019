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
	public static ArrayList<ArrayList<Button>> buttonMap ;
	
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
		Button currentButton = Utils_Controllable.getCurrentButton() ;
		currentButton.getClickListener().enter(null, 0, 0, -1, null) ;
	}

	public static void selectButton()
	{
		InputEvent event1 = new InputEvent();
		event1.setType(InputEvent.Type.touchUp);
		buttonMap.get(cursorPos.x).get(cursorPos.y).fire(event1);
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
			buttonMap = null ; 
			cursorPos = null ; 
		}	
	}
}