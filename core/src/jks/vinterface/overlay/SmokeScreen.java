package jks.vinterface.overlay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;

public class SmokeScreen 
{

	public boolean growing ; 
	
	Texture planLast ; 
	Texture planCurrent ;
	
	float movingAlpha ; 
	float alphaSpeed ; 
	
	public SmokeScreen() 
	{
		growing = true ; 
	}
	

	public void act(float delta) 
	{
		if(planCurrent == null)
			return ;
		
		movingAlpha += delta * movingAlpha ; 
	}
	
	public void draw(Batch batch)
	{
		
		if(planLast != null)
		{
			batch.draw(planLast,0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		}
		
		if(planCurrent != null)
		{
			batch.setColor(1, 1, 1, movingAlpha);
			batch.draw(planCurrent,0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		}
		
		batch.setColor(1, 1, 1, 1);
	}

}