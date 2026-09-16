package jks.vue.models.game;

import jks.tools.Utils_Debug;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jks.index.Index_Interface;

public class GameItem 
{

	public String path_EtatDebut ; 
	public String path_EtatApres_1 ; 
	public String path_EtatApres_2 ; 
	
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
	
	@JsonIgnore
	public Texture objectTexture ; 
	@JsonIgnore
	public Rectangle bottomSelectBound ; 
	@JsonIgnore
	Rectangle textureBounds ; 
	@JsonIgnore
	boolean picked ; 
	/** Under the mouse: a click now would reach it, so it is drawn outlined (r40, d10). */
	@JsonIgnore
	boolean hovered ; 
	
	public GameItem() 
	{}
	
	public GameItem(String path, String name) 
	{
		path_EtatDebut = name ; 
		this.path = path ; 
		this.name = name ; 
	}
	
	String relativePath ;
	
	public void init()
	{
		relativePath =  "game/wagon/" + path + "/" ; 
		Index_Interface.loadTexture(relativePath + path_EtatDebut);
		
		if(path_EtatApres_1 != null && !"".equals(path_EtatApres_1))
			Index_Interface.loadTexture(relativePath + path_EtatApres_1);
		if(path_EtatApres_2 != null && !"".equals(path_EtatApres_2))
			Index_Interface.loadTexture(relativePath + path_EtatApres_2);
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
		
		if(objectTexture != null)
			setGameReady() ; 
	}
	
	public void draw(Batch batch)
	{	
		if(objectTexture == null || picked)
			return ;
		
		batch.draw(objectTexture, posX, posY, objectTexture.getWidth(), objectTexture.getHeight());
		
		// A line around it, not over it (d10): see ItemOutline.
		if(hovered || ItemOutline.lightAll)
			ItemOutline.draw(batch, objectTexture, posX, posY);
	}
	
	/** Takes the pointer in world coordinates, or null when nothing in the carriage can be clicked. */
	public void updateHover(Vector3 pointer)
	{
		// The same test tryTouch makes, so what lights up is exactly what a click reaches.
		hovered = pointer != null && !picked && textureBounds != null
				&& textureBounds.contains(pointer.x, pointer.y) ;
	}
	
	@JsonIgnore
	public boolean isHovered()
	{
		return hovered ; 
	}
	
	/** Taken, so it is not drawn and not clickable. Put back by {@link #resetToStart()} (r60). */
	@JsonIgnore
	public boolean isPicked()
	{
		return picked ; 
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
					GVars_Game.pickItem(this);
				}
					
			}	
			else if(touchingWith == null)
			{
				return ; 
			}
			else if(name_Interaction_1 != null && name_Interaction_1.equals(touchingWith.name))
			{
				if(inTest)
					Utils_Debug.log("misa interaction 1");
				else
				{
					objectTexture = Index_Interface.manager.get(relativePath + path_EtatApres_1, Texture.class);
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
					GVars_Game.applyItem(this, touchingWith.name,2) ;  
				}
			}
		}
	}
	
}
