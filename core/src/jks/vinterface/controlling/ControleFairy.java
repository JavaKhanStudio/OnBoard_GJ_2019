//package jks.vinterface.controlling;
//
//import com.badlogic.gdx.math.Vector2;
//import com.badlogic.gdx.scenes.scene2d.ui.Button;
//
//import jks.personnage.index.Enum_AnimState;
//import jks.personnage.index.SIW_Data;
//import jks.personnage.model.SpriteModel;
//
//public class ControleFairy extends SpriteModel
//{
//	
//	Vector2 fairyTargetPosition ;
//	
//	
//	public ControleFairy(SIW_Data index) 
//	{
//		super(index);
//		changeAnimationState(Enum_AnimState.IDLE, false);
//	}
//	
//	public void moveToo(Button currentButton)
//	{
//		teleportAt(currentButton) ; 
//	}
//	
//	public void teleportAt(Button currentButton) 
//	{
//		
//		float sizeY = getFrameHeight(index.animationList.get(Enum_AnimState.IDLE).getKeyFrame(0)) ;
//		
//		if(currentButton.getHeight() > sizeY)
//		{
//			float height = 1 ; 
//			position.set(currentButton.getX() - currentButton.getWidth()/2 , currentButton.getY()) ; 
//		}
//		else
//		{
//			position.set(currentButton.getX() - currentButton.getWidth()/2 , currentButton.getY() + currentButton.getHeight()/2) ; 
//		}
//	}
//
//}
