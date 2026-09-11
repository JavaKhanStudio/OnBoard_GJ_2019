package jks.vue.models.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import jks.index.Index_Interface;
import jks.vars.GVars_Heart;
import jks.vinterface.Utils_TexturesAcess;

/**
 * The little sign in the top-left corner of a level that opens the pause screen with the
 * mouse; Escape does the same from the keyboard (d4). It is the plank off the top of the
 * pause panel, PAUSE and all, at the size of a HUD item, and it stays half see-through until
 * the pointer is on it so it does not compete with the carriage.
 *
 * It is a stage button, and the stage comes before IKM_Game_Keyboard in Vue_Game's input
 * multiplexer: the button takes the touchDown, so a click on it never reaches the carriage
 * items underneath.
 */
public class PauseButton extends ImageButton
{
	/** The plank at the top of pauseMenu.png, in the image's own pixels. */
	private static final int PLANK_X = 0, PLANK_Y = 30, PLANK_WIDTH = 1095, PLANK_HEIGHT = 200 ;
	private static final float RESTING_ALPHA = 0.8f ;

	public PauseButton()
	{
		super(new TextureRegionDrawable(new TextureRegion(Utils_TexturesAcess.getTexture(Index_Interface.pause_Panel),
				PLANK_X, PLANK_Y, PLANK_WIDTH, PLANK_HEIGHT))) ;
		setName("pauseButton") ;

		// A ChangeListener, like every other button: it is what a click fires.
		addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				// Under the pause screen's dim it is still there; it only ever opens it.
				if(!GVars_Heart.isPaused)
					GVars_Heart.togglePauseMenu() ;
			}
		}) ;
		resize() ;
	}

	/**
	 * About a sixth of the window wide, at the plank's own shape, in the top-left corner. Any
	 * smaller and the PAUSE painted on it cannot be read at 1280x720.
	 */
	public void resize()
	{
		float width = Gdx.graphics.getWidth() / 6.5f ;
		float height = width * PLANK_HEIGHT / PLANK_WIDTH ;
		float margin = Gdx.graphics.getWidth() / 100f ;
		setSize(width, height) ;
		getImageCell().size(width, height) ;
		setPosition(margin, Gdx.graphics.getHeight() - height - margin) ;
	}

	@Override
	public void act(float delta)
	{
		super.act(delta) ;
		getColor().a = isOver() ? 1f : RESTING_ALPHA ;
	}
}
