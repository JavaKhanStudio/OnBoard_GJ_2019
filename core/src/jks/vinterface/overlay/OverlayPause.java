package jks.vinterface.overlay;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;

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
		
		retour = new ImageButton(Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.mainMenus_Settings),
				Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.mainMenus_SettingsON));
		retour.addListener(new InputListener()
		{
			@Override
	        public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) 
	        {
				GVars_Heart.togglePauseMenu();
				return true;
	        }
		}) ;
		
		retour.setSize(Gdx.graphics.getWidth() / 7.5f, Gdx.graphics.getHeight() / 9.0f);
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
	public ArrayList<ArrayList<Button>> mapInterface() 
	{
		ArrayList<ArrayList<Button>> returningList = new ArrayList<ArrayList<Button>>(); 
		ArrayList<Button> buttonList = new ArrayList<>() ;
		buttonList.add(retour) ;
		returningList.add(buttonList) ; 
		return returningList;
	}
	
	@Override
	public Vector2Int startAt()
	{return new Vector2Int(0,0) ;}

	@Override
	public void resize() 
	{
	
	}
}
