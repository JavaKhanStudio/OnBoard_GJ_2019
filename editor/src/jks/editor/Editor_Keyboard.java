package jks.editor;


import static jks.editor.GVars_Editor.listItems;
import static jks.editor.GVars_Editor.listItemsInLevel;
import static jks.editor.GVars_Editor.selectedItem;
import static jks.editor.GVars_Editor.workingOnLevel;

import java.io.File;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector3;

import jks.camera.GVars_Camera;
import jks.input.GVars_Inputs;
import jks.vars.GVars_Serialization;
import jks.vue.models.game.GameItem;

public class Editor_Keyboard extends InputAdapter 
{
	
	
	
	// PC input
	@Override
	public boolean keyDown (int keycode) 
	{
		
		System.out.println("should be editor");
		if(Keys.D == keycode || Keys.RIGHT == keycode)
		{
			GVars_Inputs.rightPressed = true ; 
			return true ; 
		}
		
		else if(Keys.Q == keycode || Keys.LEFT == keycode)
		{
			GVars_Inputs.leftPressed = true ; 
			return true ; 
		}
		
		return true ; 
	}

	@Override
	public boolean keyUp (int keycode) 
	{		
		if(Keys.D == keycode || Keys.RIGHT == keycode)
		{
			GVars_Inputs.rightPressed = false ; 
			return true ; 
		}
		
		else if(Keys.Q == keycode || Keys.LEFT == keycode)
		{
			GVars_Inputs.leftPressed = false ; 
			return true ; 
		}
		else if(Keys.S == keycode)
		{
			try 
			{
				GVars_Serialization.objectMapper.writeValue(new File(GVars_Editor.workingOnLevel.path_meta + ".wa"),GVars_Editor.workingOnLevel);
			} 
			catch (Exception e) 
			{e.printStackTrace();}
			
			return true ; 
		}
						
		return false ; 
	}
	
	
	@Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button)
	{	
		if(button == 0)
		{
			if(selectedItem != null)
			{
				GameItem itemIn = listItemsInLevel.get(selectedItem.name) ; 
				if(itemIn == null)
				{
					itemIn = listItems.get(selectedItem.name) ;
					listItemsInLevel.put(selectedItem.name, itemIn) ; 
					workingOnLevel.listItems.add(itemIn) ; 
				}
				
				// Through the camera, whose world is 1600x900 whatever the window (r64): the
				// screen-pixel sum this replaces only placed items right in a 1600x900 editor.
				Vector3 at = GVars_Camera.camera.unproject(new Vector3(screenX, screenY, 0)) ;
				itemIn.setPosition(at.x, at.y);
			}
		}
		else
		{
			Vector3 tmp= new Vector3(Gdx.input.getX(),Gdx.input.getY(),0);
			GVars_Camera.camera.unproject(tmp);
			
			for(GameItem item : GVars_Editor.workingOnLevel.listItems)
			{
				item.tryTouch(tmp,null, true);
			}
		}
		
		
        return false;
    }
		
}