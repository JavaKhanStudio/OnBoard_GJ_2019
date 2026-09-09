package jks.dialog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;

public class GVars_TestDialog 
{

	public static LabelStyle bubbleText ; 
	
	public static FreeTypeFontGenerator generator_Bubble ;
	
	public static FreeTypeFontParameter parameter ; 
	
	private static float fontMainTitleSizeDivide = 30 ; 
	
	private static BitmapFont font ;
	
	public static void init()
	{
		bubbleText = new LabelStyle() ; 
		bubbleText.fontColor = Color.BLACK ;
		
		generator_Bubble = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/OptimusPrinceps.ttf"));
		resize() ; 
	}
	
	public static void resize()
	{
		parameter = new FreeTypeFontParameter();
		parameter.size = (int) (Gdx.graphics.getWidth()/fontMainTitleSizeDivide) ;
		font = generator_Bubble.generateFont(parameter) ; 
		bubbleText.font = font ; 
	}
	
}
