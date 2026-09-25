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
	/** The WALK key frames where a heel comes down, each played as a footstep. None: a silent walk. */
	public int[] stepFrames = {} ;
	/** World px a second at full walk; the rest of his speed is SpriteModel's. */
	public float walkSpeed ;
	
	/**
	 * One frame of Ross's walk (r114). 0.106 s until then, when his legs covered half the ground
	 * he did and he skated; Simon settled on 0.075 s and a speed from each age's step.
	 */
	public static final float WALK_FRAME_SECONDS = 0.075f ;
	/** Frames of 'move' from one heel strike to the next: two steps in its 8. */
	public static final int FRAMES_PER_STEP = 4 ;
	
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
		data.animationList.put(Enum_AnimState.WALK, new Animation(WALK_FRAME_SECONDS, textureAtlas.findRegions("move"))) ;
		
		data.scale = age.scale ; 
		data.walkSpeed = age.walkSpeed() ; 
		// Frames 4 and 8 of 'move', counted from 1: the front heel strikes in both, in every age's
		// sheet. Two steps a cycle, one every FRAMES_PER_STEP frames.
		data.stepFrames = new int[] {3, 7} ; 
		
		return data ; 
	}
	


}
