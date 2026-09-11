package jks.vinterface.overlay;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisTable;

import jks.index.Index_Interface;
import jks.tools.Vector2Int;
import jks.vinterface.GVars_UI;
import jks.vinterface.Block_Resolution;
import jks.vinterface.Block_Sound;
import jks.vinterface.Utils_TexturesAcess;
import jks.vue.Utils_View;

public class OverlayOptions extends OverlayModel
{

	ImageButton retour ;
	float returnButtonPositionY = 0 ;
	
	VisTable mainTable ; 
	
	Block_Resolution graphicBloc ; 
	Block_Sound soundBloc ; 
	VisTable prefBloc ; 
	VisTable languageBloc ; 
	
	String frames = Index_Interface.frame_Gray ;
	
	ReplayAction backway ;
	
	public OverlayOptions(ReplayAction ref) 
	{
		super(GVars_UI.baseSkin) ;
		backway = ref ; 
		this.setLayoutEnabled(false);
			
		// The return sign, as on the credits: this was the Settings button art.
		retour = new ImageButton(Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.button_Return),
				Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.button_Return));
		retour.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				Utils_View.removeCurrentOverlay() ;
				Utils_View.removeFilter() ;
				backway.enterScene(0);
			}
		}) ;
			
		mainTable = new VisTable() ;
		mainTable.setTouchable(Touchable.childrenOnly);
		mainTable.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(frames));
		
		graphicBloc = new Block_Resolution(); 
		graphicBloc.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(frames));
		
		soundBloc = new Block_Sound() ; 
		soundBloc.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(frames));
		
		// Preferences and language were never designed. They build empty boxes, same as
		// before - but they no longer borrow the sound section's builder to do it.
		prefBloc = new VisTable() ; 
		prefBloc.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(frames));
		
		languageBloc = new VisTable() ; 
		languageBloc.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(frames));
		
		mainTable.addActor(graphicBloc);
		mainTable.addActor(soundBloc);
		mainTable.addActor(prefBloc);
		retour.setPosition(-5, 0);
		resize() ; 
		
		MoveToAction getIn = new MoveToAction();
	    getIn.setPosition(0, returnButtonPositionY);
	    getIn.setDuration(0.3f);
	    retour.addAction(getIn); 
	    
		this.addActor(mainTable);
		this.addActor(retour);
	}
	
	float widthPercent = 4f/5 ;
	float heightPercent = 3.5f/5 ;
	
	public void resize() 
	{
		float sizebuttonX = Gdx.graphics.getWidth() / 7.5f ;
		float decalSideX = Gdx.graphics.getWidth() / 23f ;
		float size_Main_Width = Gdx.graphics.getWidth() - (decalSideX * 2) - sizebuttonX * 1.5f ; 
		float size_Main_Height = Gdx.graphics.getHeight() * heightPercent ; 
		float decalY = size_Main_Height / 20f ;
		
		mainTable.setWidth(size_Main_Width);
		mainTable.setHeight(size_Main_Height);
		mainTable.setPosition((Gdx.graphics.getWidth() - size_Main_Width)/2, (Gdx.graphics.getHeight() - size_Main_Height)/2);
		
		float bloc_Width = (size_Main_Width/2) - (decalSideX * 1.5f) ; 
		float bloc_Height = (size_Main_Height/2) - (decalY * 1.5f) ; 
		
		graphicBloc.resize();
		graphicBloc.setWidth(bloc_Width);
		graphicBloc.setHeight(bloc_Height);
		graphicBloc.setPosition(decalSideX, graphicBloc.getHeight() + decalY * 2);
		
		soundBloc.setWidth(bloc_Width);
		soundBloc.setHeight(bloc_Height);
		soundBloc.setPosition((decalSideX * 2) + bloc_Width, graphicBloc.getHeight() + decalY * 2);
	
		prefBloc.setWidth(bloc_Width);
		prefBloc.setHeight(bloc_Height);
		prefBloc.setPosition(decalSideX, decalY);
		
		languageBloc.setWidth(bloc_Width);
		languageBloc.setHeight(bloc_Height);
		languageBloc.setPosition(decalSideX, decalY);
		
		float buttonHeight = sizebuttonX / Index_Interface.button_Return_Aspect ;
		retour.setSize(sizebuttonX, buttonHeight);
		retour.getImageCell().size(sizebuttonX, buttonHeight);
		returnButtonPositionY = Gdx.graphics.getHeight() / 3.8f ; 
		
		if(retour.getX() < 0) 
			retour.setPosition(-sizebuttonX, returnButtonPositionY);
		else
			retour.setPosition(0, returnButtonPositionY);
			
		this.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}
	
	


	
	public VisTable buildPerfBloc()
	{
		VisTable table = new VisTable() ;
		return table ; 
	}
	
	public VisTable buildLanguageBloc()
	{
		VisTable table = new VisTable() ;
		return table ; 
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
		// Three columns, left to right as they sit on screen: the return sign, the
		// graphics settings, the sound settings.
		ArrayList<ArrayList<Actor>> returningList = new ArrayList<ArrayList<Actor>>(); 
		ArrayList<Actor> buttonList = new ArrayList<>() ;
		buttonList.add(retour) ;
		returningList.add(buttonList) ; 
		returningList.add(graphicBloc.focusOrder()) ;
		returningList.add(soundBloc.focusOrder()) ;
		return returningList;
	}
	
	
	@Override
	public Vector2Int startAt()
	{return new Vector2Int(1,0);}
	
	@Override
	public Button getBackButton()
	{return retour ;}
}