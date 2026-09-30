package jks.vue.models;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Texture;

import jks.input.IKM_Game_Keyboard;
import jks.input.IKM_Game_XBoxController;
import jks.sounds.Enum_Music;
import jks.sounds.GVars_AudioManager;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vue.AVue_Model;

/**
 * What the intro and the outro share (r196): a run of full-screen pages, each fading up over
 * fadeInXSec, waiting for a click, and fading out over fadeOutXSec. Each cinematic says what a
 * click does once a page is up, and what comes after a page has faded out.
 */
public abstract class AVue_Scenematic extends AVue_Model
{

	public Texture page_1 ;
	public Texture page_2 ;
	public Texture page_3 ;

	public Texture currentpage ;

	ArrayList<Texture> imageSequence ;

	int currentIndex = 0;

	boolean inDescent = false ;
	float fadeInXSec = 2;
	float fadeOutXSec = 1;
	public float currentAlpha ;

	/** The cinematic's input and music, and an empty run of pages for init() to fill. */
	protected void startScenematic()
	{
		resize(0,0) ;
		GVars_Heart.inCinematic = true ;

		Gdx.input.setInputProcessor(new InputMultiplexer(GVars_UI.mainUi, new IKM_Game_Keyboard()));
		Controllers.clearListeners();
		Controllers.addListener(new IKM_Game_XBoxController()) ;

		imageSequence = new ArrayList<Texture>() ;
		GVars_AudioManager.PlayMusic(Enum_Music.GAME_INTRO);
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
			// A click while a page leaves is swallowed, not kept for the next one.
			if(GVars_Heart.inCinematic_Click)
			{
				GVars_Heart.inCinematic_Click = false ;
			}

			currentAlpha -= (1/fadeOutXSec) * delta ;

			if(currentAlpha < 0)
				fadedOut() ;
		}
		else
		{
			currentAlpha += (1/fadeInXSec) * delta ;

			if(currentAlpha > 1)
			{currentAlpha = 1 ;}

			whileUp(delta) ;

			if(GVars_Heart.inCinematic_Click)
			{
				GVars_Heart.inCinematic_Click = false ;
				clicked() ;
			}
		}
	}

	/** Each frame the page is not leaving, once its alpha is counted. */
	protected void whileUp(float delta)
	{}

	/** A click on a page that is not leaving: set inDescent to make it go. */
	protected abstract void clicked() ;

	/** The page has faded all the way out: the next page, or what follows the cinematic. */
	protected abstract void fadedOut() ;

	@Override
	public void resize(int x, int y)
	{
	}

}
