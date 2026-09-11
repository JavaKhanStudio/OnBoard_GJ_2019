package jks.vinterface;

import java.util.ArrayList;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;

import jks.amain.Utils_Config;
import jks.sounds.GVars_AudioManager;

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

	private final VisSlider volume ;
	private final VisCheckBox muted ;

	public Block_Sound()
	{
		super(true) ;
		align(Align.top) ;

		add(new VisLabel("Son")).colspan(2).padBottom(8f).row() ;

		muted = new VisCheckBox(" Couper le son") ;
		muted.setChecked(Utils_Config.current.volume <= 0f) ;
		muted.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				apply() ;
			}
		}) ;
		add(muted).colspan(2).left().row() ;

		add(new VisLabel("Volume")).left().padRight(8f) ;
		volume = new VisSlider(0f, 1f, 0.05f, false) ;
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
		add(volume).growX().row() ;
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
		return order ;
	}

	/** The volume this block currently represents, for tests and for the caller. */
	public float chosenVolume()
	{
		return muted.isChecked() ? 0f : volume.getValue() ;
	}

}
