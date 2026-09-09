package jks.personnage.model;

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class SIW_Data 
{
	public HashMap<Enum_AnimState,Animation<TextureRegion>> animationList ;
	public float scale ;
	
	public SIW_Data() 
	{
		animationList = new HashMap<Enum_AnimState,Animation<TextureRegion>>() ;
		
	}
	
	public SIW_Data(TextureAtlas textureAtlas, HashMap<Enum_AnimState,String> animationName, float animationSpeed, float Scale)
	{
		animationList = new HashMap<Enum_AnimState,Animation<TextureRegion>>() ;
		
		for(Enum_AnimState finalName : animationName.keySet())
		{
			animationList.put(finalName, new Animation(animationSpeed, textureAtlas.findRegions(animationName.get(finalName)))) ;
		}
	
		scale = Scale ;
	}
	
	public static SIW_Data getRoss(Enum_AGE age)
	{
		TextureAtlas textureAtlas = new TextureAtlas(Gdx.files.internal("game/anim/" + age.path + ".atlas"));
	
		SIW_Data data = new SIW_Data() ; 
		data.animationList.put(Enum_AnimState.IDLE, new Animation(0.106f, textureAtlas.findRegions("idle"))) ;
		data.animationList.put(Enum_AnimState.WALK, new Animation(0.106f, textureAtlas.findRegions("move"))) ;
		
		data.scale = age.scale ; 
		
		return data ; 
	}
	


}
