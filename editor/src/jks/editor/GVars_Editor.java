package jks.editor;
import java.util.HashMap;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

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
		GVars_Serialization.objectMapper = prepareJson() ; 
		ref = appli ; 
	}
	
	public static void deleteSelected() 
	{
		
	}
	
	private static ObjectMapper prepareJson() 
	{
		ObjectMapper objectMapper = new ObjectMapper() ; 
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) ; 
//		objectMapper.addMixInAnnotations(TextureRegion.class, MyMixInForIgnoreType.class);
		return objectMapper ; 
	}

}
