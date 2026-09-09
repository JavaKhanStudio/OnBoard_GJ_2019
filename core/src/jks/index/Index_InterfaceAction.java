package jks.index;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction;
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

public class Index_InterfaceAction 
{

	final static float getUpInXSec = 0.5f ; 
	final static float getDownInXSec = 0.5f ; 
	
	public static void LogoPop(Actor applyOn)
	{
		float heightCenter = (applyOn.getHeight()/2 + Gdx.graphics.getHeight()/2) ; 
		float decalY = applyOn.getHeight()/20 ; 
		
		MoveToAction movementUp = new MoveToAction();
		movementUp.setPosition(applyOn.getX(), heightCenter + decalY);
		movementUp.setDuration(getUpInXSec);
		
		MoveToAction movementDown = new MoveToAction();
		movementDown.setPosition(applyOn.getX(), heightCenter);
		movementDown.setDuration(getUpInXSec);
		
	    SequenceAction sequence = new SequenceAction();
	    sequence.addAction(movementUp);
	    sequence.addAction(movementDown);

		AlphaAction alpha = new AlphaAction() ;
		alpha.setAlpha(1);
		alpha.setDuration(getUpInXSec);
		
	    
	    applyOn.addAction(sequence);
	    applyOn.addAction(alpha);
	}
	
	public static void upAndDown(Actor applyOn, float endUpAtX, float endUpAtY)
	{
		float heightCenter = (applyOn.getHeight()/2 + Gdx.graphics.getHeight()/2) ; 
		float decalY = applyOn.getHeight()/20 ; 
		
		MoveToAction movementUp = new MoveToAction();
		movementUp.setPosition(applyOn.getX(), heightCenter + decalY);
		movementUp.setDuration(getUpInXSec);
		
		MoveToAction movementDown = new MoveToAction();
		movementDown.setPosition(applyOn.getX(), heightCenter);
		movementDown.setDuration(getUpInXSec);
		
	    SequenceAction sequence = new SequenceAction();
	    sequence.addAction(movementUp);
	    sequence.addAction(movementDown);

		AlphaAction alpha = new AlphaAction() ;
		alpha.setAlpha(1);
		alpha.setDuration(getUpInXSec);
		
	    
	    applyOn.addAction(sequence);
	    applyOn.addAction(alpha);
	}
		
}