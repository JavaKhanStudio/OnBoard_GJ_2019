package jks.personnage.model;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;

public abstract class AnimationModel
{
	protected boolean reverse ;
	protected float stateTime ;
	public Vector2 position = new Vector2();
	
	public void update(float delta) 
	{stateTime += delta ;}
	
	public abstract void draw(Batch batch) ; 
	
	public void reverse(boolean reverse)
	{this.reverse = reverse ;}
	
	public boolean getIsReserve() {
		return reverse ; 
	}
	
}
