package jks.input;

import java.util.HashMap;

import com.badlogic.gdx.controllers.Controller;

public class GVars_Controller 
{
	public static HashMap<Controller,Player_Inputs> playerList ; 
	public static Player_Inputs pcPlayer ; 
	
	public static void init()
	{
		playerList = new HashMap<Controller,Player_Inputs>() ; 
		playerList.put(null, new Player_Inputs()) ; 
	}
	
	public static Player_Inputs getPlayer(Controller controller)
	{return playerList.get(controller) ;}
	
	
	public static void act(float delta)
	{
		// not controller ready
	}
}