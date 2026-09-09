package jks.dialog;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;

import jks.index.Index_Interface;
import jks.vinterface.GVars_UI;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.tools.DialogBubble;
import jks.vinterface.tools.DialogBubble.DialogSize;

public class ThinkingDialogTest extends ApplicationAdapter 
{
	
	// https://github.com/rafaskb/typing-label 
	public static void main (String[] arg) 
	{			
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

		config.setWindowedMode(1600, 900);
//		config.setWindowedMode(3200, 1800);
		config.useOpenGL3(true, 1, 1);
		config.setTitle("Test");
		config.setResizable(false);

		Lwjgl3Application application = new Lwjgl3Application(new ThinkingDialogTest(), config);
	}
	
	
	public AssetManager manager ; 
	
	private static String testText = "{COLOR=black} Ceci est une phrase en continue que j'espère fonctionnera correctement pourtant il n'est pas sur que ce soit le cas" ; 
	private static String testTextADV = "{SPEED=0.75}{COLOR=black}Ceci{RAINBOW} est un test {ENDRAINBOW} \n {WAVE}Et ca fonctionne bienn{ENDWAVE}" ; 
	private static String testTextLong = "{COLOR=black} DEDAZDZDZDZDZADEDAZDZDZDZDZADEDAZDZDZDZDZADEDAZDZDZDZDZADEDAZDZDZDZDZADEDAZDZDZDZDZA" ; 
	
	@Override
	public void create () 
	{
		Index_Interface.checkManager();
		Index_Interface.manager.load(Index_Interface.bubbleThink, Texture.class);
		Index_Interface.manager.finishLoading();
		GVars_Font.initFont() ; 
		
		mainInit() ; 
		DialogBubble bubble1_1 = new DialogBubble(testTextADV,DialogSize.BUBBLE_SMALL_TEXT_LARGE,true) ; 
		bubble1_1.setPosition(00, 0);
		
		DialogBubble bubble1_2 = new DialogBubble(testTextADV,DialogSize.BUBBLE_MEDIUM_TEXT_LARGE,true) ; 
		bubble1_2.setPosition(bubble1_1.getWidth(), 00);
		
		DialogBubble bubble1_3 = new DialogBubble(testTextADV,DialogSize.BUBBLE_LARGE_TEXT_LARGE,true) ; 
		bubble1_3.setPosition(bubble1_2.getX() + bubble1_2.getWidth(), 00);
		
		DialogBubble bubble2_1 = new DialogBubble(testTextLong,DialogSize.BUBBLE_SMALL_TEXT_LARGE,true) ; 
		bubble2_1.setPosition(0, bubble1_3.getHeight());
		
		DialogBubble bubble2_2 = new DialogBubble(testTextLong,DialogSize.BUBBLE_MEDIUM_TEXT_LARGE,true) ; 
		bubble2_2.setPosition(bubble2_1.getWidth(), bubble1_3.getHeight());
		
		DialogBubble bubble2_3 = new DialogBubble(testTextLong,DialogSize.BUBBLE_LARGE_TEXT_LARGE,true) ; 
		bubble2_3.setPosition(bubble2_2.getX() + bubble2_2.getWidth(), bubble1_3.getHeight());
		
		DialogBubble bubble3_1 = new DialogBubble(testText,DialogSize.BUBBLE_SMALL_TEXT_MEDIUM,true) ; 
		bubble3_1.setPosition(0, bubble2_1.getY() + bubble2_1.getHeight());
		
		DialogBubble bubble3_2 = new DialogBubble(testText,DialogSize.BUBBLE_MEDIUM_TEXT_MEDIUM,true) ; 
		bubble3_2.setPosition(bubble3_1.getWidth(), bubble2_2.getY() +  bubble2_3.getHeight());
		
		DialogBubble bubble3_3 = new DialogBubble(testText,DialogSize.BUBBLE_LARGE_TEXT_MEDIUM,true) ; 
		bubble3_3.setPosition(bubble3_2.getX() + bubble3_2.getWidth(), bubble2_3.getY() + bubble2_3.getHeight());
		
		
		System.out.println(bubble1_1.getWidth());
		
		GVars_UI.mainUi.addActor(bubble1_1);
		GVars_UI.mainUi.addActor(bubble1_2);
		GVars_UI.mainUi.addActor(bubble1_3);
		
		GVars_UI.mainUi.addActor(bubble2_1);
		GVars_UI.mainUi.addActor(bubble2_2);
		GVars_UI.mainUi.addActor(bubble2_3);
		
		GVars_UI.mainUi.addActor(bubble3_1);
		GVars_UI.mainUi.addActor(bubble3_2);
		GVars_UI.mainUi.addActor(bubble3_3);
	}
		
	private void mainInit()
	{
	//	GVars_Serialization.init(); 
		GVars_UI.init() ; 
//		GVars_Camera.init();	
//		GVars_Controller.init();
//		Index_Interface.init();
	}

	@Override
	public void render () 
	{
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    	float delta = Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f);
    	GVars_UI.mainUi.act(delta);
    	
//    	Gdx.gl.glEnable(GL20.GL_BLEND);
    	GVars_UI.mainUi.draw() ;
   	}

}