package jks.vinterface.overlay;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import jks.index.Index_Interface;
import jks.tools.Vector2Int;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vinterface.Utils_TexturesAcess;

public class OverlayPause extends OverlayModel
{

	ImageButton retour ;
	
	public OverlayPause() 
	{
		super(GVars_UI.baseSkin) ;

		float xPosition = Gdx.graphics.getWidth() * 0.28f;
		float yposition = Gdx.graphics.getHeight() / 2.8f;
//		coupeSonLibelle.setPosition(xPosition, yposition);
		
		// The return sign, as on the credits: this was the Settings button art.
		retour = new ImageButton(Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.button_Return),
				Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.button_Return));
		retour.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				GVars_Heart.togglePauseMenu();
			}
		}) ;
		
		float buttonWidth = Gdx.graphics.getWidth() / 7.5f ;
		float buttonHeight = buttonWidth / Index_Interface.button_Return_Aspect ;
		retour.setSize(buttonWidth, buttonHeight);
		retour.getImageCell().size(buttonWidth, buttonHeight);
		retour.setPosition(Gdx.graphics.getWidth() / 2 - retour.getWidth() / 2, Gdx.graphics.getHeight() / 2.8f);
		
		this.setLayoutEnabled(false);
		this.setFillParent(true);
		this.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		this.addActor(retour);
	}

	@Override
	public void destroy() 
	{this.remove() ;}

	@Override
	public boolean disableMainClickAction() 
	{return true;}

	@Override
	public ArrayList<ArrayList<Actor>> mapInterface() 
	{
		ArrayList<ArrayList<Actor>> returningList = new ArrayList<ArrayList<Actor>>(); 
		ArrayList<Actor> buttonList = new ArrayList<>() ;
		buttonList.add(retour) ;
		returningList.add(buttonList) ; 
		return returningList;
	}
	
	@Override
	public Vector2Int startAt()
	{return new Vector2Int(0,0) ;}

	@Override
	public Button getBackButton()
	{return retour ;}

	@Override
	public void resize() 
	{
	
	}
}
