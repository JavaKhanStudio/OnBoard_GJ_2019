package jks.vue.models.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import jks.camera.GVars_Camera;
import jks.vinterface.GVars_UI;
import jks.vinterface.font.GVars_Font;
import jks.vue.GVars_Steam;
import jks.vue.GVars_Steam.Motion;
import jks.vue.GVars_Steam.Ring;

/**
 * The steam lab (r262): a carriage, and the steam that leaves it when its key is whole, played
 * on demand. Pick where the first ring rises from - Whole is the steam before r262 - and the
 * frame it holds on while Ross's line is read (each shape keeps its own), then press Play: the same line every time, the
 * change at the same moment, and the next carriage comes up under the steam.
 *
 * Under the shapes, how the steam moves (r262, second round): Snap is the flip-book d16
 * shipped; Breathe, Gather and Drift ease through the frames, slower, and keep the ring alive
 * while the line is read. Each keeps its own knobs: the rise, the close and the lift in
 * seconds, how many frames the ring swells by, how long a breath lasts, how far it rolls out.
 *
 * A development screen, not part of the game. Nothing leads here; open it with
 *   ./gradlew :desktop:runGame -Donboard.start=steam_lab
 *
 * Nothing is saved. Copy settings puts the choice on the clipboard as JSON, to paste back to
 * the board.
 */
public class SteamLab extends VisTable
{
	/** A line of carriage 1's, long enough for the ring to be seen holding. */
	static final String LINE = "wa1.cage.message2" ;

	public SteamLab()
	{
		setName("steamLab") ;
		setBackground(VisUI.getSkin().newDrawable("white", 0.1f, 0.1f, 0.1f, 0.75f)) ;
		pad(10f, 16f, 10f, 16f) ;
		align(Align.topLeft) ;

		add(new VisLabel("Steam at a carriage's end", GVars_Font.labelStyle_OptionsTitle)).colspan(3).left().padBottom(6f).row() ;

		final VisSlider frame = new VisSlider(1f, 8f, 1f, false) ;
		final VisLabel shown = new VisLabel("", GVars_Font.labelStyle_Second) ;

		VisTable shapes = new VisTable() ;
		ButtonGroup<VisTextButton> group = new ButtonGroup<>() ;
		for(final Ring ring : Ring.values())
		{
			VisTextButton button = new VisTextButton(label(ring), "toggle") ;
			button.setName("ring-" + ring.name()) ;
			group.add(button) ;
			button.setChecked(GVars_Steam.ring == ring) ;
			button.addListener(new ChangeListener()
			{
				@Override
				public void changed(ChangeEvent event, Actor actor)
				{
					if(((VisTextButton) actor).isChecked())
					{
						GVars_Steam.ring = ring ;
						showFrame(frame, shown) ;
					}
				}
			}) ;
			shapes.add(button).padRight(6f) ;
		}
		add(shapes).colspan(3).left().row() ;

		frame.setName("ringFrame") ;
		showFrame(frame, shown) ;
		frame.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				if(GVars_Steam.ring == Ring.WHOLE)
					return ;
				GVars_Steam.ring.frame = Math.round(frame.getValue()) ;
				shown.setText(String.valueOf(GVars_Steam.ring.frame)) ;
			}
		}) ;
		add(new VisLabel("Ring frame", GVars_Font.labelStyle_Second)).left().padTop(8f).padRight(16f) ;
		add(frame).width(160f).padTop(8f) ;
		add(shown).left().padTop(8f).padLeft(12f).width(40f).row() ;

		add(new VisLabel("Motion", GVars_Font.labelStyle_Second)).colspan(3).left().padTop(10f).row() ;
		final VisSlider[] knobs = new VisSlider[KNOBS.length] ;
		final VisLabel[] knobShown = new VisLabel[KNOBS.length] ;
		VisTable motions = new VisTable() ;
		ButtonGroup<VisTextButton> motionGroup = new ButtonGroup<>() ;
		for(final Motion motion : Motion.values())
		{
			VisTextButton button = new VisTextButton(label(motion), "toggle") ;
			button.setName("motion-" + motion.name()) ;
			motionGroup.add(button) ;
			button.setChecked(GVars_Steam.motion == motion) ;
			button.addListener(new ChangeListener()
			{
				@Override
				public void changed(ChangeEvent event, Actor actor)
				{
					if(((VisTextButton) actor).isChecked())
					{
						GVars_Steam.motion = motion ;
						showKnobs(knobs, knobShown) ;
					}
				}
			}) ;
			motions.add(button).padRight(6f) ;
		}
		add(motions).colspan(3).left().row() ;

		for(int k = 0 ; k < KNOBS.length ; k++)
		{
			final int knob = k ;
			knobs[k] = new VisSlider(0f, KNOB_MAX[k], KNOB_MAX[k] / 40f, false) ;
			knobs[k].setName("knob-" + KNOBS[k]) ;
			knobShown[k] = new VisLabel("", GVars_Font.labelStyle_Second) ;
			knobs[k].addListener(new ChangeListener()
			{
				@Override
				public void changed(ChangeEvent event, Actor actor)
				{
					Motion motion = GVars_Steam.motion ;
					if(motion == Motion.SNAP)
						return ;
					float[] values = motion.knobs() ;
					values[knob] = knobs[knob].getValue() ;
					motion.set(values) ;
					knobShown[knob].setText(format(values[knob])) ;
				}
			}) ;
			add(new VisLabel(KNOB_LABELS[k], GVars_Font.labelStyle_Second)).left().padRight(16f) ;
			add(knobs[k]).width(160f) ;
			add(knobShown[k]).left().padLeft(12f).width(40f).row() ;
		}
		showKnobs(knobs, knobShown) ;

		VisTable buttons = new VisTable() ;
		VisTextButton play = new VisTextButton("Play") ;
		play.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{play() ;}
		}) ;
		VisTextButton copy = new VisTextButton("Copy settings") ;
		copy.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{Gdx.app.getClipboard().setContents(settings()) ;}
		}) ;
		buttons.add(play).padRight(8f) ;
		buttons.add(copy) ;
		add(buttons).colspan(3).left().padTop(8f).row() ;

		pack() ;
		// Left of the key, which fills the top right.
		setPosition(Gdx.graphics.getWidth() * 0.4f, Gdx.graphics.getHeight() - getHeight() - 12f) ;
	}

	/** The slider on the picked shape's own frame; Whole has none, it covers. */
	private static void showFrame(VisSlider frame, VisLabel shown)
	{
		Ring ring = GVars_Steam.ring ;
		frame.setProgrammaticChangeEvents(false) ;
		frame.setValue(ring == Ring.WHOLE ? 8f : ring.frame) ;
		frame.setProgrammaticChangeEvents(true) ;
		frame.setDisabled(ring == Ring.WHOLE) ;
		shown.setText(ring == Ring.WHOLE ? "-" : String.valueOf(ring.frame)) ;
	}

	/** The knobs of a {@link Motion}, in the order of {@link Motion#knobs()}. */
	static final String[] KNOBS = {"rise", "close", "lift", "swell", "period", "drift"} ;
	static final String[] KNOB_LABELS = {"Rise (s)", "Close (s)", "Lift (s)", "Swell (frames)", "Breath (s)", "Roll out"} ;
	static final float[] KNOB_MAX = {3f, 3f, 3f, 3f, 8f, 0.12f} ;

	/** The sliders on the picked motion's own knobs; Snap has none, it flips at twelve a second. */
	private static void showKnobs(VisSlider[] knobs, VisLabel[] shown)
	{
		Motion motion = GVars_Steam.motion ;
		float[] values = motion.knobs() ;
		for(int k = 0 ; k < knobs.length ; k++)
		{
			knobs[k].setProgrammaticChangeEvents(false) ;
			knobs[k].setValue(values[k]) ;
			knobs[k].setProgrammaticChangeEvents(true) ;
			knobs[k].setDisabled(motion == Motion.SNAP) ;
			shown[k].setText(motion == Motion.SNAP ? "-" : format(values[k])) ;
		}
	}

	private static String format(float value)
	{
		return String.valueOf(Math.round(value * 100f) / 100f) ;
	}

	private static String label(Motion motion)
	{
		switch(motion)
		{
			case SNAP:    return "Snap (d16)" ;
			case BREATHE: return "Breathe" ;
			case GATHER:  return "Gather" ;
			default:      return "Drift" ;
		}
	}

	private static String label(Ring ring)
	{
		switch(ring)
		{
			case WHOLE:  return "Whole (before)" ;
			case CORNER: return "Corner" ;
			case FLOOR:  return "Floor" ;
			default:     return "Four corners" ;
		}
	}

	/**
	 * Ross says the line, and the steam leaves the carriage as completeCarriage has it: its
	 * timing from the line, the bubble in front. The next carriage, back to 1 after the 4th.
	 */
	public static boolean play()
	{
		GVars_Game.sayItemMessage(LINE) ;
		float reading = GVars_Game.dialogBubble.secondsBusy() ;
		return GVars_Steam.creep(Math.max(reading, GVars_Game.CREEP_MIN_SECONDS), () ->
		{
			GVars_Camera.resetCamera() ;
			GVars_Game.currentLevelInt = GVars_Game.currentLevelInt % GVars_Game.LEVEL_COUNT + 1 ;
			GVars_Game.loadLevel(GVars_Game.currentLevelInt) ;
		}, GVars_Game.dialogBubble) ;
	}

	/** Every knob, where it starts in the game, and the ones moved. */
	public static String settings()
	{
		StringBuilder json = new StringBuilder("{\"lab\": \"steam\", \"ticket\": \"r262\",\n") ;
		StringBuilder moved = new StringBuilder() ;
		json.append(" \"ring\": \"").append(GVars_Steam.ring.name()).append("\", \"ringStart\": \"").append(GVars_Steam.SHIPPED_RING.name()).append("\",\n") ;
		if(GVars_Steam.ring != GVars_Steam.SHIPPED_RING)
			moved.append("\"ring\"") ;
		for(Ring ring : Ring.values())
		{
			if(ring == Ring.WHOLE)
				continue ;
			String key = "frame" + ring.name() ;
			json.append(" \"").append(key).append("\": ").append(ring.frame).append(", \"").append(key).append("Start\": ").append(ring.shippedFrame).append(",\n") ;
			if(ring.frame != ring.shippedFrame)
				moved.append(moved.length() > 0 ? ", " : "").append('"').append(key).append('"') ;
		}
		json.append(" \"motion\": \"").append(GVars_Steam.motion.name()).append("\", \"motionStart\": \"").append(GVars_Steam.SHIPPED_MOTION.name()).append("\",\n") ;
		if(GVars_Steam.motion != GVars_Steam.SHIPPED_MOTION)
			moved.append(moved.length() > 0 ? ", " : "").append("\"motion\"") ;
		for(Motion motion : Motion.values())
		{
			if(motion == Motion.SNAP)
				continue ;
			float[] values = motion.knobs() ;
			json.append(" \"").append(motion.name()).append("\": {") ;
			for(int k = 0 ; k < KNOBS.length ; k++)
			{
				String key = motion.name() + "." + KNOBS[k] ;
				json.append(k > 0 ? ", " : "").append('"').append(KNOBS[k]).append("\": ").append(format(values[k])) ;
				if(Math.abs(values[k] - motion.shipped[k]) > 0.001f)
					moved.append(moved.length() > 0 ? ", " : "").append('"').append(key).append('"') ;
			}
			json.append("}, \"").append(motion.name()).append("Start\": {") ;
			for(int k = 0 ; k < KNOBS.length ; k++)
				json.append(k > 0 ? ", " : "").append('"').append(KNOBS[k]).append("\": ").append(format(motion.shipped[k])) ;
			json.append("},\n") ;
		}
		return json.append(" \"moved\": [").append(moved).append("]}").toString() ;
	}

	/** Opens the lab over whatever carriage is showing. */
	public static void open()
	{
		GVars_UI.mainUi.addActor(new SteamLab()) ;
	}
}
