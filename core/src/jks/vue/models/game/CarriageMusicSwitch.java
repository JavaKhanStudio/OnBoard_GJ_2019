package jks.vue.models.game;

import java.util.EnumMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import jks.sounds.Enum_Music;
import jks.sounds.Enum_Music.CarriageVariant;
import jks.sounds.GVars_Audio;
import jks.sounds.GVars_AudioManager;
import jks.vinterface.GVars_UI;
import jks.vinterface.font.GVars_Font;

/**
 * Which music plays under the carriage (r94): the main song as shipped, the same song aged for
 * this carriage, or a track written for it. The comparison is the point, so a switch keeps the
 * bar - the main and modulated songs go on from where they were - and the rails stay under it.
 *
 * A lab panel, not part of the game: it shows with -Donboard.lab=true, which the board's Carriage
 * labs pass, at the top between the pause button and the key. Nothing is saved; every run starts
 * on the main song.
 */
public class CarriageMusicSwitch extends VisTable
{
	private final EnumMap<CarriageVariant, VisTextButton> buttons = new EnumMap<>(CarriageVariant.class) ;
	private final VisLabel playing ;

	public CarriageMusicSwitch()
	{
		setName("carriageMusicSwitch") ;
		add(new VisLabel("Music", GVars_Font.labelStyle_Second)).padRight(14f) ;
		for(final CarriageVariant variant : CarriageVariant.values())
		{
			VisTextButton button = new VisTextButton(label(variant), "toggle") ;
			button.addListener(new ChangeListener()
			{
				@Override
				public void changed(ChangeEvent event, Actor actor)
				{GVars_AudioManager.setCarriageVariant(variant) ;}
			}) ;
			buttons.put(variant, button) ;
			add(button).padRight(8f) ;
		}
		playing = new VisLabel("", GVars_Font.labelStyle_Second) ;
		add(playing).left().padLeft(6f).width(260f) ;
		pack() ;
	}

	private static String label(CarriageVariant variant)
	{
		switch(variant)
		{
			case MAIN :      return "Main" ;
			case MODULATED : return "Modulated" ;
			default :        return "Ideal" ;
		}
	}

	@Override
	public void act(float delta)
	{
		super.act(delta) ;
		for(CarriageVariant variant : buttons.keySet())
		{
			VisTextButton button = buttons.get(variant) ;
			button.setProgrammaticChangeEvents(false) ;
			button.setChecked(variant == GVars_Audio.carriageVariant) ;
			button.setProgrammaticChangeEvents(true) ;
		}
		playing.setText(describe()) ;
		// Top middle: the pause button holds the left corner and the key the right.
		setPosition(Math.round((Gdx.graphics.getWidth() - getWidth()) / 2f), Gdx.graphics.getHeight() - getHeight() - 12f) ;
	}

	/** What is coming out, by file: an ideal track not made yet says so rather than passing for one. */
	private static String describe()
	{
		int carriage = GVars_Game.currentLevelInt ;
		if(GVars_Audio.carriageVariant == CarriageVariant.IDEAL && GVars_AudioManager.idealFile(carriage) == null)
			return "no wa" + carriage + " ideal yet, main song" ;
		Enum_Music track = GVars_AudioManager.currentMusic() ;
		if(track == null)
			return "silent" ;
		// The font has no underscore and no full stop.
		return GVars_AudioManager.fileFor(track).nameWithoutExtension().replace('_', ' ') ;
	}

	/** Opens the panel; it stays through every carriage after it. */
	public static void open()
	{
		GVars_UI.mainUi.addActor(new CarriageMusicSwitch()) ;
	}
}
