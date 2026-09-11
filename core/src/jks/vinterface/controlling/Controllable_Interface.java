package jks.vinterface.controlling;

import java.util.ArrayList;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;

import jks.tools.Vector2Int;
import jks.vinterface.GVars_UI;

/**
 * A screen the keyboard can drive. mapInterface() lays its widgets out as columns of rows:
 * up and down move within a column, left and right move between columns.
 */
public interface Controllable_Interface 
{
	public ArrayList<ArrayList<Actor>> mapInterface() ;
	
	public default Vector2Int startAt()
	{return new Vector2Int() ;}
	
	public default void activateInterfaceControle()
	{GVars_UI.activedInterface(this) ;}
	
	public default Controllable_Interface getClosingLink()
	{return null ;}
	
	/** What Escape presses. Null: Escape does nothing here. */
	public default Button getBackButton()
	{return null ;}

	/** True when the widgets show their own focus, so no outline is drawn around them. */
	public default boolean highlightsItself()
	{return false ;}

	public Controllable_Interface currentControllable = null;
}
