package jks.vue;

import static jks.vinterface.GVars_UI.mainUi;

import java.util.ArrayList;

import org.lwjgl.opengl.GL30;

import com.badlogic.gdx.Gdx;

import jks.vinterface.ToRender;
import jks.vinterface.overlay.OverlayModel; 

public abstract class AVue_Model 
{

	public ArrayList<ToRender> toRender = new ArrayList<ToRender>(); 
	
	public OverlayModel overlay ; 
	public AView_Model_Filter filter ; 
	
	public abstract void init() ;
	public abstract void destroy() ;
	public abstract void restart() ; 
	
	public abstract void update (float delta) ;
	public abstract void render () ;
	
	public void resize(int x, int y)
	{
		if(filter != null)
		{filter.resize();}
		
		if(overlay != null)
		{overlay.resize();}
	
	}
	
	public void clear()
	{
		Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);
	}
	
	public void renderBeforeInterface()
	{
		for (ToRender rende : toRender) 
			rende.render();
		

		if (filter != null) 
			filter.draw();
		
	}
	
	public void drawInterface()
	{
		mainUi.draw() ;		
	}
	
}