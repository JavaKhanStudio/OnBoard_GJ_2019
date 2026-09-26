package jks.vue.models.game;

import jks.tools.Utils_Debug;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

import jks.index.Index_Interface;

public class GameItem 
{

	public String path_EtatDebut ; 
	public String path_EtatApres_1 ; 
	public String path_EtatApres_2 ; 
	/**
	 * What the inventory bar shows once it is taken, when that is not the item as it stands in
	 * the carriage (r86): the bare knife, not the knife on its mount. Null: path_EtatDebut.
	 */
	public String path_Inventaire ; 
	
	public String name ; 
	public String description ;
	public String path ; 
	
	public String name_Interaction_1 ; 
	public String message_Crucial_1 ; 
	public String name_Interaction_2 ; 
	public String message_Crucial_2 ;
	
	public int state = 0 ; 
	public float posX, posY; 
	
	public boolean giveKey_take ; 
	public boolean giveKey_interfact ; 
	public boolean pickable = true; 
	public int keyNumb ; 
	
	// Transient: not in the .wa. libGDX Json reads and writes every field that is not, whatever
	// its visibility (r136); runtime state such as picked or hovered (r40) stays out of the files.
	public transient Texture objectTexture ; 
	public transient Rectangle bottomSelectBound ; 
	transient Rectangle textureBounds ; 
	transient boolean picked ; 
	/** Under the mouse: a click now would reach it, so it is drawn outlined (r40, d10). */
	transient boolean hovered ; 
	/** Which line it is outlined with: what it gives (r71). Set by WagonLevel.init. */
	transient ItemOutline.Kind outline = ItemOutline.Kind.NEUTRAL ; 
	/** Something has been used on it: its click line no longer describes it (r74). */
	transient boolean used ; 
	
	public GameItem() 
	{}
	
	public GameItem(String path, String name) 
	{
		path_EtatDebut = name ; 
		this.path = path ; 
		this.name = name ; 
	}
	
	transient String relativePath ;
	
	public void init()
	{
		relativePath =  "game/wagon/" + path + "/" ; 
		Index_Interface.loadTexture(relativePath + path_EtatDebut);
		
		if(path_EtatApres_1 != null && !"".equals(path_EtatApres_1))
			Index_Interface.loadTexture(relativePath + path_EtatApres_1);
		if(path_EtatApres_2 != null && !"".equals(path_EtatApres_2))
			Index_Interface.loadTexture(relativePath + path_EtatApres_2);
		if(path_Inventaire != null && !"".equals(path_Inventaire))
			Index_Interface.loadTexture(relativePath + path_Inventaire);
	}
	
	/** The picture the inventory bar carries: path_Inventaire, or the item as it was found (r86). */
	public Texture inventoryTexture()
	{
		if(path_Inventaire != null && !"".equals(path_Inventaire))
			return Index_Interface.manager.get(relativePath + path_Inventaire, Texture.class) ; 
		return Index_Interface.manager.get(relativePath + path_EtatDebut, Texture.class) ; 
	}
	
	/**
	 * A pickable item with an after-texture leaves it behind when taken (r86): the knife goes,
	 * its empty mount stays on the wall. It is only drawn - not hovered, not clickable again.
	 * No pickable item had an after-texture before, so none that vanished whole now changes.
	 */
	public boolean leavesSomethingBehind()
	{
		return pickable && path_EtatApres_1 != null && !"".equals(path_EtatApres_1) ; 
	}
	
	/** Taken into the inventory: gone from the carriage, or left as its after-texture (r86). */
	void take()
	{
		picked = true ; 
		hovered = false ; 
		if(leavesSomethingBehind() && objectTexture != null)
			objectTexture = Index_Interface.manager.get(relativePath + path_EtatApres_1, Texture.class) ; 
	}
	
	public void setGameReady()
	{
		objectTexture = Index_Interface.manager.get(relativePath + path_EtatDebut, Texture.class) ;
		textureBounds = new Rectangle(posX,posY,objectTexture.getWidth(), objectTexture.getHeight());
	}
	
	public void setPosition(float x, float y)
	{
		textureBounds.x = x ; textureBounds.y = y ; 
		posX = x ; posY = y ; 
	}
	
	public void destroy()
	{
		
	}
	
	/**
	 * Back to the state a fresh run finds it in (r60, notice n13): not picked, and wearing its
	 * first texture again rather than whatever an interaction left it in.
	 *
	 * GVars_Game.preloadedlevel caches one WagonLevel per carriage and hands the same instance
	 * back on a second playthrough, so without this the carriage opens with its items already
	 * taken. Dropping the cache instead would leak each level's Parallax_Heart.
	 *
	 * Does nothing to an item that was never made game ready: its texture is not loaded, and
	 * setGameReady would throw asking the manager for it.
	 */
	public void resetToStart()
	{
		picked = false ; 
		hovered = false ; 
		used = false ; 
		
		if(objectTexture != null)
			setGameReady() ; 
	}
	
	public void draw(Batch batch)
	{	
		if(objectTexture == null || (picked && !leavesSomethingBehind()))
			return ;
		
		batch.draw(objectTexture, posX, posY, objectTexture.getWidth(), objectTexture.getHeight());
		
		// A line around it, not over it (d10): see ItemOutline. What a taken item left is not
		// something to click, so it is never outlined.
		if(!picked && (hovered || ItemOutline.lightAll))
			ItemOutline.draw(batch, objectTexture, posX, posY, outline);
	}
	
	/** Takes the pointer in world coordinates, or null when nothing in the carriage can be clicked. */
	public void updateHover(Vector3 pointer)
	{
		// The same test tryTouch makes, so what lights up is exactly what a click reaches.
		hovered = pointer != null && !picked && textureBounds != null
				&& textureBounds.contains(pointer.x, pointer.y) ;
	}
	
	public ItemOutline.Kind getOutline()
	{
		return outline ; 
	}
	
	public boolean isHovered()
	{
		return hovered ; 
	}
	
	/**
	 * Taken, so it is not clickable, and not drawn unless it leaves something behind (r86). Put
	 * back by {@link #resetToStart()} (r60).
	 */
	public boolean isPicked()
	{
		return picked ; 
	}
	
	/**
	 * The text-table key of what Ross says when the item is clicked (r74): "wa2.clope.click"
	 * for clope.png in carriage 2. Named by the item, not stored in the .wa, so every item has
	 * one without the editor having to learn a field. Vue_LineLab lists and edits them.
	 */
	public static String clickKey(int carriage, String itemName)
	{
		String stem = itemName.endsWith(".png") ? itemName.substring(0, itemName.length() - 4) : itemName ; 
		return "wa" + carriage + "." + stem + ".click" ; 
	}
	
	public void tryTouch(Vector3 touchPos, GameItem touchingWith, boolean inTest)
	{
		if(picked)
			return ;
		
		
		if(textureBounds.contains(touchPos.x, touchPos.y))
		{
			
			if(pickable)
			{
				if(!inTest)
				{
					// Said first, so a key piece this gives thinks of the next step after it (r73).
					GVars_Game.sayClickLine(this) ; 
					GVars_Game.pickItem(this);
				}
					
			}	
			else if(touchingWith == null)
			{
				if(!inTest && !used)
					GVars_Game.sayClickLine(this) ; 
				return ; 
			}
			else if(name_Interaction_1 != null && name_Interaction_1.equals(touchingWith.name))
			{
				if(inTest)
					Utils_Debug.log("misa interaction 1");
				else
				{
					objectTexture = Index_Interface.manager.get(relativePath + path_EtatApres_1, Texture.class);
					used = true ; 
					// Said first, so a key piece this gives thinks of the next step after it (r73).
					GVars_Game.sayItemMessage(message_Crucial_1) ; 
					GVars_Game.applyItem(this, touchingWith.name,1) ; 
				}
			}
			else if (name_Interaction_2 != null && name_Interaction_2.equals(touchingWith.name))
			{
				if(inTest)
					Utils_Debug.log("misa interaction 2");	
				else
				{
					objectTexture = Index_Interface.manager.get(relativePath + path_EtatApres_2, Texture.class);
					used = true ; 
					GVars_Game.sayItemMessage(message_Crucial_2) ; 
					GVars_Game.applyItem(this, touchingWith.name,2) ;  
				}
			}
			else if(!inTest && !used)
			{
				// Held something it does nothing with: he still says what it is.
				GVars_Game.sayClickLine(this) ; 
			}
		}
	}
	
}
