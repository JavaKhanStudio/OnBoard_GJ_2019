package jks.vinterface.font;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.github.tommyettinger.textra.Font;
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
	static HashMap<Enum_Fonts, Font> activeTextraFont ;
	
	/** Menu fonts replaced by a resolution change, kept alive until shutdown. */
	private static final java.util.List<BitmapFont> retiredFonts = new java.util.ArrayList<BitmapFont>() ;
	private static int lastBuiltWidth = -1 ;
	
	public static void initFont()
	{
		activeFont = new HashMap() ;
		activeLabelStyle = new HashMap() ; 
		activeTextraFont = new HashMap() ; 
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
	
	/**
	 * Builds the label styles the menus draw with.
	 *
	 * This was commented out, which left labelStyle_ScreenTitle, labelStyle_OptionsTitle and
	 * labelStyle_Second null - and scene2d rejects a null style outright. The start screen
	 * and the options screen both died on their first label the moment either was reached,
	 * which is how they stayed broken while the game was launching straight into level 1.
	 *
	 * The two typefaces are the ones the project already carries and never loaded:
	 * OptimusPrinceps for titles, GeosansLight for everything smaller.
	 */
	public static void prebuild()
	{
		generator_Titles = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/OptimusPrinceps.ttf"));
		generator_Seconds = new FreeTypeFontGenerator(Gdx.files.internal("ui/fonts/GeosansLight.ttf"));
		parameter = new FreeTypeFontParameter();
		
		labelStyle_ScreenTitle = new LabelStyle(baseSkin.get("default", LabelStyle.class)) ; 
		labelStyle_OptionsTitle = new LabelStyle(baseSkin.get("default", LabelStyle.class)) ; 
		labelStyle_Second = new LabelStyle(baseSkin.get("default", LabelStyle.class)) ;
		labelStyle_BigStuff = new LabelStyle(baseSkin.get("default", LabelStyle.class)) ;
	}
	
	/**
	 * Regenerates the menu fonts for the current window size. FreeType renders to a bitmap,
	 * so a resolution change needs new glyphs rather than a scaled-up old texture.
	 */
	public static void resize()
	{
		if(generator_Titles == null)
			return ;   // prebuild() has not run yet - nothing to resize
		
		// Nothing to do if the window is the width these glyphs were rendered for. This is
		// the common case by far: Vue_StartScreen calls resize() immediately after building
		// its menu, and regenerating identical fonts there is pure churn.
		if(lastBuiltWidth == Gdx.graphics.getWidth() && font_MainMenu != null)
			return ;
		
		// A new set of textures. The previous ones cannot be disposed here: labels built
		// before this call hold a glyph cache pointing at them, and disposing underneath a
		// live label draws every character as a solid block - correct spacing, dead texture.
		// They are retired instead, and released when the game shuts down. Only an actual
		// resolution change reaches this point, so the list stays short.
		retireMenuFonts() ;
		lastBuiltWidth = Gdx.graphics.getWidth() ;
		
		parameter.size = (int) (Gdx.graphics.getWidth()/fontMainTitleSizeDivide) ;
		font_MainMenu = generator_Titles.generateFont(parameter);
		
		parameter.size = (int) (Gdx.graphics.getWidth()/fontTitleSizeDivide) ;
		font_Title = generator_Titles.generateFont(parameter);
		
		parameter.size = (int) (Gdx.graphics.getWidth()/fontBasicSizeDivide) ;
		font_Second = generator_Seconds.generateFont(parameter);
		fontont_SelectBox = generator_Seconds.generateFont(parameter);
		
		labelStyle_ScreenTitle.font = font_MainMenu ; 
		labelStyle_OptionsTitle.font = font_Title ; 
		labelStyle_Second.font = font_Second ; 
		labelStyle_BigStuff.font = font_MainMenu ; 
	}
	
	private static void retireMenuFonts()
	{
		for(BitmapFont font : new BitmapFont[]{font_MainMenu, font_Title, font_Second, fontont_SelectBox})
			if(font != null)
				retiredFonts.add(font) ;
	}
	
	/** Releases every menu font, including ones a previous resolution left behind. */
	public static void dispose()
	{
		retireMenuFonts() ;
		for(BitmapFont font : retiredFonts)
			font.dispose() ;
		retiredFonts.clear() ;
		
		font_MainMenu = font_Title = font_Second = fontont_SelectBox = null ;
		lastBuiltWidth = -1 ;
		
		if(generator_Titles != null)  { generator_Titles.dispose() ;  generator_Titles = null ; }
		if(generator_Seconds != null) { generator_Seconds.dispose() ; generator_Seconds = null ; }
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
	
	/**
	 * The same font, wrapped for TextraTypist.
	 *
	 * TextraTypist draws through its own Font type rather than a scene2d LabelStyle, so the
	 * FreeType-generated BitmapFont from {@link #buildLabel} gets wrapped once and cached -
	 * building a Font per resize would re-measure every glyph.
	 */
	public static Font buildTextraFont(Enum_Fonts font)
	{
		Font textraFont = activeTextraFont.get(font) ;
		if(textraFont == null)
		{
			textraFont = new Font(buildLabel(font).font) ;
			activeTextraFont.put(font, textraFont) ;
		}
		
		return textraFont ; 
	}
	
}