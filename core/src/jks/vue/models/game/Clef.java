package jks.vue.models.game;

import jks.index.Index_Text;
import jks.tools.Utils_Debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.VisImage;

import jks.index.Index_Interface;
import jks.sounds.Enum_Effect_Sound;
import jks.sounds.GVars_AudioManager;
import jks.vinterface.GVars_UI;

public class Clef extends Table
{

	boolean hasPiece1 ; 
	boolean hasPiece2 ; 
	boolean hasPiece3 ; 
	
//	VisImageButton part1 ;
	VisImage part1 ;
	VisImage part2 ;
	VisImage part3 ; 
	
	float sizePart ; 
	float decal ; 
	
	public Clef(WagonLevel level)
	{
		
		this.setLayoutEnabled(false);
			
		part1 = new VisImage(Index_Interface.manager.get(Index_Interface.key1, Texture.class)) ;
		part2 = new VisImage(Index_Interface.manager.get(Index_Interface.key2, Texture.class)) ;	
		part3 = new VisImage(Index_Interface.manager.get(Index_Interface.key3, Texture.class)) ;
		
		// The .wa files name a row of i18n/textes.tsv, not the words themselves (r76).
		part1.addListener(buildListener(Index_Text.get(level.hint1))) ; 
		part2.addListener(buildListener(Index_Text.get(level.hint2))) ; 
		part3.addListener(buildListener(Index_Text.get(level.hint3))) ; 
		
		
		resize() ; 
		
		this.add(part1) ;
		this.add(part2) ;
		this.add(part3) ;
	}
	
	private ClickListener buildListener(String text)
	{
		return new ClickListener()
		{
			@Override
			public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor)
			{
//				Utils_Debug.log(text + " ENTER");
				GVars_Game.dialogBubble.applyText(text); 
			}
			
			@Override
			public void exit (InputEvent event, float x, float y, int pointer, Actor toActor) 
			{
//				Utils_Debug.log(text + " EXIT");
				GVars_Game.tryMakeDiseaper(text); 
			}
		} ;
	}
	
	public void resize()
	{
		decal = Gdx.graphics.getWidth()/100 ; 
		sizePart = Gdx.graphics.getWidth()/12 ; 
		this.setSize((sizePart * 3) + (decal * 2), sizePart);
		this.setPosition(Gdx.graphics.getWidth() - this.getWidth(), Gdx.graphics.getHeight() - sizePart - decal);
		
		part1.setSize(sizePart, sizePart);
		part1.setX(decal);
		
		part2.setSize(sizePart, sizePart);
		part2.setX(sizePart + decal);
		
		part3.setSize(sizePart, sizePart);
		part3.setX((sizePart * 2) + decal);
	}
	
	public void applySucces(int value)
	{	
		boolean hadIt = has(value) ; 
		
		if(value == 1)
		{
			hasPiece1 = true ; 
			part1.setDrawable(Index_Interface.manager.get(Index_Interface.key1Full, Texture.class)) ;
		}
		else if(value == 2)
		{
			hasPiece2 = true ; 
			part2.setDrawable(Index_Interface.manager.get(Index_Interface.key2Full, Texture.class)) ;
		}
		else if(value == 3)
		{
			hasPiece3 = true ; 
			part3.setDrawable(Index_Interface.manager.get(Index_Interface.key3Full, Texture.class)) ;
		}
		
		// The last piece plays the level's sound instead, from nextLevel: two chimes on one
		// pickup would only blur each other. A piece already held makes no sound at all.
		boolean complete = hasPiece1 && hasPiece2 && hasPiece3 ; 
		if(!hadIt && has(value) && !complete)
			GVars_AudioManager.PlayEffect(Enum_Effect_Sound.Slot.KEY_PIECE) ; 
		
		if(complete)
		{
			GVars_Game.nextLevel() ; 
		}
	}
	
	private boolean has(int value)
	{
		return value == 1 ? hasPiece1 : value == 2 ? hasPiece2 : value == 3 ? hasPiece3 : false ; 
	}
	
}