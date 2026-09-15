package jks.vue.models;

import java.util.ArrayList;
import java.util.EnumMap;
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

import jks.amain.Utils_Config;
import jks.index.Index_Interface;
import jks.sounds.Enum_Music;
import jks.sounds.Enum_Sounds_Game;
import jks.sounds.Enum_Sounds_Interface;
import jks.sounds.Enum_Train_Sound;
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
 * The volume sliders act live and are NOT saved - the options screen is where those settings
 * live. The train rows are the exception: "Use" is how the train sounds are chosen (r39), so
 * it is saved to the config, and the game plays whichever is marked. The other effects have no
 * play button because none have files: the names exist, the recordings were never made. Give
 * one a file and it earns a button here.
 */
public class Vue_SoundLab extends AVue_Model
{
	/** Name, detail, Play, Stop, Use, and a filler that keeps the buttons beside the detail. */
	private static final int COLUMNS = 6 ;

	private VisLabel nowPlaying, trainPlaying ;
	private final EnumMap<Enum_Train_Sound, VisLabel> trainMarks = new EnumMap<>(Enum_Train_Sound.class) ;

	@Override
	public void init()
	{
		toRender = new ArrayList<>() ;
		GVars_AudioManager.StopMusic() ;
		Gdx.input.setInputProcessor(GVars_UI.mainUi) ;

		VisTable page = new VisTable() ;
		page.setFillParent(true) ;
		page.align(Align.top) ;
		page.pad(28f, 64f, 20f, 64f) ;
		page.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.frame_Gray)) ;

		page.add(label("Sound lab", GVars_Font.labelStyle_ScreenTitle)).colspan(COLUMNS).row() ;
		page.add(label("Everything the game can play, through its own audio path. Only Use is saved.",
			GVars_Font.labelStyle_Second)).colspan(COLUMNS).padBottom(18f).row() ;

		page.add(label("Music", GVars_Font.labelStyle_OptionsTitle)).colspan(COLUMNS).left().padBottom(6f).row() ;
		for(Enum_Music track : Enum_Music.values())
			addMusicRow(page, track) ;

		nowPlaying = label("", GVars_Font.labelStyle_Second) ;
		addVolumeRow(page, nowPlaying) ;

		page.add(label("Train", GVars_Font.labelStyle_OptionsTitle)).colspan(COLUMNS).left().padTop(12f).row() ;
		for(Enum_Train_Sound.Slot slot : Enum_Train_Sound.Slot.values())
		{
			page.add(label(slot == Enum_Train_Sound.Slot.RAILS ? "Rails - loops under every level" : "Departure - once, as a new game opens level 1",
				GVars_Font.labelStyle_Second)).colspan(COLUMNS).left().padTop(8f).row() ;
			for(Enum_Train_Sound candidate : Enum_Train_Sound.values())
				if(candidate.slot == slot)
					addTrainRow(page, candidate) ;
		}
		trainPlaying = label("", GVars_Font.labelStyle_Second) ;
		addEffectsVolumeRow(page, trainPlaying) ;
		refreshTrainMarks() ;

		StringBuilder missing = new StringBuilder() ;
		for(Enum_Sounds_Game effect : Enum_Sounds_Game.values())
			missing.append(missing.length() == 0 ? "" : ", ").append(shown(effect.name())) ;
		for(Enum_Sounds_Interface effect : Enum_Sounds_Interface.values())
			missing.append(", ").append(shown(effect.name())) ;
		addMissingEffectRow(page, "Other effects: " + missing, "no file, never recorded") ;

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
		page.add() ;
		page.add().expandX().row() ;
	}

	private void addTrainRow(VisTable page, final Enum_Train_Sound candidate)
	{
		VisTextButton play = new VisTextButton("Play") ;
		play.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				GVars_AudioManager.StopTrain() ;
				if(candidate.slot == Enum_Train_Sound.Slot.RAILS)
					GVars_AudioManager.PlayRails(candidate) ;
				else
					GVars_AudioManager.PlayDeparture(candidate) ;
			}
		}) ;

		VisTextButton stop = new VisTextButton("Stop") ;
		stop.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{GVars_AudioManager.StopTrain() ;}
		}) ;

		VisTextButton use = new VisTextButton("Use") ;
		use.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{choose(candidate) ;}
		}) ;

		VisLabel mark = label("", GVars_Font.labelStyle_Second) ;
		trainMarks.put(candidate, mark) ;

		page.add(label(candidate.label, GVars_Font.labelStyle_Second)).left().padRight(24f) ;
		// The enum name, not the path: the file names have underscores, which this font cannot draw.
		page.add(label(shown(candidate.name()) + describeSize(Gdx.files.internal(candidate.path)), GVars_Font.labelStyle_Second)).left().padRight(24f) ;
		page.add(play).padRight(8f) ;
		page.add(stop).padRight(8f) ;
		page.add(use).padRight(12f) ;
		page.add(mark).expandX().left().row() ;
	}

	/** Saved at once: this is the one choice the lab exists to make. */
	public static void choose(Enum_Train_Sound candidate)
	{
		if(candidate.slot == Enum_Train_Sound.Slot.RAILS)
		{
			GVars_Audio.railsChoice = candidate ;
			Utils_Config.current.railsSound = candidate.name() ;
		}
		else
		{
			GVars_Audio.departureChoice = candidate ;
			Utils_Config.current.departureSound = candidate.name() ;
		}
		Utils_Config.save() ;
	}

	private void refreshTrainMarks()
	{
		for(Enum_Train_Sound candidate : trainMarks.keySet())
		{
			boolean chosen = candidate == GVars_Audio.railsChoice || candidate == GVars_Audio.departureChoice ;
			trainMarks.get(candidate).setText(chosen ? "in the game" : "") ;
		}
	}

	private void addEffectsVolumeRow(VisTable page, VisLabel status)
	{
		final VisLabel percent = label("", GVars_Font.labelStyle_Second) ;
		final VisSlider volume = new VisSlider(0f, 1f, 0.05f, false) ;
		volume.setValue(GVars_Audio.effectVolume) ;
		percent.setText(Math.round(volume.getValue() * 100) + " %") ;
		volume.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				GVars_AudioManager.setEffectsVolume(volume.getValue()) ;
				percent.setText(Math.round(volume.getValue() * 100) + " %") ;
			}
		}) ;

		page.add(label("Effects", GVars_Font.labelStyle_Second)).left().padRight(24f) ;
		page.add(volume).left().width(280f) ;
		page.add(percent).left().padLeft(12f) ;
		page.add(status).colspan(COLUMNS - 3).left().padLeft(24f).row() ;
	}

	private void addVolumeRow(VisTable page, VisLabel status)
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
		page.add(percent).left().padLeft(12f) ;
		page.add(status).colspan(COLUMNS - 3).left().padLeft(24f).row() ;
	}

	private void addMissingEffectRow(VisTable page, String names, String why)
	{
		page.add(label(names + "  -  " + why, GVars_Font.labelStyle_Second)).colspan(COLUMNS).left().padTop(14f).row() ;
	}

	/** Path, and whether it is really there - the enum can name a file that does not exist. */
	private static String describe(FileHandle file)
	{
		if(file == null || !file.exists())
			return (file == null ? "no file" : file.path()) + "  MISSING" ;

		float megabytes = file.readBytes().length / (1024f * 1024f) ;
		return file.path() + String.format(Locale.ROOT, "  %.1f MB", megabytes) ;
	}

	private static String describeSize(FileHandle file)
	{
		if(!file.exists())
			return "  MISSING" ;
		return String.format(Locale.ROOT, "  %.1f MB", file.readBytes().length / (1024f * 1024f)) ;
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
		refreshTrainMarks() ;

		Enum_Train_Sound rails = GVars_AudioManager.currentRails() ;
		trainPlaying.setText(rails == null ? "No rails bed playing" : "Looping " + rails.label) ;

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
