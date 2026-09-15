package jks.vue.models.game;

import jks.tools.Utils_Debug;

import java.util.ArrayList;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.kotcrab.vis.ui.widget.VisImageButton;

import jks.camera.GVars_Camera;
import jks.index.Index_Interface;
import jks.sounds.Enum_Effect_Sound;
import jks.sounds.GVars_AudioManager;
import jks.personnage.model.Enum_AGE;
import jks.personnage.model.SIW_Data;
import jks.personnage.model.SpriteModel;
import jks.vars.GVars_Heart;
import jks.vars.GVars_Serialization;
import jks.vinterface.GVars_UI;
import jks.vinterface.Utils_TexturesAcess;
import jks.vinterface.tools.DialogBubble;
import jks.vinterface.tools.DialogBubble.DialogSize;
import jks.vue.models.Vue_Scenematic_Outro;

public class GVars_Game 
{
	
	public static HashMap<Integer,WagonLevel> preloadedlevel = new HashMap<Integer, WagonLevel>(); 
	public static ArrayList<GameItem> playerInventory ;
	public static GameItem selectedItem ;
	TextBubble textShown ; 
	public static WagonLevel currentLevel ;
	public static Clef clef ; 
	public static final int LEVEL_COUNT = 4 ; 
	public static int currentLevelInt = 1 ; 
	
	public static SpriteModel ross ; 
	public static HashMap<String,SpriteModel> rossMap ; 
	
	public static int karma = 0 ;
	
	public static DialogBubble dialogBubble ;
	public static String currentLeadingText ; 
	
	public static void init()
	{
		karma = 0 ; 
		
		dialogBubble = new DialogBubble(DialogSize.BUBBLE_LARGE_TEXT_MEDIUM) ;
		GVars_UI.mainUi.addActor(dialogBubble);
		
		playerInventory = new ArrayList<GameItem>() ; 
		Index_Interface.loadGame() ; 
		if(rossMap == null)
			rossMap = new HashMap<String, SpriteModel>() ; 
	}
	
	public static void applyText(String text) 
	{
		
	}

	public static void pickItem(GameItem gameItem)
	{
		TextureRegionDrawable drawable = Utils_TexturesAcess.buildDrawingRegionTexture(gameItem.objectTexture) ; 
		VisImageButton selectable = new VisImageButton(drawable) ; 
		selectable.setWidth(currentLevel.decalYBot);
		selectable.setHeight(currentLevel.decalYBot);
		selectable.setX(currentLevel.decalYBot * playerInventory.size());
		selectable.addListener(new InputListener()
		{		
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) 
			{return true ;}
			
			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button)
			{
				selectedItem = gameItem ;
				selectable.setChecked(true);
				selectable.toggle();
//				selectable.setColor(Color.WHITE);
			}
		}) ; 	
		
		playerInventory.add(gameItem) ; 
		gameItem.picked = true ; 
		
		GVars_UI.mainUi.addActor(selectable);
		
		if(gameItem.giveKey_take)
			GVars_Game.addKey(gameItem.keyNumb) ; 
		
	}
	
	public static void loadLevel(int value)
    {	
		if(value > LEVEL_COUNT)
			return ; 
		
		GVars_Heart.vue.restart();
		
		WagonLevel level = preloadedlevel.get(value) ; 
		if(level == null)
		{
			Utils_Debug.log("Level " + value + " was not preloaded, loading it now") ;
			level = preLoadLevel(value) ;
		}
		
		clef = new Clef(level) ; 
		GVars_UI.mainUi.addActor(clef);
    	currentLevel = level ; 
    	
    	Enum_AGE age = Enum_AGE.getFromName(level.rossAge) ; 
    	
    	ross = new SpriteModel(SIW_Data.getRoss(age)) ; 
		ross.reverse(true);
		ross.position.y = age.positionY ; 
		ross.position.x = 200 ; 
    	
    	Index_Interface.manager.finishLoading() ; 
		currentLevel.setAsGameReady(); 
    }
    
    public static WagonLevel preLoadLevel(int value)
    {
    	WagonLevel level = preloadedlevel.get(value) ; 
    	if(preloadedlevel.get(value) != null)
    		return level; 
    	
    	try
    	{
    		// Read as a stream, not handle.file(). file() hands back a path relative to the
    		// process working directory, which only works when the assets sit loose on disk -
    		// it cannot see inside a jar. Everything else already resolves the libGDX way
    		// (no "assets/" prefix, found on the classpath), so this now matches.
    		FileHandle handle = Gdx.files.internal("game/wagon/wa" + value + ".wa") ; 

    		level = GVars_Serialization.objectMapper.readValue(handle.read(), WagonLevel.class) ; 
    		level.init(); 
    		preloadedlevel.put(value, level) ; 
    		return level ; 
    	}
    	catch (Exception e)
    	{
    		// This used to print the stack trace and return null, and the caller then walked
    		// straight into a NullPointerException inside Clef - which says nothing about the
    		// actual problem. If a level will not load the game cannot continue, so say why.
    		throw new GdxRuntimeException(
    			"Could not load level " + value + " from game/wagon/wa" + value + ".wa", e) ;
    	}
    }
	
	public static void checkForSelection()
	{
		
	}

	public static void addKey(int keyNumb) 
	{
		Utils_Debug.log("applying succes " + keyNumb);
		clef.applySucces(keyNumb);
	}

	public static void applyItem(GameItem gameItem, String name, int choiceNumber) 
	{
		if(choiceNumber == 2)
		{
			karma ++ ; 
		}
		
		GameItem selected = null ;
		for(GameItem item : currentLevel.listItems)
		{
			if(name.equals(item.name))
			{
				selected = item ; 
			
				break ; 
			}
		}
		
		if(selected == null)
			return ; 
		
		if(gameItem.giveKey_interfact)
		{
			clef.applySucces(gameItem.keyNumb);
		}
	}

	public static void nextLevel() 
	{
		// Before the outro's changeVue, which stops the train but lets this finish over it.
		GVars_AudioManager.PlayEffect(Enum_Effect_Sound.Slot.LEVEL_COMPLETE) ; 
		
		if(currentLevelInt == 4)
		{
			GVars_Heart.changeVue(new Vue_Scenematic_Outro(),true) ; 
		}
		
		GVars_Camera.init();
		loadLevel(++currentLevelInt) ; 
	}

	public static void tryMakeDiseaper(String text) 
	{
		Utils_Debug.log(dialogBubble.getText());
		if(dialogBubble.getText().equals(text))
		{
			dialogBubble.makeDisappear() ; 
			Utils_Debug.log("am out of the same");
		}
		
	}	
}