package jks.personnage.model;

import jks.tools.Utils_Debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import jks.sounds.GVars_AudioManager;
import jks.vars.GVars_Heart;
import jks.vue.models.game.GVars_Personnage;

public class SpriteModel extends AnimationModel 
{

	public Enum_AnimState lastState ; 
	/** The WALK key frame drawn last, so a step sounds once as its frame comes up; -1 when not walking. */
	private int lastWalkFrame = -1 ;
	
	public SIW_Data index;
	public Animation<TextureRegion> currentState;
	public TextureRegion currentFrame;
	public TextureRegion refFrame;
	public Enum_AnimState currentAnimState;
	
	public Vector2 velocity = new Vector2();

	public SpriteModel(SIW_Data index) 
	{
		this.index = index;
		changeAnimationState(Enum_AnimState.IDLE, true);
//		TextureRegion texture = index.animationList.get(Enum_AnimState.IDLE).getKeyFrame(0);
	}

	float WIDTH, HEIGHT;

	@Override
	public void draw(Batch batch) 
	{

		if ((!GVars_Heart.isPaused || currentAnimState == Enum_AnimState.DEATH)) {
			stateTime += Gdx.graphics.getDeltaTime(); // Accumulate elapsed animation time
		}
		try
		{
			if (currentState != null) {
				currentFrame = currentState.getKeyFrame(stateTime, PlayMode.LOOP == currentState.getPlayMode());
				playFootsteps() ;
	
				WIDTH = getFrameWidth(currentFrame);
				HEIGHT = getFrameHeight(currentFrame);
	
				batch.draw(currentFrame, position.x + (reverse ? 0 : WIDTH), position.y, WIDTH * (reverse ? 1 : -1),
						HEIGHT);
			} 
			else 
			{Utils_Debug.warn("impossible de trouver state pour");}
		}
		catch(Exception e)
		{
			Utils_Debug.warn("Impossible de trouver " + currentAnimState.name());
		}

	}
	
	/**
	 * A footstep each time a heel comes down (r87). Driven by the frame on screen rather than a
	 * timer, so the sound stays on the foot, stops the moment he is IDLE, and holds while paused.
	 */
	private void playFootsteps()
	{
		if(currentAnimState != Enum_AnimState.WALK)
		{
			lastWalkFrame = -1 ;
			return ;
		}
		
		int frame = currentState.getKeyFrameIndex(stateTime) ;
		if(frame == lastWalkFrame)
			return ;
		lastWalkFrame = frame ;
		for(int step : index.stepFrames)
			if(frame == step)
				GVars_AudioManager.PlayFootstep() ;
	}
	
	@Override
	public void update(float delta) 
	{

		if(velocity.x > 0) {
			reverse = true ; 
		} else if(velocity.x < 0) {
			reverse = false ; 
		}
		
		if(velocity.x > 0 && velocity.x > GVars_Personnage.maxVelocityX)
			velocity.x = GVars_Personnage.maxVelocityX ; 
		if(velocity.x < 0 && velocity.x < -GVars_Personnage.maxVelocityX)
			velocity.x = -GVars_Personnage.maxVelocityX ; 
		
		if(position.x >= GVars_Personnage.maxPositionX)
		{
			position.x = GVars_Personnage.maxPositionX ; 
			if(velocity.x > 0)
				velocity.x = 0 ; 
		}
		else if(position.x <= GVars_Personnage.minPositionX)
		{
			position.x = GVars_Personnage.minPositionX ; 
			if(velocity.x < 0)
				velocity.x = 0 ; 
		}
			
		
		position.add(velocity);
		checkForAnim() ; 
		//velocity.scl(1 / delta); // Change velocity back.

	}
	
	public void checkForAnim() 
	{
		Enum_AnimState shouldBe = null ; 
		if (Math.abs(velocity.x) < GVars_Personnage.minVelocityX) 
		{
			velocity.x = 0;
			shouldBe = Enum_AnimState.IDLE ;
		} 
		else
		{
			shouldBe = Enum_AnimState.WALK;
		}
//		else if (Math.abs(velocity.x) > GVars_Personnage.velocityXRun) 
//		{
//			shouldBe = Enum_AnimState.RUN ;
//		} 
//		else 
//		{
//			shouldBe = Enum_AnimState.WALK;
//		}
		
		if(shouldBe != null)
			changeAnimationState(shouldBe,true) ;
	}
	
	public float getFrameWidth() 
	{
		if(refFrame == null) 
			return 0 ; 
		else
			return refFrame.getRegionWidth() * index.scale ;
	}

	public float getFrameHeight() 
	{
		if(refFrame == null) 
			return 0 ; 
		else
			return refFrame.getRegionHeight() * index.scale ;
	}

	public float getFrameWidth(TextureRegion frame) 
	{return (frame.getRegionWidth() * index.scale);}

	public float getFrameHeight(TextureRegion frame) 
	{return (frame.getRegionHeight() * index.scale);}

	
	public void changeAnimationState(Enum_AnimState state, boolean repeat)
	{
		currentState = index.animationList.get(state) ;
		refFrame = currentState.getKeyFrame(0) ; 
		if(Enum_AnimState.IDLE == state) 
		{
			repeat = false ; 
		}
		if(currentState == null)
		{
			currentState = index.animationList.values().iterator().next() ;
			Utils_Debug.warn("Impossible de trouver l'animation - " + state.toString() +  " - dans SpriteModel");
		}
		currentAnimState = state  ;

		if (repeat)
			currentState.setPlayMode(PlayMode.LOOP);
		else
			currentState.setPlayMode(PlayMode.NORMAL);

		lastState = state;
	}

}
