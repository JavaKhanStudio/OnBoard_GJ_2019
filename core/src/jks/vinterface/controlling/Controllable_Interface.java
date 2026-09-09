package jks.vinterface.controlling;

import java.util.ArrayList;

import com.badlogic.gdx.scenes.scene2d.ui.Button;

import jks.tools.Vector2Int;
import jks.vinterface.GVars_UI;

public interface Controllable_Interface 
{
	public ArrayList<ArrayList<Button>> mapInterface() ;
	
	public default Vector2Int startAt()
	{return new Vector2Int() ;}
	
	public default void activateInterfaceControle()
	{GVars_UI.activedInterface(this) ;}
	
	public default Controllable_Interface getClosingLink()
	{return null ;}
	
	public Controllable_Interface currentControllable = null;
}
