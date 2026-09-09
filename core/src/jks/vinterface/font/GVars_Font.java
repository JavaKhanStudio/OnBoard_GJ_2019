package jks.vinterface.font;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import jks.vars.FVars_Heart;
import jks.vinterface.font.Index_Fonts.Enum_Fonts;

import static jks.vinterface.GVars_UI.* ;

import java.util.HashMap; 

public class GVars_Font 
{
	public static BitmapFont font12 ;
	public static BitmapFont font24 ;
	
	public static FreeTypeFontGenerator ftfg ; 
	public static final float baseSize = 0.40f ;
	
	public static FreeTypeFontGenerator generator_Titles ;
	public static FreeTypeFontGenerator generator_Seconds ;
	public static FreeTypeFontParameter parameter ;
	
	public static BitmapFont font_Title ; 
	public static BitmapFont font_MainMenu ; 
	public static BitmapFont font_Second ; 
	public static BitmapFont fontont_SelectBox ; 
	
	public static LabelStyle labelStyle_OptionsTitle ;
	public static LabelStyle labelStyle_Second ; 
	public static LabelStyle labelStyle_ScreenTitle ; 
	public static LabelStyle labelStyle_BigStuff ;

	private static float fontMainTitleSizeDivide = 30 ; 
	private static float fontTitleSizeDivide = 40 ; 
	private static float fontBasicSizeDivide = 55 ; 
	
	static HashMap<Enum_Fonts, BitmapFont> activeFont ;
	static HashMap<String, FreeTypeFontGenerator> fontGenerators ;
	static HashMap<Enum_Fonts, LabelStyle> activeLabelStyle ;
	
	public static void initFont()
	{
		activeFont = new HashMap() ;
		activeLabelStyle = new HashMap() ; 
		fontGenerators = new HashMap() ; 
//		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Calibri.ttf"));
//		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
//		
//		parameter.size = (int)(baseSize * Gdx.graphics.getWidth()/FVars_Heart.screenXModel);
//		font12 = generator.generateFont(parameter); // font size 12 pixels
//		
//		parameter.size = (int)(3 * baseSize * Gdx.graphics.getWidth()/FVars_Heart.screenXModel);
//		font24 = generator.generateFont(parameter); // font size 12 pixels
//		
//		generator.dispose(); // don't forget to dispose to avoid memory leaks!
	}
	
	public static void prebuild()
	{
//		generator_Titles = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/OptimusPrinceps.ttf"));
//		generator_Seconds = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/GeosansLight.ttf"));
//		parameter = new FreeTypeFontParameter();
//		
//		labelStyle_ScreenTitle = new LabelStyle(baseSkin.get("default", LabelStyle.class)) ; 
//		labelStyle_OptionsTitle = new LabelStyle(baseSkin.get("default", LabelStyle.class)) ; 
//		labelStyle_Second = new LabelStyle(baseSkin.get("default", LabelStyle.class)) ;
	}
	
	public static void resize()
	{
//		parameter.size = (int) (Gdx.graphics.getWidth()/fontMainTitleSizeDivide) ;
//		font_MainMenu = generator_Titles.generateFont(parameter);
//		
//		parameter.size = (int) (Gdx.graphics.getWidth()/fontTitleSizeDivide) ;
//		font_Title = generator_Titles.generateFont(parameter);
//		
//		parameter.size = (int) (Gdx.graphics.getWidth()/fontBasicSizeDivide) ;
//		font_Second = generator_Seconds.generateFont(parameter);
//		fontont_SelectBox = generator_Seconds.generateFont(parameter);
//		
////		font_MainMenu.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
//		labelStyle_ScreenTitle.font = font_MainMenu ; 
//		labelStyle_OptionsTitle.font = font_Title ; 
//		labelStyle_Second.font = font_Second ; 
		
//		massResize(mainUi.getActors()) ; 
	}
	
	public static LabelStyle buildLabel(Enum_Fonts font)
	{
		LabelStyle labelStyle = activeLabelStyle.get(font); 
		if(labelStyle == null) 
		{
			BitmapFont bitmapFont = activeFont.get(font) ; 
			if(bitmapFont == null) 
			{
				FreeTypeFontGenerator fontGenerator = fontGenerators.get(font.path) ; 
				if(fontGenerator == null)
				{
					fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal(font.path));
					fontGenerators.put(font.path, fontGenerator) ; 
				}
				parameter = new FreeTypeFontParameter();
				parameter.size = (int) (Gdx.graphics.getWidth()/font.basedSizeDevide) ;
				bitmapFont = fontGenerator.generateFont(parameter) ;
				activeFont.put(font, bitmapFont) ; 
			}
			
			labelStyle = new LabelStyle() ; 
			labelStyle.font = bitmapFont ; 
			activeLabelStyle.put(font, labelStyle) ; 
		}
		
		
		return labelStyle ; 
	}
	
}