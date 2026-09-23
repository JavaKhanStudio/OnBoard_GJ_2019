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
import jks.index.Index_Text;
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
import jks.vue.GVars_Fade;
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
	/** Above this, Ross leaves the train at the end (Vue_Scenematic_Outro); at it or below, he stays. */
	public static final int KARMA_TO_LEAVE = 2 ;
	
	public static DialogBubble dialogBubble ;
	/** The row of picked-up items along the bottom bar. Emptied when the carriage changes (r59). */
	public static InventoryBar inventory ;
	public static String currentLeadingText ; 
	
	public static boolean leavingEnding()
	{
		return karma > KARMA_TO_LEAVE ; 
	}
	
	public static void init()
	{
		karma = 0 ; 
		
		dialogBubble = new DialogBubble(DialogSize.BUBBLE_LARGE_TEXT_MEDIUM) ;
		GVars_UI.mainUi.addActor(dialogBubble);
		
		playerInventory = new ArrayList<GameItem>() ; 
		inventory = new InventoryBar() ; 
		GVars_UI.mainUi.addActor(inventory);
		Index_Interface.loadGame() ; 
		if(rossMap == null)
			rossMap = new HashMap<String, SpriteModel>() ; 
	}
	
	public static void applyText(String text) 
	{
		
	}

	/**
	 * What Ross says when something is used on an item (r81): message_Crucial_1 or _2 of the
	 * .wa, a key of i18n/textes.tsv. Most items have none, and then nothing is said.
	 */
	public static void sayItemMessage(String key)
	{
		if(key == null || key.isEmpty() || dialogBubble == null)
			return ; 
		dialogBubble.showForReading(Index_Text.get(key)) ; 
	}
	
	/**
	 * What Ross says when an item is clicked (r74): the row GameItem.clickKey names. An item
	 * the table has no row for says nothing - TextTableTest makes sure none is missing.
	 */
	public static void sayClickLine(GameItem item)
	{
		String key = GameItem.clickKey(currentLevelInt, item.name) ; 
		if(Index_Text.has(key))
			sayItemMessage(key) ; 
	}
	
	/**
	 * What Ross has on his mind at each step of a carriage (r73): as it opens, and each time a
	 * piece of the key comes, he thinks the hint of the first piece he still lacks - his state
	 * of mind and, between the lines, what he needs to find. The same line the piece shows
	 * under the mouse. Nothing once the key is whole: the carriage is on its way out.
	 */
	public static void thinkOfNextStep(float afterSeconds)
	{
		if(!thinkAtEachStep || dialogBubble == null || clef == null || currentLevel == null)
			return ; 
		
		String key = currentLevel.hintKey(clef.firstMissing()) ; 
		if(key == null)
			return ; 
		
		String text = Index_Text.get(key) ; 
		// After whatever is being said - an item's line (r81) - rather than over it.
		afterSeconds = Math.max(afterSeconds, dialogBubble.secondsBusy()) ; 
		if(afterSeconds > 0)
			dialogBubble.showForReading(text, afterSeconds) ; 
		else
			dialogBubble.showForReading(text) ; 
	}
	
	public static void pickItem(GameItem gameItem)
	{
		TextureRegionDrawable drawable = Utils_TexturesAcess.buildDrawingRegionTexture(gameItem.objectTexture) ; 
		VisImageButton selectable = new VisImageButton(drawable) ; 
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
		
		// So the bar can find it again when the item is used up (r77).
		selectable.setUserObject(gameItem) ; 
		playerInventory.add(gameItem) ; 
		gameItem.picked = true ; 
		
		// The bar sizes and centres it, and every item already in the row moves over (r59).
		inventory.carry(selectable);
		
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
		
		// A carriage is entered empty-handed: its own items are the only ones its
		// interactions name, so anything still carried would only read as unfinished (r59).
		emptyInventory() ; 
		// Whatever the last carriage was saying belongs to it (r81).
		if(dialogBubble != null)
			dialogBubble.makeDisappear() ; 
		
		clef = new Clef(level) ; 
		GVars_UI.mainUi.addActor(clef);
    	currentLevel = level ; 
    	
    	Enum_AGE age = Enum_AGE.getFromName(level.rossAge) ; 
    	
    	ross = new SpriteModel(SIW_Data.getRoss(age)) ; 
		ross.reverse(true);
		ross.position.y = age.positionY ; 
		ross.position.x = 190 ; 
    	
    	Index_Interface.manager.finishLoading() ; 
		currentLevel.setAsGameReady(); 
		
		// Once the carriage has faded in (GVars_Fade.IN_SECONDS), so it is read, not missed.
		thinkOfNextStep(CARRIAGE_THOUGHT_DELAY) ; 
    }
	
	/**
	 * Off only for the render tests that measure the bubble or the carriage against a still
	 * frame (KeyHintRenderTest, ItemHoverRenderTest): a thought typing at 1.5 s is not still.
	 */
	public static boolean thinkAtEachStep = true ; 
	
	/** How long a carriage's first thought waits after the carriage is swapped in (r73). */
	public static final float CARRIAGE_THOUGHT_DELAY = 1.5f ; 
    
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
	
	/**
	 * Puts the story back to its first page, so looping round from the ending is a real new run
	 * (r60) rather than a fifth carriage with nothing left in it.
	 *
	 * The carriages themselves are cached in preloadedlevel and keep everything the player did
	 * to them - picked items stay picked, a used item keeps its after-texture (notice n13) - so
	 * each one is put back rather than thrown away, which would leak its Parallax_Heart.
	 */
	public static void resetForNewRun()
	{
		currentLevelInt = 1 ; 
		karma = 0 ; 
		emptyInventory() ; 
		
		for(WagonLevel level : preloadedlevel.values())
			for(GameItem item : level.listItems)
				item.resetToStart() ; 
	}
	
	/**
	 * An item that has been used is spent (r77): out of the hand and off the bar. No item in
	 * the four carriages is used on two things, and one left in hand could be used on the same
	 * thing again - giving its key piece, and its karma, a second time.
	 */
	public static void useUp(GameItem item)
	{
		if(selectedItem == item)
			selectedItem = null ; 
		if(playerInventory != null)
			playerInventory.remove(item) ; 
		if(inventory != null)
			inventory.drop(item) ; 
	}
	
	public static void emptyInventory()
	{
		if(playerInventory != null)
			playerInventory.clear();
		selectedItem = null ; 
		if(inventory != null)
			inventory.empty();
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
		
		useUp(selected) ; 
		
		if(gameItem.giveKey_interfact)
		{
			clef.applySucces(gameItem.keyNumb);
		}
	}

	public static void nextLevel() 
	{
		// The carriage fades out and the next one, or the ending, fades in (r44). The swap
		// waits for the black; a second call while that fade runs changes nothing.
		boolean started = GVars_Fade.through(() ->
		{
			if(currentLevelInt == LEVEL_COUNT)
			{
				GVars_Heart.changeVue(new Vue_Scenematic_Outro(),true) ; 
			}
			
			GVars_Camera.resetCamera();
			loadLevel(++currentLevelInt) ; 
		}) ;
		
		// Before the outro's changeVue, which stops the train but lets this finish over it.
		if(started)
			GVars_AudioManager.PlayEffect(Enum_Effect_Sound.Slot.LEVEL_COMPLETE) ; 
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