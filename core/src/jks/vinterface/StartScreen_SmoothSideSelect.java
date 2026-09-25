package jks.vinterface;

import jks.index.Index_Text;
import jks.tools.Utils_Debug;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

import jks.sounds.Enum_Effect_Sound;
import jks.sounds.GVars_AudioManager;
import jks.vars.GVars_Heart;
import jks.vinterface.controlling.Controllable_Interface;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.overlay.OverlayCredits;
import jks.vinterface.overlay.OverlayOptions;
import jks.vinterface.overlay.ReplayAction;
import jks.vue.GVars_Fade;
import jks.vue.Utils_View;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.Vue_Game;

public class StartScreen_SmoothSideSelect extends Table implements ReplayAction, Controllable_Interface
{

	float sizeX ; 
	float sizeY ;
	float decalX ;
	float decalY ;
	float topPosY ;
	
	ArrayList<Table> buttonContainerList ;
	ArrayList<Actor> selectableOptionsX ;
	ArrayList<ArrayList<Actor>> selectableOptionsMapped ;
	int index = 0; 
	Integer movingBy = 0; 
	
	final Float baseSpeed = 0.4f ;
	final float baseAlpha = 0.7f ;
	final float enterSpeed = 0.34f; 
	final float enterspeedDelayIncrement = 0.12f ; 
	final float leavingSpeed = 0.2f ; 
	final float leavingSpeedDelayIncrement = 0.05f ; 
	
	ReplayAction ref ;
	 
	public StartScreen_SmoothSideSelect()
	{
		resize() ;
		ref = this ; 
		this.setLayoutEnabled(false);
		buttonContainerList = new ArrayList<>() ; 
		selectableOptionsX = new ArrayList<>() ; 
		selectableOptionsMapped = new ArrayList<>() ;
		selectableOptionsMapped.add(selectableOptionsX) ; 	
		
		Button jouer = buildButton(Index_Text.get("menu.play")) ;
		selectableOptionsX.add(jouer) ; 
		jouer.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				// The menu fades out and the first carriage fades in (r44).
				GVars_Fade.through(() ->
				{
					// A new game from its first page, whatever the last one left: the carriage
					// count, karma, and items taken from the cached carriages (r110). The ending
					// resets too (r60); this covers any other way back to the menu.
					GVars_Game.resetForNewRun() ; 
					// Was building two Vue_Game instances and discarding the first.
					Vue_Game myGame = new Vue_Game() ; 
					GVars_Heart.changeVue(myGame,true) ; 
					GVars_Game.loadLevel(1);
					// The train leaves the station. Only here: a new game, not the dev start points.
					GVars_AudioManager.PlayEffect(Enum_Effect_Sound.Slot.DEPARTURE);
				}) ;
			}
		}) ;
		
		Button options = buildButton(Index_Text.get("menu.options")) ;
		selectableOptionsX.add(options) ; 
		options.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				exitScene();
				Utils_View.setOverlay(new OverlayOptions(ref));
			}
		}) ;
		
		// NOT "Crédits", however much it wants to be. The menu font is OptimusPrinceps, whose
		// cmap has no é (U+00E9) and no usable É (U+00C9) - FreeType draws a bare accent where
		// the letter should be, so the button reads "Cr¨dits". Rendered and looked at under d6.
		// The font draws everything as capitals anyway, and an unaccented capital is ordinary
		// French typography. Simon settled it under d8: "Credits is fine" - so this is the
		// word, not a stopgap, and the font is not getting patched or swapped to spell it.
		// Index_Credits.TITLE, the heading of the screen this opens, is the same word for the
		// same reason. Anything in GeosansLight - the options rows, "Simon Bédard" in the
		// credits - takes accents fine.
		Button credits = buildButton(Index_Text.get("menu.credits")) ;
		selectableOptionsX.add(credits) ; 
		credits.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				exitScene();
				Utils_View.setOverlay(new OverlayCredits(ref));
			}
		}) ;
		
		Button quitter = buildButton(Index_Text.get("menu.quit")) ;
		selectableOptionsX.add(quitter) ; 
		quitter.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				// Not System.exit: that skips libGDX's shutdown, so the OpenAL device is still
				// open when the process tears down, and on Windows OpenAL then aborts
				// (c0000409) - a crash on every Quit. This ends the frame, runs dispose()
				// and closes audio properly, exactly like the window's close button.
				Gdx.app.exit();
			}
		}) ;
		
	}

	public void resize() 
	{
		this.setWidth(Gdx.graphics.getWidth()/4);
		this.setHeight(Gdx.graphics.getHeight());
		decalX = Gdx.graphics.getWidth()/15 ;
		sizeX = Gdx.graphics.getWidth()/5.2f ;
		sizeY = Gdx.graphics.getHeight()/18.4f ; 
		topPosY = Gdx.graphics.getHeight()/2.5f ;
		movingBy = 10 ; 
		decalY = 0 ;
		
		if(buttonContainerList != null)
		{
			for(int a = 0 ; a < buttonContainerList.size() ; a++)
			{
				Table buttonTable = buttonContainerList.get(a) ; 
				buttonTable.getChild(0).setBounds(0, 0, sizeX, sizeY);
				buttonTable.getChild(1).setBounds(0, 0, sizeX, sizeY);
				setParkingPosition(buttonTable,a + 1) ; 
			}
		}
		
	}
	
	public Button buildButton(String text)
	{		
		Table table = new Table(); 
		table.setLayoutEnabled(false);
		
		Label textLabel = new Label(text, GVars_Font.labelStyle_ScreenTitle) ;
		textLabel.setTouchable(Touchable.disabled);
		
		Button textButton = new Button(GVars_UI.baseSkin);
		textButton.setColor(0, 0, 0, 0.0f);
					
		table.add(textButton) ;
		table.add(textLabel) ;
		textLabel.setAlignment(Align.left);
		textButton.setBounds(0, 0, sizeX, sizeY);
		textLabel.setBounds(0, 0, sizeX, sizeY);
		
		setParkingPosition(table, index + 1) ; 
		textButton.addListener(new InputListener()
		{		
			public void enter (InputEvent event, float x, float y, int pointer, Actor fromActor)
			{
				textLabel.clearActions();
				textLabel.addAction(buildSelectSequence(movingBy,0)) ; 
				textButton.clearActions();
				textButton.addAction(buildAlpha(baseAlpha));
			}

			public void exit (InputEvent event, float x, float y, int pointer, Actor toActor) 
			{
				textLabel.clearActions();
				textLabel.addAction(buildDeselectSequence(0,0)) ;
				textButton.clearActions();
				textButton.addAction(buildAlpha(0));
			}
		}) ; 
	
		index ++ ;
					
		buttonContainerList.add(table) ;
		this.add(table) ;
		
		return textButton ; 
	}
	
	private void setParkingPosition(Table table, int position) 
	{
		float positionX = -sizeX ; 
		float positionY = topPosY - (sizeY * position) - (decalY * position) ;		
		table.setBounds(positionX, positionY, sizeX, sizeY);
	}

	public void enterScene(float startAfterXSecondes)
	{
		resize();
		for(int a = 0 ; a < buttonContainerList.size() ; a++)
		{
			DelayAction preInitDelay = new DelayAction(startAfterXSecondes) ;
			MoveToAction action1 = new MoveToAction();
		    action1.setPosition(decalX, topPosY - (sizeY * a) - (decalY * a));
		    action1.setDuration(enterSpeed);
		    
		    DelayAction delay = new DelayAction(a * (enterspeedDelayIncrement)) ; 
		    
		    SequenceAction sequence = new SequenceAction();
		    sequence.addAction(preInitDelay);
		    sequence.addAction(delay);
		    sequence.addAction(action1);
		   
		    buttonContainerList.get(a).addAction(sequence);
		}

		
		GVars_UI.activedInterface(this);
	}
	
	public void exitScene()
	{

		for(int a = 0 ;  a < buttonContainerList.size() ; a++)
		{
			MoveToAction action1 = new MoveToAction();
		    action1.setPosition(-sizeX, topPosY - (sizeY * (a)) - (decalY * a));
		    action1.setDuration(leavingSpeed);
		    
		    MoveToAction action2 = new MoveToAction();
		    action2.setPosition(-sizeX, topPosY - (sizeY * (a + 1)) - (decalY * a));
		    action2.setDuration(0);
		    
		    DelayAction delay = new DelayAction(a * (leavingSpeedDelayIncrement)) ; 
		    
		    SequenceAction sequence = new SequenceAction();
		    sequence.addAction(delay);
		    sequence.addAction(action1);
		    sequence.addAction(action2);
		    
		    buttonContainerList.get(a).addAction(sequence);
		}
	}
	 
	
	public SequenceAction buildSelectSequence(int positionX, int positionY)
	{
		MoveToAction action1 = new MoveToAction();
	    action1.setPosition(positionX, positionY);
	    action1.setDuration(baseSpeed);
	        
	    SequenceAction sequence = new SequenceAction() ;
	    sequence.addAction(action1);
	    
	    return sequence ; 
	}
	
	public Action buildAlpha(float a)
	{
		AlphaAction alpha = new AlphaAction() ; 
		alpha.setAlpha(a);
		alpha.setDuration(baseSpeed);
		return alpha ;
	}
	
	public SequenceAction buildDeselectSequence(int positionX, int positionY)
	{
		MoveToAction action1 = new MoveToAction();
		action1.setPosition(positionX, positionY);
	    action1.setDuration(baseSpeed);
	    
	    SequenceAction sequence = new SequenceAction();
	    sequence.addAction(action1);
	    
	    return sequence ; 
	}

	@Override
	public ArrayList<ArrayList<Actor>> mapInterface() 
	{
		return selectableOptionsMapped;
	}
	
	/** The entries slide out and light up on hover, and the keyboard focus uses that. */
	@Override
	public boolean highlightsItself()
	{
		return true ;
	}
}

//for(int a = 0 ;  a < list.size() ; a++)
//{
//	MoveToAction action1 = new MoveToAction();
//    action1.setPosition(-sizeX, topPosY - (sizeY * (a+1)) - (decalY * a));
//    action1.setDuration(leavingSpeed);
//    
//    DelayAction delay = new DelayAction(a * (leavingSpeedDelayIncrement)) ; 
//    
//    SequenceAction sequence = new SequenceAction();
//    sequence.addAction(delay);
//    sequence.addAction(action1);
//    
//    list.get(a).addAction(sequence);
//}

//for(int a = list.size() - 1 ;  a >= 0 ; a--)
//{
//	MoveToAction action1 = new MoveToAction();
//    action1.setPosition(-sizeX, topPosY - (sizeY * (a+1)) - (decalY * a));
//    action1.setDuration(leavingSpeed);
//    
//    DelayAction delay = new DelayAction(((list.size() - a) + 1)  * (leavingSpeedDelayIncrement)) ; 
//    
//    SequenceAction sequence = new SequenceAction();
//    sequence.addAction(delay);
//    sequence.addAction(action1);
//    
//    list.get(a).addAction(sequence);
//}