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
import jks.vue.GVars_Steam.Ring;

/**
 * The steam lab (r262): a carriage, and the steam that leaves it when its key is whole, played
 * on demand. Pick where the first ring rises from - Whole is the steam before r262 - and the
 * frame it holds on while Ross's line is read (each shape keeps its own), then press Play: the same line every time, the
 * change at the same moment, and the next carriage comes up under the steam.
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
		return json.append(" \"moved\": [").append(moved).append("]}").toString() ;
	}

	/** Opens the lab over whatever carriage is showing. */
	public static void open()
	{
		GVars_UI.mainUi.addActor(new SteamLab()) ;
	}
}
