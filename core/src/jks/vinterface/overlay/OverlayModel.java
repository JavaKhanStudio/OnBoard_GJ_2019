package jks.vinterface.overlay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import jks.index.Index_Interface;
import jks.vinterface.Utils_TexturesAcess;
import jks.vinterface.controlling.Controllable_Interface;
import jks.vue.Utils_View;

public abstract class OverlayModel extends Table implements Controllable_Interface
{
	
	Color backgroundColor ; 
	Texture texture ; 
	
	public OverlayModel(Skin skin) 
	{
		super(skin);
		texture = Utils_TexturesAcess.getTexture(Index_Interface.empty) ; 
		backgroundColor = new Color(0, 0, 0, 0.5f) ; 
		this.setTouchable(Touchable.childrenOnly);
	}
	
	@Override
	protected void drawBackground (Batch batch, float parentAlpha, float x, float y) 
	{
		batch.setColor(backgroundColor);
		batch.draw(texture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}
	
	public abstract void resize() ; 
	
	public void destroy()
	{this.remove() ;}
	
	public boolean disableMainClickAction()
	{return true ;}
	
	/**
	 * The return sign of the credits and the options: closes the overlay and its filter and
	 * sends the start menu back in. Unsized and unplaced; each overlay lays it out in resize().
	 */
	protected static ImageButton buildReturnSign(final ReplayAction backway)
	{
		ImageButton retour = new ImageButton(
			Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.button_Return),
			Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.button_Return)) ;
		retour.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				Utils_View.removeCurrentOverlay() ;
				Utils_View.removeFilter() ;
				backway.enterScene(0) ;
			}
		}) ;
		return retour ;
	}
	
}