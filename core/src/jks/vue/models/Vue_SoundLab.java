package jks.vue.models;

import java.util.ArrayList;
import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import jks.index.Index_Interface;
import jks.sounds.Enum_Music;
import jks.sounds.Enum_Sounds_Game;
import jks.sounds.Enum_Sounds_Interface;
import jks.sounds.GVars_Audio;
import jks.sounds.GVars_AudioManager;
import jks.vinterface.GVars_UI;
import jks.vinterface.Utils_TexturesAcess;
import jks.vinterface.font.GVars_Font;
import jks.vue.AVue_Model;

/**
 * The sound lab: every piece of music and every sound effect the game names, on one screen,
 * played through the game's own audio path so what you hear is what a player hears.
 *
 * A development screen, not part of the game. Nothing leads here; open it with
 *   ./gradlew :desktop:runGame -Donboard.start=sound_lab
 *
 * The volume slider acts live and is NOT saved - the options screen is where the setting
 * lives. Sound effects have no play button because none have files: the names exist, the
 * recordings were never made. Give one a file and it earns a button here.
 */
public class Vue_SoundLab extends AVue_Model
{
	/** Name, detail, Play, Stop, and a filler that keeps the buttons beside the detail. */
	private static final int COLUMNS = 5 ;

	private VisLabel nowPlaying ;

	@Override
	public void init()
	{
		toRender = new ArrayList<>() ;
		GVars_AudioManager.StopMusic() ;
		Gdx.input.setInputProcessor(GVars_UI.mainUi) ;

		VisTable page = new VisTable() ;
		page.setFillParent(true) ;
		page.align(Align.top) ;
		page.pad(56f, 64f, 56f, 64f) ;
		page.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.frame_Gray)) ;

		page.add(label("Sound lab", GVars_Font.labelStyle_ScreenTitle)).colspan(COLUMNS).row() ;
		page.add(label("Everything the game can play, through its own audio path. Nothing here is saved.",
			GVars_Font.labelStyle_Second)).colspan(COLUMNS).padBottom(18f).row() ;

		page.add(label("Music", GVars_Font.labelStyle_OptionsTitle)).colspan(COLUMNS).left().padBottom(6f).row() ;
		for(Enum_Music track : Enum_Music.values())
			addMusicRow(page, track) ;

		nowPlaying = label("", GVars_Font.labelStyle_Second) ;
		page.add(nowPlaying).colspan(COLUMNS).left().padTop(8f).row() ;
		addVolumeRow(page) ;

		page.add(label("Sound effects", GVars_Font.labelStyle_OptionsTitle)).colspan(COLUMNS).left().padTop(22f).padBottom(6f).row() ;
		for(Enum_Sounds_Game effect : Enum_Sounds_Game.values())
			addMissingEffectRow(page, effect.name(), "in game") ;
		for(Enum_Sounds_Interface effect : Enum_Sounds_Interface.values())
			addMissingEffectRow(page, effect.name(), "interface") ;

		GVars_UI.mainUi.addActor(page) ;
	}

	private void addMusicRow(VisTable page, final Enum_Music track)
	{
		FileHandle file = GVars_AudioManager.fileFor(track) ;

		VisTextButton play = new VisTextButton("Play") ;
		play.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{GVars_AudioManager.PlayMusic(track) ;}
		}) ;

		VisTextButton stop = new VisTextButton("Stop") ;
		stop.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{GVars_AudioManager.StopMusic() ;}
		}) ;

		page.add(label(shown(track.name()), GVars_Font.labelStyle_Second)).left().padRight(24f) ;
		page.add(label(describe(file), GVars_Font.labelStyle_Second)).left().padRight(24f) ;
		page.add(play).padRight(8f) ;
		page.add(stop) ;
		page.add().expandX().row() ;
	}

	private void addVolumeRow(VisTable page)
	{
		final VisLabel percent = label("", GVars_Font.labelStyle_Second) ;
		final VisSlider volume = new VisSlider(0f, 1f, 0.05f, false) ;
		volume.setValue(GVars_Audio.muted ? 0f : GVars_Audio.masterVolume) ;
		percent.setText(Math.round(volume.getValue() * 100) + " %") ;
		volume.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				GVars_AudioManager.setMasterVolume(volume.getValue()) ;
				GVars_AudioManager.setMuted(volume.getValue() <= 0f) ;
				percent.setText(Math.round(volume.getValue() * 100) + " %") ;
			}
		}) ;

		page.add(label("Volume", GVars_Font.labelStyle_Second)).left().padRight(24f) ;
		page.add(volume).left().width(280f) ;
		page.add(percent).colspan(COLUMNS - 2).left().padLeft(12f).row() ;
	}

	private void addMissingEffectRow(VisTable page, String name, String where)
	{
		page.add(label(shown(name), GVars_Font.labelStyle_Second)).left().padRight(24f) ;
		page.add(label(where + " - no file, never recorded", GVars_Font.labelStyle_Second)).colspan(COLUMNS - 1).left().row() ;
	}

	/** Path, and whether it is really there - the enum can name a file that does not exist. */
	private static String describe(FileHandle file)
	{
		if(file == null || !file.exists())
			return (file == null ? "no file" : file.path()) + "  MISSING" ;

		float megabytes = file.readBytes().length / (1024f * 1024f) ;
		return file.path() + String.format(Locale.ROOT, "  %.1f MB", megabytes) ;
	}

	/** The menu fonts have no underscore - FreeType leaves a hole where it would be. */
	private static String shown(String enumName)
	{return enumName.replace('_', ' ') ;}

	private static VisLabel label(String text, LabelStyle style)
	{return new VisLabel(text, style) ;}

	@Override
	public void destroy()
	{}

	@Override
	public void restart()
	{}

	@Override
	public void update(float delta)
	{
		GVars_UI.mainUi.act(delta) ;

		Enum_Music playing = GVars_AudioManager.currentMusic() ;
		if(playing == null)
			nowPlaying.setText("Nothing playing") ;
		else
		{
			int seconds = (int) GVars_AudioManager.musicPosition() ;
			nowPlaying.setText(String.format(Locale.ROOT, "Playing %s  %d:%02d  (loops)",
				shown(playing.name()), seconds / 60, seconds % 60)) ;
		}
	}

	@Override
	public void render()
	{
		Gdx.gl.glClearColor(0.12f, 0.12f, 0.12f, 1) ;
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT) ;
		renderBeforeInterface() ;
		drawInterface() ;
	}
}
