package jks.vue.models.game;

import jks.index.Index_Text;
import jks.tools.Utils_Debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
		hints = new String[] {null, Index_Text.get(level.hint1), Index_Text.get(level.hint2), Index_Text.get(level.hint3)} ; 
		part1.addListener(buildListener(1)) ; 
		part2.addListener(buildListener(2)) ; 
		part3.addListener(buildListener(3)) ; 
		
		
		resize() ; 
		
		this.add(part1) ;
		this.add(part2) ;
		this.add(part3) ;
	}
	
	/** Each piece's hint, by piece number 1 to 3: what to go and find. */
	final String[] hints ; 
	/**
	 * What was said as each piece came (r91): the line of the item that gave it, the good or
	 * the bad action alike. Null for a piece not held, or given by something with nothing to say.
	 */
	final String[] results = new String[4] ; 
	
	/** Under the mouse, a piece says its hint until it is held, then how it was won (r91). */
	public String lineOf(int piece)
	{
		return has(piece) && results[piece] != null ? results[piece] : hints[piece] ; 
	}
	
	private ClickListener buildListener(int piece)
	{
		return new ClickListener()
		{
			/** What enter showed, so exit takes away that and not a line said since. */
			String shown ; 
			
			@Override
			public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor)
			{
				shown = lineOf(piece) ; 
				GVars_Game.dialogBubble.applyText(shown); 
			}
			
			@Override
			public void exit (InputEvent event, float x, float y, int pointer, Actor toActor) 
			{
				if(shown != null)
					GVars_Game.tryMakeDiseaper(shown); 
			}
		} ;
	}
	
	/**
	 * One flash of a piece: down to this alpha and back, twice over (r91, as r93's pulse). Soft
	 * since r104: at 0.2 alpha and three quick dips the piece all but vanished, a strobe.
	 */
	static final float FLASH_ALPHA = 0.6f ; 
	static final float FLASH_SECONDS = 0.25f ; 
	static final int FLASH_COUNT = 2 ; 
	
	/**
	 * Something was picked up (r91): the next piece missing flashes, so the eye goes to the key
	 * and its hint - the piece whose hint Ross said as the carriage opened. Only that one since
	 * r104: every missing piece at once made the whole key flash. Ross no longer says the next
	 * hint by himself at each step.
	 */
	public void flashMissing()
	{
		int piece = firstMissing() ; 
		if(piece == 0)
			return ; 
		VisImage part = piece == 1 ? part1 : piece == 2 ? part2 : part3 ; 
		part.clearActions() ; 
		part.getColor().a = 1 ; 
		part.addAction(Actions.repeat(FLASH_COUNT, Actions.sequence(
				Actions.alpha(FLASH_ALPHA, FLASH_SECONDS, Interpolation.sine), 
				Actions.alpha(1, FLASH_SECONDS, Interpolation.sine)))) ; 
	}
	
	/** A piece is flashing now. For the render tests. */
	public boolean isFlashing(int piece)
	{
		VisImage part = piece == 1 ? part1 : piece == 2 ? part2 : part3 ; 
		return part.hasActions() ; 
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
	
	/** A piece comes, with nothing said for it. */
	public void applySucces(int value)
	{
		applySucces(value, null) ; 
	}
	
	/**
	 * A piece comes. resultKey is the text-table key of what Ross said as it came: shown under
	 * the mouse from then on in place of the hint (r91). Null when nothing was said.
	 */
	public void applySucces(int value, String resultKey)
	{	
		boolean hadIt = has(value) ; 
		if(!hadIt && value >= 1 && value <= 3 && resultKey != null && Index_Text.has(resultKey))
			results[value] = Index_Text.get(resultKey) ; 
		
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
		
		// The last piece plays the level's sound instead, from completeCarriage: two chimes on
		// one pickup would only blur each other. A piece already held makes no sound at all.
		// Ross says nothing more by himself: the hints wait under the mouse (r91).
		boolean complete = hasPiece1 && hasPiece2 && hasPiece3 ; 
		if(!hadIt && has(value) && !complete)
			GVars_AudioManager.PlayEffect(Enum_Effect_Sound.Slot.KEY_PIECE) ; 
		
		if(complete)
			GVars_Game.completeCarriage() ; 
	}
	
	/** The lowest piece not yet held, 1 to 3, or 0 once the key is whole. */
	public int firstMissing()
	{
		for(int piece = 1 ; piece <= 3 ; piece++)
			if(!has(piece))
				return piece ; 
		return 0 ; 
	}
	
	public boolean has(int value)
	{
		return value == 1 ? hasPiece1 : value == 2 ? hasPiece2 : value == 3 ? hasPiece3 : false ; 
	}
	
}