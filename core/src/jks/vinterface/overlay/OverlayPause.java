package jks.vinterface.overlay;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import jks.amain.Utils_Config;
import jks.index.Index_Interface;
import jks.sounds.GVars_Audio;
import jks.sounds.GVars_AudioManager;
import jks.tools.Vector2Int;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vinterface.Utils_TexturesAcess;

/**
 * The pause screen: the painted PAUSE panel, a "Couper le son" box and the return sign.
 *
 * The art for all of it sat in ui/icon/pause/ since 2019 while this showed a lone button
 * in the Settings art. Escape opens and closes it during a level.
 */
public class OverlayPause extends OverlayModel
{

	ImageButton retour ;
	private final Image panel ;
	private final Image muteLabel ;
	private final ImageButton muteBox ;

	/** pauseMenu.png is 1095x1009; the box and its label were painted at the panel's scale. */
	private static final float PANEL_WIDTH = 1095f, PANEL_HEIGHT = 1009f ;
	
	public OverlayPause() 
	{
		super(GVars_UI.baseSkin) ;

		panel = new Image(Utils_TexturesAcess.getTexture(Index_Interface.pause_Panel)) ;
		panel.setTouchable(Touchable.disabled) ;
		
		muteBox = new ImageButton(Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.pause_Box_Empty),
				Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.pause_Box_Empty),
				Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.pause_Box_Ticked)) ;
		muteBox.setProgrammaticChangeEvents(false) ;
		muteBox.setChecked(GVars_Audio.muted) ;
		muteBox.setProgrammaticChangeEvents(true) ;
		muteBox.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				setMuted(muteBox.isChecked()) ;
			}
		}) ;

		// The words are part of the control, as they would be on any checkbox.
		muteLabel = new Image(Utils_TexturesAcess.getTexture(Index_Interface.pause_Label_Mute)) ;
		muteLabel.addListener(new ClickListener()
		{
			@Override
			public void clicked(InputEvent event, float x, float y)
			{
				muteBox.toggle() ;
			}
		}) ;

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
		
		this.setLayoutEnabled(false);
		this.setFillParent(true);
		
		this.addActor(panel);
		this.addActor(muteLabel);
		this.addActor(muteBox);
		this.addActor(retour);
		resize() ;
	}

	/**
	 * Same rules as the Sound block in the options: muted is saved as a volume of 0. Unmuting
	 * brings back the volume the game was at, or full if muting had already zeroed it.
	 */
	private static void setMuted(boolean muted)
	{
		if(muted)
		{
			GVars_AudioManager.setMuted(true) ;
			Utils_Config.current.volume = 0f ;
		}
		else
		{
			float volume = GVars_Audio.masterVolume > 0f ? GVars_Audio.masterVolume : 1f ;
			GVars_AudioManager.setMasterVolume(volume) ;
			GVars_AudioManager.setMuted(false) ;
			Utils_Config.current.volume = volume ;
		}
		Utils_Config.save() ;
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
		buttonList.add(muteBox) ;
		buttonList.add(retour) ;
		returningList.add(buttonList) ; 
		return returningList;
	}
	
	/** On the return sign, so Enter straight after Escape resumes rather than muting. */
	@Override
	public Vector2Int startAt()
	{return new Vector2Int(0,1) ;}

	@Override
	public Button getBackButton()
	{return retour ;}

	@Override
	public void resize() 
	{
		float screenWidth = Gdx.graphics.getWidth(), screenHeight = Gdx.graphics.getHeight() ;
		this.setBounds(0, 0, screenWidth, screenHeight);
	
		float height = screenHeight * 0.8f ;
		float width = height * PANEL_WIDTH / PANEL_HEIGHT ;
		float x = (screenWidth - width) / 2f, y = (screenHeight - height) / 2f ;
		float scale = width / PANEL_WIDTH ;
		panel.setBounds(x, y, width, height) ;

		// Positions in the panel's own pixels, measured off pauseMenu.png: the plank with
		// PAUSE on it is the top 220, the framed board below runs from about 120 to 970.
		// The mute row is drawn a third larger than painted, which was too small to read.
		float rowScale = scale * 1.3f ;
		float labelWidth = 477 * rowScale, labelHeight = 46 * rowScale ;
		float boxWidth = 74 * rowScale, boxHeight = 62 * rowScale ;
		float rowWidth = labelWidth + boxWidth * 1.4f ;
		float rowX = x + (width - rowWidth) / 2f ;
		float rowY = y + height - 470 * scale ;
		muteLabel.setBounds(rowX, rowY + (boxHeight - labelHeight) / 2f, labelWidth, labelHeight) ;
		muteBox.setBounds(rowX + rowWidth - boxWidth, rowY, boxWidth, boxHeight) ;
		muteBox.getImageCell().size(boxWidth, boxHeight) ;

		float buttonWidth = 440 * scale ;
		float buttonHeight = buttonWidth / Index_Interface.button_Return_Aspect ;
		retour.setBounds(x + (width - buttonWidth) / 2f, y + height - 780 * scale, buttonWidth, buttonHeight) ;
		retour.getImageCell().size(buttonWidth, buttonHeight) ;
	}
}
