package jks.vue.models;

import jks.tools.Utils_Debug;

import static jks.index.Index_Interface.*;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Matrix4;
import com.kotcrab.vis.ui.widget.VisImage;

import jks.camera.GVars_Camera;
import jks.input.IKM_Game_Keyboard;
import jks.input.IKM_Game_XBoxController;
import jks.sounds.Enum_Music;
import jks.sounds.GVars_AudioManager;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.font.Index_Fonts.Enum_Fonts;
import jks.vue.AVue_Model;
import jks.vue.models.game.GVars_Game;

public class Vue_Scenematic_Outro extends AVue_Model
{
	
	public Texture page_1 ;
	public Texture page_2 ;
	public Texture page_3 ;
	
	public Texture currentpage ;
	
	ArrayList<Texture> imageSequence ;
	
	int currentIndex = 0; 
	
	/** How long the closing words hold, once up, before the game goes back to the menu by itself. */
	private static final float CLOSURE_SECONDS = 6f ;
	
	/** True once the pictures are done and the card is what is on screen (r60). */
	boolean closing ; 
	float closureHold ; 
	
	/**
	 * This view draws in window pixels. The batch is shared with the carriage, which leaves its
	 * own panned camera in it, so say which space rather than inherit one.
	 */
	private final Matrix4 screen = new Matrix4() ; 
	private final GlyphLayout layout = new GlyphLayout() ; 
	
	@Override
	public void init() 
	{
		resize(0,0) ; 
		GVars_Heart.inCinematic = true ; 
		
		Gdx.input.setInputProcessor(new InputMultiplexer(GVars_UI.mainUi, new IKM_Game_Keyboard()));
		Controllers.clearListeners();
		Controllers.addListener(new IKM_Game_XBoxController()) ; 
		
		imageSequence = new ArrayList<Texture>() ; 
		GVars_AudioManager.PlayMusic(Enum_Music.GAME_INTRO);
		
		// The pages cross-fade through whatever is behind them, and the carriage left the clear
		// colour white - which is what the end of the game looked like (r60). Black, like the
		// start screen this now returns to.
		Gdx.gl.glClearColor(0, 0, 0, 1) ; 
		
		Utils_Debug.log(manager + "manager");
		
		page_1 = manager.get(outroPage1, Texture.class) ;
		currentpage = page_1 ; 
		
		if(GVars_Game.leavingEnding()) // Leave Ending
		{
			page_2 = manager.get(outroPage_leave, Texture.class) ;
			imageSequence.add(page_2) ; 
		}
		else // stay Ending
		{
			page_2 = manager.get(outroPage_stay_1, Texture.class) ;
			page_3 = manager.get(outroPage_stay_2, Texture.class) ;
			
			imageSequence.add(page_2) ; 
			imageSequence.add(page_3) ; 
		}
	
	}
	

	@Override
	public void destroy() 
	{}

	@Override
	public void restart() 
	{}

	
	
	@Override
	public void update(float delta) 
	{
		GVars_UI.mainUi.act(delta);
		
		if(inDescent)
		{
			if(GVars_Heart.inCinematic_Click)
			{
				GVars_Heart.inCinematic_Click = false ; 
			}
			
			currentAlpha -= (1/fadeOutXSec) * delta ; 
			
			if(currentAlpha < 0)
			{
				if( imageSequence.size() > 0)
				{
					currentIndex ++ ; 
					currentpage = imageSequence.get(0) ;
					
//					if(currentIndex == 2)
//					{smokeScreenOn = true ;}
//					else
//					{smokeScreenOn = false ;}
					
					imageSequence.remove(0) ; 
					inDescent = false ; 
				}
				else if(!closing)
				{
					// The last picture is gone. Nothing used to replace it and the screen stayed
					// blank for good (r60); the closing words come up in its place instead.
					closing = true ; 
					currentpage = null ; 
					closureHold = CLOSURE_SECONDS ; 
					inDescent = false ; 
				}
				else
				{
					// And the words are gone too. Back to the menu with a whole run put away,
					// so the next Jouer starts in the first carriage with its items back.
					GVars_Game.resetForNewRun() ; 
					GVars_Heart.changeVue(new Vue_StartScreen(), true) ; 
				}
			}
			
		}
		else
		{
			currentAlpha += (1/fadeInXSec) * delta ; 
			
			if(currentAlpha > 1)
			{currentAlpha = 1 ;}
			
			// The words leave on their own, so the ending finishes even if nobody clicks again.
			if(closing && currentAlpha >= 1)
			{
				closureHold -= delta ; 
				if(closureHold <= 0)
					inDescent = true ; 
			}
			
			if(GVars_Heart.inCinematic_Click)
			{
				inDescent = true ;
				GVars_Heart.inCinematic_Click = false ; 
			}
		}
			
	}
	
	/** The pictures are done and the closing card is what is on screen (r60). */
	public boolean isClosing()
	{
		return closing ; 
	}
	
	boolean inDescent = false ; 
	float fadeInXSec = 2; 
	float fadeOutXSec = 1; 
	float smokeSpeed ; 
	float currentAlpha ; 

	@Override
	public void render() 
	{
		drawInterface() ;
		
		screen.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) ; 
		GVars_Camera.staticBatch.setProjectionMatrix(screen) ; 
		GVars_Camera.staticBatch.begin() ;
		
		GVars_Camera.staticBatch.setColor(1, 1, 1, currentAlpha); 
		
		if(currentpage != null)
		{GVars_Camera.staticBatch.draw(currentpage, 0, 0,  Gdx.graphics.getWidth(), Gdx.graphics.getHeight());}
		else
		{drawClosure() ;}
				
		GVars_Camera.staticBatch.end() ;
	}
	
	/**
	 * The card, centred as one block and fading on the same alpha as the pictures did. The
	 * words are ClosureMessage's; what is decided here is only where they sit.
	 */
	private void drawClosure()
	{
		BitmapFont words = GVars_Font.buildLabel(Enum_Fonts.STORY_CLOSURE).font ; 
		BitmapFont end = GVars_Font.buildLabel(Enum_Fonts.STORY_END).font ; 
		
		String[] lines = ClosureMessage.lines(GVars_Game.karma, GVars_Game.leavingEnding()) ; 
		
		float step = words.getLineHeight() * 1.4f ; 
		float endStep = end.getLineHeight() * 1.6f ; 
		float block = lines.length * step + endStep ; 
		float y = (Gdx.graphics.getHeight() + block) / 2f ; 
		
		for(String line : lines)
		{
			drawCentred(words, line, y) ; 
			y -= step ; 
		}
		
		drawCentred(end, ClosureMessage.END, y - endStep * 0.4f) ; 
	}
	
	private void drawCentred(BitmapFont font, String text, float y)
	{
		if(text.isEmpty())
			return ; 
		
		font.setColor(1, 1, 1, currentAlpha) ; 
		layout.setText(font, text) ; 
		font.draw(GVars_Camera.staticBatch, layout, (Gdx.graphics.getWidth() - layout.width) / 2f, y) ; 
	}

	private void buildSmokeScreen()
	{
		
	}
	
	
	@Override
	public void resize(int x, int y) 
	{
//		logoSize = Gdx.graphics.getWidth()/5 ; 
	}
	
	
	
}