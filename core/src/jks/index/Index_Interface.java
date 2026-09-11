package jks.index;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.utils.Array;

import jks.amain.Utils_Config;
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
	/** boutonRetour.png is 1268x546; keep its shape when scaling it down to a button. */
	public static final float button_Return_Aspect = 1268f / 546f ;
	
	// The pause screen: the PAUSE panel and its "Couper le son" box.
	public static String pause_Panel = pausePath + "pauseMenu.png" ; 
	public static String pause_Label_Mute = pausePath + "libelleCoupeSon.png" ; 
	public static String pause_Box_Empty = pausePath + "cocheVide.png" ; 
	public static String pause_Box_Ticked = pausePath + "cocheOk.png" ;
	/** The same board with PAUSE taken off its plank, for the options screen's titles. See tools/make_blank_panel.py. */
	public static String board_Blank = pausePath + "panneauVide.png" ;
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

	/**
	 * Every Texture this manager holds goes through here: the UI, and the carriages and items
	 * WagonLevel and GameItem load. A plain manager.load leaves libGDX's default of Nearest
	 * with no mipmaps, which is only right for pixel art - this art is large and drawn small,
	 * and Nearest shredded it (the PIXMEN logo lost its beak at 1280x720). The atlases were
	 * always exported Linear, so smooth was the intent all along.
	 *
	 * Mipmaps are the "Mipmaps" switch in the options: smoother still when art is shrunk a
	 * long way, a touch softer otherwise, and a third more memory. The switch is read when
	 * the texture arrives, not when it is queued: the start screen queues level 1 before
	 * anyone has reached the options.
	 */
	public static void loadTexture(String path)
	{
		manager.load(path, Texture.class, filtering);
	}
	
	private static final TextureParameter filtering = new TextureParameter() ;
	static
	{
		filtering.minFilter = TextureFilter.Linear ;
		filtering.magFilter = TextureFilter.Linear ;
		filtering.loadedCallback = (assets, fileName, type) ->
		{
			if(Utils_Config.current.useMipmaps)
				filter(assets.get(fileName, Texture.class), true) ;
		} ;
	}
	
	/**
	 * Applies the mipmap switch to everything already loaded, so the options screen shows the
	 * difference at once instead of on the next launch. Switching off only changes the filter;
	 * the mip levels already built stay in memory until the game closes.
	 */
	public static void applyMipmaps(boolean useMipmaps)
	{
		if(manager == null)
			return ;
		
		for(Texture texture : manager.getAll(Texture.class, new Array<Texture>()))
			filter(texture, useMipmaps) ;
	}
	
	private static void filter(Texture texture, boolean useMipmaps)
	{
		if(useMipmaps)
		{
			texture.bind() ;
			Gdx.gl.glGenerateMipmap(GL20.GL_TEXTURE_2D) ;
			texture.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear) ;
		}
		else
			texture.setFilter(TextureFilter.Linear, TextureFilter.Linear) ;
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
		loadTexture(key1Full);
		loadTexture(key1);
		loadTexture(key2);
		loadTexture(key2Full);
		loadTexture(key3);
		loadTexture(key3Full);
		
		loadTexture(bubbleThink);
	}
	
	private static void loadIntro() 
	{
		loadTexture(introPage_1);
		loadTexture(introPage_2);
		loadTexture(introPage_3);
		loadTexture(smokeImage);
		manager.finishLoading();
	}
	
	public static void loadOutro() 
	{
		loadTexture(outroPage1);
		loadTexture(outroPage_stay_1);
		loadTexture(outroPage_stay_2);
		loadTexture(outroPage_leave);
		manager.finishLoading();
	}
	
	private static void loadPreloadLogos() 
	{
		loadTexture(introLogo_Jam);
		loadTexture(introLogo_LibGDX);
		loadTexture(introLogo_Team);
		manager.finishLoading();
	}

	
	private static void loadBasic() 
	{
		loadTexture(empty);
		loadTexture(frame_Gray);
		loadTexture(frame_GraySmoke);
		loadTexture(button_Return);
		loadTexture(pause_Panel);
		loadTexture(pause_Label_Mute);
		loadTexture(pause_Box_Empty);
		loadTexture(pause_Box_Ticked);
		loadTexture(board_Blank);
	}

	public static void loadEnter()
	{
		loadTexture(maisMenus_Background);
		loadTexture(mainMenus_New);
		loadTexture(mainMenus_NewON);
		loadTexture(mainMenus_Settings);
		loadTexture(mainMenus_SettingsON);
		loadTexture(mainMenus_quit);
		loadTexture(mainMenus_quitON);
		manager.finishLoading();
	}
	
	public static void loadGame()
	{
//		manager.load(wagon_1, Texture.class);
		manager.finishLoading();
	}

}