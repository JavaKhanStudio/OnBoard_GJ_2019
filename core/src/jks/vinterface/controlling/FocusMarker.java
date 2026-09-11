package jks.vinterface.controlling;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import jks.vinterface.GVars_UI;

/**
 * The outline round the widget the keyboard is on, for the screens whose widgets do not
 * show focus themselves. A hover highlight alone is too faint on the options screen, and
 * the Retour sign has none at all.
 *
 * Follows its widget every frame, since the widgets slide in. The 2019 code meant a fairy
 * sprite to fly to the selected button (ControleFairy, never finished); this is the same
 * idea, drawn with the skin's own white and VisUI's blue.
 */
public class FocusMarker extends Actor
{
	private final Vector2 corner = new Vector2() ;
	private Drawable white ;
	private Color colour ;

	public FocusMarker()
	{
		setTouchable(Touchable.disabled) ;
	}

	@Override
	public void draw(Batch batch, float parentAlpha)
	{
		Actor target = Utils_Controllable.getFocused() ;
		if(!GVars_UI.focusShown || target == null || target.getStage() == null || !target.isVisible()
				|| GVars_UI.currentControllable == null || GVars_UI.currentControllable.highlightsItself())
			return ;

		if(white == null)
		{
			white = GVars_UI.baseSkin.getDrawable("white") ;
			colour = GVars_UI.baseSkin.getColor("vis-blue") ;
		}

		target.localToStageCoordinates(corner.set(0, 0)) ;
		float line = Math.max(2f, Gdx.graphics.getHeight() / 240f) ;
		float x = corner.x - line * 2, y = corner.y - line * 2 ;
		float width = target.getWidth() + line * 4, height = target.getHeight() + line * 4 ;

		Color previous = batch.getColor().cpy() ;
		batch.setColor(colour.r, colour.g, colour.b, colour.a * parentAlpha) ;
		white.draw(batch, x, y, width, line) ;
		white.draw(batch, x, y + height - line, width, line) ;
		white.draw(batch, x, y, line, height) ;
		white.draw(batch, x + width - line, y, line, height) ;
		batch.setColor(previous) ;
	}
}
