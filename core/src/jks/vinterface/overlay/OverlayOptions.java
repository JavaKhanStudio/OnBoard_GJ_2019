package jks.vinterface.overlay;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.kotcrab.vis.ui.widget.VisTable;

import jks.index.Index_Interface;
import jks.tools.Vector2Int;
import jks.vinterface.GVars_UI;
import jks.vinterface.Block_Resolution;
import jks.vinterface.Utils_TexturesAcess;
import jks.vue.Utils_View;

public class OverlayOptions extends OverlayModel
{

	ImageButton retour ;
	float returnButtonPositionY = 0 ;
	
	VisTable mainTable ; 
	
	Block_Resolution graphicBloc ; 
	VisTable soundBloc ; 
	VisTable prefBloc ; 
	VisTable languageBloc ; 
	
	String frames = Index_Interface.frame_Gray ;
	
	ReplayAction backway ;
	
	public OverlayOptions(ReplayAction ref) 
	{
		super(GVars_UI.baseSkin) ;
		backway = ref ; 
		this.setLayoutEnabled(false);
			
		retour = new ImageButton(Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.mainMenus_Settings),
				Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.mainMenus_SettingsON));
		retour.addListener(new InputListener()
		{
			@Override
	        public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) 
	        {
				Utils_View.removeCurrentOverlay() ;
				Utils_View.removeFilter() ;
				backway.enterScene(0);
				return true;
	        }
		}) ;
			
		mainTable = new VisTable() ;
		mainTable.setTouchable(Touchable.childrenOnly);
		mainTable.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(frames));
		
		graphicBloc = new Block_Resolution(); 
		graphicBloc.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(frames));
		
		soundBloc = buildSoundsBloc() ; 
		soundBloc.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(frames));
		
		prefBloc = buildSoundsBloc() ; 
		prefBloc.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(frames));
		
		languageBloc = buildSoundsBloc() ; 
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
		
		retour.setSize(sizebuttonX, Gdx.graphics.getHeight() / 9.0f);
		returnButtonPositionY = Gdx.graphics.getHeight() / 3.8f ; 
		
		if(retour.getX() < 0) 
			retour.setPosition(-sizebuttonX, returnButtonPositionY);
		else
			retour.setPosition(0, returnButtonPositionY);
			
		this.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}
	
	

	public VisTable buildSoundsBloc()
	{
		VisTable table = new VisTable() ;
		return table ; 
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
	{return new Vector2Int(0,0);}
}