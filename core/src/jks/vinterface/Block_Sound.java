package jks.vinterface;

import java.util.ArrayList;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;

import jks.amain.Utils_Config;
import jks.sounds.GVars_AudioManager;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.tools.PaintedCheckBox;

/**
 * The sound section of the options screen.
 *
 * Was an empty class, and the options screen built three copies of an empty table where
 * the sound, preferences and language sections were meant to go. Now that the volume
 * setting actually reaches the music, there is something for it to control.
 *
 * Changes apply while you drag and are written to the config file as soon as you let go,
 * so they survive a restart - which nothing in the options screen did before.
 */
public class Block_Sound extends VisTable
{

	private final VisSlider volume, effects ;
	private final PaintedCheckBox muted ;
	private final VisLabel title, volumeLabel, effectsLabel ;
	
	// Copies of GVars_Font's styles in ink, refreshed in resize() when the fonts are rebuilt.
	private final LabelStyle titleStyle = Utils_Board.ink(GVars_Font.labelStyle_ScreenTitle) ;
	private final LabelStyle textStyle = Utils_Board.ink(GVars_Font.labelStyle_Second) ;
	
	private final Cell<VisLabel> titleCell ;
	private final Cell<PaintedCheckBox> mutedCell ;
	private final Cell<VisLabel> volumeCell, effectsCell ;

	public Block_Sound()
	{
		setBackground(Utils_Board.board()) ;
		align(Align.top) ;

		title = new VisLabel("Son", titleStyle) ;
		title.setAlignment(Align.center) ;
		titleCell = add(title).colspan(2).expandX().fillX() ;
		row() ;

		muted = new PaintedCheckBox("Couper le son", GVars_Font.font_Second, Utils_Board.INK) ;
		muted.setChecked(Utils_Config.current.volume <= 0f) ;
		muted.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				apply() ;
			}
		}) ;
		mutedCell = add(muted).colspan(2).fillX() ;
		row() ;

		volumeLabel = new VisLabel("Volume", textStyle) ;
		volumeCell = add(volumeLabel).left() ;
		volume = new VisSlider(0f, 1f, 0.05f, false, Utils_Board.slider(24f)) ;
		volume.setValue(Math.max(0f, Math.min(1f, Utils_Config.current.volume))) ;
		volume.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				// Live, so you can hear what you are setting rather than guessing.
				GVars_AudioManager.setMasterVolume(volume.getValue()) ;
				if(volume.getValue() > 0f && muted.isChecked())
					muted.setChecked(false) ;
				else
					apply() ;
			}
		}) ;
		add(volume).growX().padLeft(12f) ;
		row() ;

		// The train sounds, on top of Volume (r39). Muting still silences them with the music.
		effectsLabel = new VisLabel("Effets", textStyle) ;
		effectsCell = add(effectsLabel).left() ;
		effects = new VisSlider(0f, 1f, 0.05f, false, Utils_Board.slider(24f)) ;
		effects.setValue(Math.max(0f, Math.min(1f, Utils_Config.current.effectsVolume))) ;
		effects.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				GVars_AudioManager.setEffectsVolume(effects.getValue()) ;
				Utils_Config.current.effectsVolume = effects.getValue() ;
				Utils_Config.save() ;
			}
		}) ;
		add(effects).growX().padLeft(12f) ;
	}
	
	/**
	 * Fits the block to a board of this size, with rows as tall as the graphics board's so the
	 * two line up side by side.
	 */
	public void resize(float width, float height, float rowHeight)
	{
		titleStyle.font = GVars_Font.font_MainMenu ;
		title.setStyle(titleStyle) ;
		textStyle.font = GVars_Font.font_Second ;
		volumeLabel.setStyle(textStyle) ;
		effectsLabel.setStyle(textStyle) ;
		muted.setFont(GVars_Font.font_Second) ;
		
		setSize(width, height) ;
		float scale = Utils_Board.scale(width) ;
		Utils_Board.pad(this, scale) ;
		titleCell.height(Utils_Board.plankHeight(scale)).padBottom(Utils_Board.gapBelowPlank(scale)) ;
		mutedCell.height(rowHeight) ;
		volumeCell.height(rowHeight) ;
		effectsCell.height(rowHeight) ;
		muted.setBoxHeight(rowHeight * 0.8f) ;
		volume.setStyle(Utils_Board.slider(rowHeight)) ;
		effects.setStyle(Utils_Board.slider(rowHeight)) ;
		invalidateHierarchy() ;
	}

	private void apply()
	{
		float chosen = muted.isChecked() ? 0f : volume.getValue() ;

		GVars_AudioManager.setMasterVolume(chosen) ;
		GVars_AudioManager.setMuted(muted.isChecked()) ;

		Utils_Config.current.volume = chosen ;
		Utils_Config.save() ;
	}

	/** The order the keyboard walks this block in, top to bottom. */
	public ArrayList<Actor> focusOrder()
	{
		ArrayList<Actor> order = new ArrayList<>() ;
		order.add(muted) ;
		order.add(volume) ;
		order.add(effects) ;
		return order ;
	}

	/** The effects slider's value, for tests. */
	public float chosenEffectsVolume()
	{
		return effects.getValue() ;
	}

	/** The volume this block currently represents, for tests and for the caller. */
	public float chosenVolume()
	{
		return muted.isChecked() ? 0f : volume.getValue() ;
	}

}
