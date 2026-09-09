package jks.editor;


import static jks.camera.GVars_Camera.camera;
import static jks.camera.GVars_Camera.staticBatch;
import static jks.vinterface.GVars_UI.mainUi;

import java.util.Collection;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.kotcrab.vis.ui.widget.VisTextButton;

import jks.camera.GVars_Camera;
import jks.index.Index_Interface;
import jks.input.GVars_Inputs;
import jks.vars.GVars_Heart;
import jks.vars.GVars_Serialization;
import jks.vinterface.GVars_UI;
import jks.vinterface.tools.JksTextureList;
import jks.vue.models.game.GameItem;
import jks.vue.models.game.WagonLevel;

public class Editor_Application extends ApplicationAdapter implements ImportAction
{

	Texture background ; 
		
	JksTextureList itemList ;
	
	@Override
	public void create() 
	{
		GVars_Editor.init(this) ;  
		Index_Interface.checkManager(); 
		GVars_UI.init();
		
		Gdx.input.setInputProcessor(new InputMultiplexer(GVars_UI.mainUi, new Editor_Keyboard()));
		
		GVars_Camera.init();
		if(GVars_Heart.isFullScreen)
		{
			DisplayMode mode = Gdx.graphics.getDisplayMode();
			Gdx.graphics.setFullscreenMode(mode);
		}
		
		size_Bloc_Selection_Parallax_Width = Gdx.graphics.getWidth()/8 ;

		itemList = buildItemList() ; 
		itemList.setWidth(size_Bloc_Selection_Parallax_Width);
		itemList.setHeight(Gdx.graphics.getHeight());
		
		ScrollPane scrollPane;
		scrollPane = new ScrollPane(itemList,GVars_UI.baseSkin) ; 
		scrollPane.setWidth(size_Bloc_Selection_Parallax_Width);
		scrollPane.setHeight(Gdx.graphics.getHeight());
		scrollPane.setFadeScrollBars(false);
		scrollPane.setWidth(size_Bloc_Selection_Parallax_Width);
		scrollPane.setHeight(Gdx.graphics.getHeight());
		
		
		VisTextButton button = new VisTextButton("TEST THIS") ; 
		button.setWidth(1000);
		button.setHeight(1000);
		GVars_UI.mainUi.addActor(scrollPane) ; 
//		GVars_UI.mainUi.addActor(button) ; 
	}
	
	
	float textZoneY  ; 
	float size_Bloc_Selection_Parallax_Width ; 
	float speedX = 200 ; 

	public void update(float delta)
	{
//		System.out.println("i do press " + GVars_Inputs.rightPressed);
		if(GVars_Inputs.rightPressed)
		{
			GVars_Camera.camera.translate(speedX * delta, 0);
		}
		else if(GVars_Inputs.leftPressed)
		{
			GVars_Camera.camera.translate(-speedX * delta, 0);
		}
		
		if(GVars_Editor.workingOnLevel != null && GVars_Editor.workingOnLevel.readyForUse)
    	{
			GVars_Editor.workingOnLevel.update(delta);
    	}
	}
	

	@Override
	public void render () 
	{
		camera.update();
    	Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        staticBatch.setProjectionMatrix(camera.combined);
        
    	float delta = Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f);
    
    	if (delta > 0) 
    	{
    		update(delta) ; 
    	}
    	
    	if(GVars_Editor.workingOnLevel != null && GVars_Editor.workingOnLevel.readyForUse)
    	{
//    		GVars_Camera.staticBatch.begin() ;
    		GVars_Editor.workingOnLevel.draw();
//    		GVars_Camera.staticBatch.end() ;
    	}
    	
    	mainUi.draw() ;	
	}
	
	private JksTextureList buildItemList() 
	{
		return new JksTextureList(GVars_UI.baseSkin,size_Bloc_Selection_Parallax_Width,size_Bloc_Selection_Parallax_Width/2) 
		{
			@Override
			public void choiceAction(GameItem item)
			{
				System.out.println();
				GVars_Editor.selectedItem = item ; 
			}
			
			@Override
			public void drawOnSelected(Batch batch, float x, float y, float width, float itemHeight)
			{	
				
			}
			
		};			
	}

    
    @Override
	public void resize(int width, int height) 
	{
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		if(GVars_Heart.vue != null)
		{
			GVars_Heart.vue.resize(width,height) ; 
		}	
	}
    
    @Override
    public void pause()
    {}

    @Override
    public void resume()
    {}

    @Override
	public void dispose() 
	{
    
    }

	@Override
	public void reciveFiles(String[] files) 
	{
		String errorMessage = "";
		
		try 
		{
			String firstFile = files[0] ; 
			if("wa".contentEquals(getExtension(firstFile)))
			{
				FileHandle handle = new FileHandle(files[0]); 
				WagonLevel level = GVars_Serialization.objectMapper.readValue(handle.file(), WagonLevel.class) ; 
				GVars_Editor.workingOnLevel = level ; 
				GVars_Editor.workingOnLevel.init(); 
				Index_Interface.manager.finishLoading() ; 
				GVars_Editor.workingOnLevel.setAsGameReady(); 
				
				System.out.println("shoudl work");
				return ; 
			}
			
			
			FileHandle fileHandle = new FileHandle(firstFile) ; 
			
			String path = fileHandle.toString() ;
			String pathEnding = path.substring(path.lastIndexOf("/") + 1, path.length()) ; 
			GVars_Editor.workingOnLevel.path_meta = pathEnding; 
			
			for(FileHandle file : fileHandle.list()) 
			{
				String fullName = file.name() ; 
				if("png".equals(getExtension(fullName)) && !"WAGON.png".equals(fullName))
				{
					if(fullName.contains("_"))
					{
						GameItem gameItem = GVars_Editor.listItems.get(fullName) ; 
						String position = fullName.substring(fullName.length() - 1, fullName.length()) ; 
						if("1".equals(position))
						{
							gameItem.path_EtatApres_1 = fullName ; 
						}
						else if("2".equals(position))
						{
							gameItem.path_EtatApres_2 = fullName ; 
						}
						else
						{
							System.err.println("IDK what apres") ; 
						}
						
					}
					else
					{
						GameItem gameItem = new GameItem(pathEnding,fullName) ; 
						GVars_Editor.listItems.put(fullName,gameItem) ; 
					}
				}
				else if("WAGON.png".equals(fullName))
				{
					
				}	
				else if("plax".equals(getExtension(fullName)))
				{
					GVars_Editor.workingOnLevel.path_parallax = fullName; 
				}
					
			}
			
			Collection<GameItem> myValues = GVars_Editor.listItems.values() ; 
			
			for(GameItem item : myValues)
			{
				item.init(); 
			}
			Index_Interface.manager.finishLoading();
			for(GameItem item : myValues)
			{
				item.setGameReady(); 
			}
			
			GVars_Editor.workingOnLevel.init(); 
			GVars_Editor.workingOnLevel.setAsGameReady(); 
			itemList.setItems(myValues);

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static String getExtension(String fileName)
	{
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1) return "";
		return fileName.substring(dotIndex + 1);	
	}
}