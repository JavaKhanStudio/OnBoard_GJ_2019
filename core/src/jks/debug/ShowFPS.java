package jks.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

import jks.vinterface.GVars_UI;
import jks.vinterface.ToRender;

public class ShowFPS implements ToRender 
{
	Label fpsLabel ; 
	float decal ;
	public ShowFPS()
	{
		decal = Gdx.graphics.getWidth()/10 ;
		BitmapFont myFont = new BitmapFont(Gdx.files.internal("skin/font-export.fnt"));
		Label.LabelStyle label1Style = new Label.LabelStyle();
		label1Style.font = myFont ; 
		fpsLabel = new Label("", label1Style);
		fpsLabel.setFontScale(3.5f);
		fpsLabel.setPosition(decal * 1.5f, Gdx.graphics.getHeight() - decal);
		GVars_UI.mainUi.addActor(fpsLabel);
	}
	
	@Override
	public void render()
	{fpsLabel.setText(Integer.toString(Gdx.graphics.getFramesPerSecond()));}
}
