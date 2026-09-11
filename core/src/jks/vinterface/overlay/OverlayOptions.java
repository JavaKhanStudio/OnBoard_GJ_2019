package jks.vinterface.overlay;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import jks.index.Index_Interface;
import jks.tools.Vector2Int;
import jks.vinterface.GVars_UI;
import jks.vinterface.Block_Resolution;
import jks.vinterface.Block_Sound;
import jks.vinterface.Utils_Board;
import jks.vinterface.Utils_TexturesAcess;
import jks.vue.Utils_View;

public class OverlayOptions extends OverlayModel
{

	ImageButton retour ;
	float returnButtonPositionY = 0 ;
	
	// Two sign boards, the pause screen's, side by side. They were two gray boxes in a gray
	// frame, with an empty third one where preferences were meant to go and never were.
	Block_Resolution graphicBloc ; 
	Block_Sound soundBloc ;
	private static final float SOUND_BOARD_SCALE = 0.72f ;

	ReplayAction backway ;
	
	public OverlayOptions(ReplayAction ref) 
	{
		super(GVars_UI.baseSkin) ;
		backway = ref ;
		this.setLayoutEnabled(false);
		// Darker than the other overlays: the start screen's logo showed between the boards.
		backgroundColor.a = 0.7f ;
			
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
		
		graphicBloc = new Block_Resolution(); 
		graphicBloc.setTouchable(Touchable.childrenOnly);
		soundBloc = new Block_Sound() ; 
		soundBloc.setTouchable(Touchable.childrenOnly);
		
		retour.setPosition(-5, 0);
		resize() ; 
		
		MoveToAction getIn = new MoveToAction();
	    getIn.setPosition(0, returnButtonPositionY);
	    getIn.setDuration(0.3f);
	    retour.addAction(getIn); 
	    
		this.addActor(graphicBloc);
		this.addActor(soundBloc);
		this.addActor(retour);
	}
	
	public void resize() 
	{
		float screenWidth = Gdx.graphics.getWidth(), screenHeight = Gdx.graphics.getHeight() ;
		float sizebuttonX = screenWidth / 7.5f ;
		float margin = screenWidth / 40f ;
		
		// The boards share what the Retour sign leaves, at the painted board's own shape.
		float left = sizebuttonX + margin ;
		float room = screenWidth - left - margin ;
		float boardWidth = (room - margin) / 2f ;
		float boardHeight = boardWidth * Utils_Board.BOARD_HEIGHT / Utils_Board.BOARD_WIDTH ;
		float tallest = screenHeight * 0.86f ;
		if(boardHeight > tallest)
		{
			boardHeight = tallest ;
			boardWidth = boardHeight * Utils_Board.BOARD_WIDTH / Utils_Board.BOARD_HEIGHT ;
		}
		// Sound has two rows, not six: a smaller board, hung level with the first.
		float soundWidth = boardWidth * SOUND_BOARD_SCALE, soundHeight = boardHeight * SOUND_BOARD_SCALE ;
		float x = left + (room - (boardWidth + margin + soundWidth)) / 2f ;
		float y = (screenHeight - boardHeight) / 2f ;

		graphicBloc.resize(boardWidth, boardHeight);
		graphicBloc.setPosition(x, y);

		soundBloc.resize(soundWidth, soundHeight, graphicBloc.rowHeight());
		soundBloc.setPosition(x + boardWidth + margin, y + boardHeight - soundHeight);
		
		float buttonHeight = sizebuttonX / Index_Interface.button_Return_Aspect ;
		retour.setSize(sizebuttonX, buttonHeight);
		retour.getImageCell().size(sizebuttonX, buttonHeight);
		returnButtonPositionY = screenHeight / 3.8f ; 
		
		if(retour.getX() < 0) 
			retour.setPosition(-sizebuttonX, returnButtonPositionY);
		else
			retour.setPosition(0, returnButtonPositionY);
			
		this.setBounds(0, 0, screenWidth, screenHeight);
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