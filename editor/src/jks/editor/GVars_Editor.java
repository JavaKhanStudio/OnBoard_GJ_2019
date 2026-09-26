package jks.editor;
import java.util.HashMap;


import jks.vars.GVars_Heart;
import jks.vars.GVars_Serialization;
import jks.vue.models.game.GameItem;
import jks.vue.models.game.WagonLevel;

public class GVars_Editor 
{
	public static ImportAction ref ;

	public static WagonLevel workingOnLevel; 
	
	public static HashMap<String,GameItem> listItems ; 
	
	public static HashMap<String,GameItem> listItemsInLevel ; 
	public static GameItem selectedItem ; 
	
	public static void init(ImportAction appli) 
	{
		listItems = new HashMap<>() ; 
		listItemsInLevel = new HashMap<String, GameItem>() ; 
		workingOnLevel = new WagonLevel() ; 
		// The game's own reader and writer (r136): what the editor saves is what the game reads.
		GVars_Serialization.prepareJson() ; 
		ref = appli ; 
	}
	
	public static void deleteSelected() 
	{
		
	}
	
}
