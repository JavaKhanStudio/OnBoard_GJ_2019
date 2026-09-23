package jks.vue.models;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
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
import jks.sounds.Enum_Sounds_Interface;
import jks.sounds.Enum_Effect_Sound;
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
 * live. The effect rows are the exception: "Use" is how the game's sound effects are chosen
 * (r39, r45), so it is saved to the config, and the game plays whichever is marked. One tab per
 * moment - rails, departure, key piece, level complete, footsteps - so every candidate fits on one
 * screen. A footstep candidate plays as a short walk, at the pace of Ross's walk cycle: one step
 * alone is too short to judge. The effects named in Enum_Sounds_Interface have no play button
 * because none have files: the names exist, the recordings were never made.
 */
public class Vue_SoundLab extends AVue_Model
{
	/** Name, detail, Play, Stop, Use, and a filler that keeps the buttons beside the detail. */
	private static final int COLUMNS = 6 ;
	private static final float NAME_WIDTH = 330f, DETAIL_WIDTH = 390f ;

	private VisLabel nowPlaying, effectsPlaying ;
	private Cell<VisTable> candidates ;
	private final EnumMap<Enum_Effect_Sound.Slot, VisTable> slotTables = new EnumMap<>(Enum_Effect_Sound.Slot.class) ;
	private final EnumMap<Enum_Effect_Sound, VisLabel> marks = new EnumMap<>(Enum_Effect_Sound.class) ;
	private final EnumMap<Enum_Effect_Sound.Slot, VisTextButton> tabs = new EnumMap<>(Enum_Effect_Sound.Slot.class) ;

	/** A footstep candidate walking, the steps it has left, and the time to the next one. */
	private static final int WALK_STEPS = 8 ;
	private static final float STEP_SECONDS = 4 * 0.106f ;
	private Enum_Effect_Sound walking ;
	private int stepsLeft ;
	private float nextStep ;

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

		page.add(label("Sound effects", GVars_Font.labelStyle_OptionsTitle)).colspan(COLUMNS).left().padTop(12f).row() ;
		VisTable tabRow = new VisTable() ;
		for(final Enum_Effect_Sound.Slot slot : Enum_Effect_Sound.Slot.values())
		{
			VisTextButton tab = new VisTextButton(shown(slot.name()), "toggle") ;
			tab.addListener(new ChangeListener()
			{
				@Override
				public void changed(ChangeEvent event, Actor actor)
				{
					if(((VisTextButton) actor).isChecked())
						showSlot(slot) ;
				}
			}) ;
			tabs.put(slot, tab) ;
			tabRow.add(tab).padRight(8f) ;
		}
		page.add(tabRow).colspan(COLUMNS).left().padTop(6f).row() ;
		for(Enum_Effect_Sound.Slot slot : Enum_Effect_Sound.Slot.values())
		{
			VisTable table = new VisTable() ;
			table.add(label(slot.label, GVars_Font.labelStyle_Second)).colspan(COLUMNS).left().padTop(8f).row() ;
			for(Enum_Effect_Sound candidate : Enum_Effect_Sound.values())
				if(candidate.slot == slot)
					addEffectRow(table, candidate) ;
			slotTables.put(slot, table) ;
		}
		candidates = page.add((VisTable) null).colspan(COLUMNS).growX().left() ;
		page.row() ;
		effectsPlaying = label("", GVars_Font.labelStyle_Second) ;
		addEffectsVolumeRow(page, effectsPlaying) ;
		showSlot(Enum_Effect_Sound.Slot.RAILS) ;

		StringBuilder missing = new StringBuilder() ;
		for(Enum_Sounds_Interface effect : Enum_Sounds_Interface.values())
			missing.append(missing.length() == 0 ? "" : ", ").append(shown(effect.name())) ;
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

		// Fixed widths, so the effect rows - a table of their own per tab - line up with these.
		page.add(label(shown(track.name()), GVars_Font.labelStyle_Second)).left().width(NAME_WIDTH) ;
		page.add(label(describe(file), GVars_Font.labelStyle_Second)).left().width(DETAIL_WIDTH) ;
		page.add(play).padRight(8f) ;
		page.add(stop) ;
		page.add() ;
		page.add().expandX().row() ;
	}

	/** Fills the candidates table with one slot's sounds, and checks only that slot's tab. */
	public void showSlot(Enum_Effect_Sound.Slot slot)
	{
		for(Enum_Effect_Sound.Slot other : tabs.keySet())
		{
			VisTextButton tab = tabs.get(other) ;
			tab.setProgrammaticChangeEvents(false) ;
			tab.setChecked(other == slot) ;
			tab.setProgrammaticChangeEvents(true) ;
		}

		candidates.setActor(slotTables.get(slot)) ;
		refreshMarks() ;
	}

	private void addEffectRow(VisTable table, final Enum_Effect_Sound candidate)
	{
		VisTextButton play = new VisTextButton("Play") ;
		play.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{play(candidate) ;}
		}) ;

		VisTextButton stop = new VisTextButton("Stop") ;
		stop.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{stopEffects() ;}
		}) ;

		VisTextButton use = new VisTextButton("Use") ;
		use.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{choose(candidate) ;}
		}) ;

		VisLabel mark = label("", GVars_Font.labelStyle_Second) ;
		marks.put(candidate, mark) ;

		table.add(label(candidate.label, GVars_Font.labelStyle_Second)).left().width(NAME_WIDTH) ;
		// The enum name, not the path: the file names have underscores, which this font cannot draw.
		table.add(label(shown(candidate.name()) + describeSize(Gdx.files.internal(candidate.path)), GVars_Font.labelStyle_Second)).left().width(DETAIL_WIDTH) ;
		table.add(play).padRight(8f) ;
		table.add(stop).padRight(8f) ;
		table.add(use).padRight(12f) ;
		table.add(mark).expandX().left().row() ;
	}

	/** What a candidate's Play button does: one at a time, and a footstep walks. */
	public void play(Enum_Effect_Sound candidate)
	{
		stopEffects() ;
		if(candidate.slot == Enum_Effect_Sound.Slot.RAILS)
			GVars_AudioManager.PlayRails(candidate) ;
		else if(candidate.slot == Enum_Effect_Sound.Slot.FOOTSTEPS)
		{
			walking = candidate ;
			stepsLeft = WALK_STEPS ;
			nextStep = 0f ;
		}
		else
			GVars_AudioManager.PlayEffect(candidate) ;
	}

	private void stopEffects()
	{
		walking = null ;
		GVars_AudioManager.StopEffects() ;
	}

	private void walk(float delta)
	{
		if(walking == null)
			return ;
		nextStep -= delta ;
		if(nextStep > 0f)
			return ;
		GVars_AudioManager.PlayFootstep(walking) ;
		nextStep += STEP_SECONDS ;
		if(--stepsLeft <= 0)
			walking = null ;
	}

	/** Saved at once: this is the one choice the lab exists to make. */
	public static void choose(Enum_Effect_Sound candidate)
	{
		GVars_Audio.choose(candidate) ;
		Utils_Config.current.chooseSound(candidate) ;
		Utils_Config.save() ;
	}

	private void refreshMarks()
	{
		for(Enum_Effect_Sound candidate : marks.keySet())
			marks.get(candidate).setText(GVars_Audio.choice(candidate.slot) == candidate ? "in the game" : "") ;
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
		return String.format(Locale.ROOT, "  %d KB", file.readBytes().length / 1024) ;
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
		refreshMarks() ;
		walk(delta) ;

		Enum_Effect_Sound rails = GVars_AudioManager.currentRails() ;
		if(walking != null)
			effectsPlaying.setText("Walking - " + walking.label) ;
		else
			effectsPlaying.setText(rails == null ? "No rails bed playing" : "Looping " + rails.label) ;

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
