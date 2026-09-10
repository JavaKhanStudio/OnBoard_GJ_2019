package jks.index;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

import jks.tools.Enum_Timming;
import jks.tools.GlobalTimmer;
import jks.vinterface.GVars_UI;
import jks.vinterface.font.GVars_Font;

import static jks.vinterface.font.GVars_Font.* ; 

public class Index_Interface 
{

	public static final String frame = "ui/frame/";
	public static final String icon = "ui/icon/";
	public static final String menu = icon + "menu/";
	public static final String pausePath = icon + "pause/";
	public static final String preload = "ui/preload/" ;
	private static final String intro = "ui/story/intro/" ;
	private static final String outro = "ui/story/outro/" ;
	public static final String misc = "misc/" ;
	
	// StartScreen 
	public static String maisMenus_Background = menu + "UI_1.png" ;
	
	public static String mainMenus_New = menu + "UI_NewGame.png";
	public static String mainMenus_NewON = menu + "UI_NewGame2.png";
	
	public static String mainMenus_Settings = menu + "UI_Settings.png";
	public static String mainMenus_SettingsON= menu + "UI_Settings2.png";
	
	public static String mainMenus_quit = menu + "UI_quit.png";
	public static String mainMenus_quitON = menu + "UI_quit2.png";
	
		
	public static String empty = icon + "grayEmpty.png" ; 
	public static String frame_Gray = frame + "grayFrame.png" ; 
	
	// The overlays were all using the Settings button art to mean "go back". This is the
	// return arrow the project already had and never loaded.
	public static String button_Return = pausePath + "boutonRetour.png" ; 
	public static String frame_GraySmoke = frame + "borderSmokeGray.png" ; 
	
	public static String introLogo_Jam = preload + "logo_jam.png" ; 
	public static String introLogo_LibGDX = preload + "logo_libGdx.png" ; 
	public static String introLogo_Team = preload + "logo_team.png" ; 
	
	public static String introPage_1 = intro + "introPage1.png" ; 
	public static String introPage_2 = intro + "introPage2.png" ; 
	public static String introPage_3 = intro + "introPage3.png" ; 
	public static String smokeImage = intro + "smoke.jpg" ; 
	
	public static String outroPage1 = outro + "outro1.png" ;
	public static String outroPage_stay_1 = outro + "outroStay1.png" ;
	public static String outroPage_stay_2 = outro + "outroStay2.png" ;
	public static String outroPage_leave = outro + "outroLeave.png" ;
	
	private static final String path = "game/wagon/" ; 
	
	public static final String keyPath = "ui/key/key" ; 
	public static final String keyPathFull = "ui/key/keyFull" ; 
	public static final String key1 = keyPath + 1 + ".png" ; 
	public static final String key1Full = keyPathFull + 1 + ".png" ; 
	public static final String key2 = keyPath + 2 + ".png" ; 
	public static final String key2Full = keyPathFull + 2 + ".png" ; 
	public static final String key3 = keyPath + 3 + ".png" ;
	public static final String key3Full = keyPathFull + 3 + ".png" ; 
	
	public static final String bubbleThink = "tools/dialog/bubble_think_2.png" ; 
	
//	public static String wagon_1 =  path + "WAGON_1.png" ; 
//	public static String wagon_2 = intro + "introPage2.png" ; 
//	public static String wagon_3 = intro + "introPage3.png" ; 
//	public static String wagon_4 = intro + "smoke.jpg" ; 
	
	public static AssetManager manager ; 
	
	public static void checkManager()
	{
		if(manager == null)
			manager = new AssetManager();
	}
	
	public static void preInit()
	{
		checkManager() ;
		loadPreloadLogos() ;
	}

	public static void initIntro()
	{
		checkManager() ; 
		loadIntro() ;	
		loadOutro() ;
	}

	public static void init()
	{
		checkManager() ; 
		
		initFont() ; 
		loadEnter() ;
		loadBasic() ; 
		loadKey() ; 
		loadIntro() ;
		loadOutro() ; 
		loadPreloadLogos() ; 
		
		manager.finishLoading();
	}
	
	private static void loadKey()
	{
		manager.load(key1Full, Texture.class);
		manager.load(key1, Texture.class);
		manager.load(key2, Texture.class);
		manager.load(key2Full, Texture.class);
		manager.load(key3, Texture.class);
		manager.load(key3Full, Texture.class);
		
		manager.load(bubbleThink, Texture.class);
	}
	
	private static void loadIntro() 
	{
		manager.load(introPage_1, Texture.class);
		manager.load(introPage_2, Texture.class);
		manager.load(introPage_3, Texture.class);
		manager.load(smokeImage, Texture.class);
		manager.finishLoading();
	}
	
	public static void loadOutro() 
	{
		manager.load(outroPage1, Texture.class);
		manager.load(outroPage_stay_1, Texture.class);
		manager.load(outroPage_stay_2, Texture.class);
		manager.load(outroPage_leave, Texture.class);
		manager.finishLoading();
	}
	
	private static void loadPreloadLogos() 
	{
		manager.load(introLogo_Jam, Texture.class);
		manager.load(introLogo_LibGDX, Texture.class);
		manager.load(introLogo_Team, Texture.class);
		manager.finishLoading();
	}

	
	private static void loadBasic() 
	{
		manager.load(empty, Texture.class);
		manager.load(frame_Gray, Texture.class);
		manager.load(frame_GraySmoke, Texture.class);
		manager.load(button_Return, Texture.class);
	}

	public static void loadEnter()
	{
		manager.load(maisMenus_Background, Texture.class);
		manager.load(mainMenus_New, Texture.class);
		manager.load(mainMenus_NewON, Texture.class);
		manager.load(mainMenus_Settings, Texture.class);
		manager.load(mainMenus_SettingsON, Texture.class);
		manager.load(mainMenus_quit, Texture.class);
		manager.load(mainMenus_quitON, Texture.class);
		manager.finishLoading();
	}
	
	public static void loadGame()
	{
//		manager.load(wagon_1, Texture.class);
		manager.finishLoading();
	}

}