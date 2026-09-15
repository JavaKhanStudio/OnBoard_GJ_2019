package jks.vue.models.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;

import jks.vinterface.GVars_UI;
import jks.vinterface.font.GVars_Font;

/**
 * GVars_Game.karma on a slider (d11), for a run started in a later carriage: nothing carries
 * over from the carriages skipped, so without it the leaving ending (karma > 2) cannot be
 * reached from carriage 3 or 4.
 *
 * A lab panel, not part of the game: it shows with -Donboard.lab=true or item_lab, in the right
 * end of the black bar under the carriage, which the inventory fills from the left. It acts
 * live and follows the karma the player earns; the outro reads whatever it holds on arrival.
 */
public class KarmaSlider extends VisTable
{
	private final VisSlider slider ;
	private final VisLabel shown ;

	public KarmaSlider()
	{
		setName("karmaSlider") ;
		slider = new VisSlider(0f, GVars_Game.LEVEL_COUNT, 1f, false) ;
		slider.setValue(GVars_Game.karma) ;
		shown = new VisLabel("", GVars_Font.labelStyle_Second) ;
		slider.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{GVars_Game.karma = Math.round(slider.getValue()) ;}
		}) ;

		add(new VisLabel("Karma", GVars_Font.labelStyle_Second)).padRight(14f) ;
		add(slider).width(160f) ;
		add(shown).left().padLeft(14f).width(230f) ;
		pack() ;
	}

	@Override
	public void act(float delta)
	{
		super.act(delta) ;
		// A point earned in play moves the slider along with it.
		if(!slider.isDragging() && Math.round(slider.getValue()) != GVars_Game.karma)
		{
			slider.setProgrammaticChangeEvents(false) ;
			slider.setValue(GVars_Game.karma) ;
			slider.setProgrammaticChangeEvents(true) ;
		}
		shown.setText(GVars_Game.karma + (GVars_Game.leavingEnding() ? "  leaving ending" : "  staying ending")) ;

		float bar = Gdx.graphics.getHeight() / 9f ;
		setPosition(Gdx.graphics.getWidth() - getWidth() - 16f, (bar - getHeight()) / 2f) ;
	}

	/** Opens the panel over the carriage showing; it stays through every carriage after it. */
	public static void open()
	{
		GVars_UI.mainUi.addActor(new KarmaSlider()) ;
	}
}
