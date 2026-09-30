package jks.editor;


import static jks.vinterface.GVars_UI.mainUi;

import java.util.Collection;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;

import jks.amain.Main_Application;
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
		
		GVars_UI.mainUi.addActor(scrollPane) ; 
	}
	
	
	float textZoneY  ; 
	float size_Bloc_Selection_Parallax_Width ; 
	float speedX = 200 ; 

	public void update(float delta)
	{
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
		GVars_Camera.beginFrame();
        
    	float delta = Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f);
    
    	if (delta > 0) 
    	{
    		update(delta) ; 
    	}
    	
    	if(GVars_Editor.workingOnLevel != null && GVars_Editor.workingOnLevel.readyForUse)
    	{
    		GVars_Editor.workingOnLevel.draw();
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
		Main_Application.fitToWindow(width, height) ;
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
		try 
		{
			String firstFile = files[0] ; 
			if("wa".contentEquals(getExtension(firstFile)))
				openLevel(new FileHandle(firstFile)) ;
			else
				openCarriageFolder(new FileHandle(firstFile)) ;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/** A dropped .wa: it becomes the level being edited. */
	private void openLevel(FileHandle handle)
	{
		WagonLevel level = GVars_Serialization.prepareJson().fromJson(WagonLevel.class, handle) ; 
		GVars_Editor.workingOnLevel = level ; 
		GVars_Editor.workingOnLevel.init(); 
		Index_Interface.manager.finishLoading() ; 
		GVars_Editor.workingOnLevel.setAsGameReady(); 
		
		System.out.println("shoudl work");
	}
	
	/** A dropped carriage folder (wa1/): its pngs become the items to place, its .plax the parallax. */
	private void openCarriageFolder(FileHandle folder)
	{
		String path = folder.toString() ;
		String pathEnding = path.substring(path.lastIndexOf("/") + 1, path.length()) ; 
		GVars_Editor.workingOnLevel.path_meta = pathEnding; 
		
		for(FileHandle file : folder.list()) 
			readCarriageFile(pathEnding, file.name()) ;
		
		loadItems(GVars_Editor.listItems.values()) ;
		
		GVars_Editor.workingOnLevel.init(); 
		GVars_Editor.workingOnLevel.setAsGameReady(); 
		itemList.setItems(GVars_Editor.listItems.values());
	}
	
	/**
	 * One file of a carriage folder. A png is an item; "name_1"/"name_2" is the state it
	 * leaves after the first or second use. WAGON.png is the carriage itself and is skipped.
	 */
	private void readCarriageFile(String pathEnding, String fullName)
	{
		if("png".equals(getExtension(fullName)) && !"WAGON.png".equals(fullName))
		{
			if(fullName.contains("_"))
				readAfterState(fullName) ;
			else
				GVars_Editor.listItems.put(fullName, new GameItem(pathEnding,fullName)) ; 
		}
		else if("plax".equals(getExtension(fullName)))
		{
			GVars_Editor.workingOnLevel.path_parallax = fullName; 
		}
	}
	
	private void readAfterState(String fullName)
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
	
	private void loadItems(Collection<GameItem> items)
	{
		for(GameItem item : items)
		{
			item.init(); 
		}
		Index_Interface.manager.finishLoading();
		for(GameItem item : items)
		{
			item.setGameReady(); 
		}
	}
	
	public static String getExtension(String fileName)
	{
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1) return "";
		return fileName.substring(dotIndex + 1);	
	}
}