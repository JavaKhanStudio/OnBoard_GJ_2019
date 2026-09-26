package jks.vue.models.game;

import jks.tools.Utils_Format;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import jks.vinterface.GVars_UI;
import jks.vinterface.font.GVars_Font;

/**
 * The item lab (d10): a carriage as it plays, with the outline's numbers on sliders in the top
 * right corner. Hover an item to see it lit, or press "Every item" to see them all at once.
 *
 * A development screen, not part of the game. Nothing leads here; open it with
 *   ./gradlew :desktop:runGame -Donboard.start=item_lab -Donboard.level=4
 *
 * Nothing is saved: the numbers beside the sliders are what to pin in ItemOutline. Yellow is the
 * colour's blue, so 0 is a deep gold and 1 nearly white.
 */
public class ItemOutlineLab extends VisTable
{
	public ItemOutlineLab()
	{
		setName("itemOutlineLab") ;
		// A plain dark card: the grey frame the sound lab uses has a minimum size that covers
		// half the carriage.
		setBackground(VisUI.getSkin().newDrawable("white", 0.1f, 0.1f, 0.1f, 0.75f)) ;
		pad(10f, 16f, 10f, 16f) ;
		align(Align.topLeft) ;

		add(new VisLabel("Item outline", GVars_Font.labelStyle_OptionsTitle)).colspan(3).left().padBottom(6f).row() ;
		slider("Width", 0.5f, 8f, 0.5f, ItemOutline.width, value -> ItemOutline.width = value) ;
		slider("Opacity", 0.05f, 1f, 0.05f, ItemOutline.opacity, value -> ItemOutline.opacity = value) ;
		slider("Stroke", 0f, 40f, 1f, ItemOutline.strokeLength, value -> ItemOutline.strokeLength = value) ;
		slider("Filled", 0.1f, 1f, 0.05f, ItemOutline.strokeFill, value -> ItemOutline.strokeFill = value) ;
		slider("Speed", 0f, 80f, 2f, ItemOutline.speed, value -> ItemOutline.speed = value) ;
		slider("Yellow", 0f, 1f, 0.05f, ItemOutline.color.b, value -> ItemOutline.color.b = value) ;

		VisTextButton all = new VisTextButton("Every item", "toggle") ;
		all.setChecked(ItemOutline.lightAll) ;
		all.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{ItemOutline.lightAll = ((VisTextButton) actor).isChecked() ;}
		}) ;
		add(all).colspan(3).left().padTop(8f).row() ;

		pack() ;
		setPosition(Gdx.graphics.getWidth() - getWidth() - 12f, Gdx.graphics.getHeight() - getHeight() - 12f) ;
	}

	private interface Setter { void set(float value) ; }

	private void slider(String name, float min, float max, float step, float start, final Setter setter)
	{
		final VisSlider slider = new VisSlider(min, max, step, false) ;
		slider.setValue(start) ;
		final VisLabel shown = new VisLabel(format(start), GVars_Font.labelStyle_Second) ;
		slider.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				setter.set(slider.getValue()) ;
				shown.setText(format(slider.getValue())) ;
			}
		}) ;
		add(new VisLabel(name, GVars_Font.labelStyle_Second)).left().padRight(16f) ;
		add(slider).width(160f) ;
		add(shown).left().padLeft(12f).width(56f).row() ;
	}

	private static String format(float value)
	{return Utils_Format.fixed(value, 2) ;}

	/** Opens the lab over whatever carriage is showing. */
	public static void open()
	{
		GVars_UI.mainUi.addActor(new ItemOutlineLab()) ;
	}
}
