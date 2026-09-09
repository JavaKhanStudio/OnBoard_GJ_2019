package jks.tools;

import java.util.HashMap;

import jks.debug.GVars_Debug;

public class GlobalTimmer
{

	public static HashMap<Object,Long> timeHolder ;
	
	
	public static void registerTime(Object key)
	{
		if(timeHolder == null)
		{timeHolder = new  HashMap<Object,Long>() ;}
		
		timeHolder.put(key, System.currentTimeMillis()) ;
	}
	
	public static Long getElapse(String key, String state)
	{return getElapse(key,state, false);}
	
	public static Long getElapse(Object key, String state, boolean reset)
	{
		if(!GVars_Debug.coreInformationDebug)
			return 0L;
		
		long finishTime = System.currentTimeMillis();
		long took = (finishTime-timeHolder.get(key)) ; 
		System.out.println(key + " at state " + state + " took: " + took+ " ms");
		
		if(reset)
			timeHolder.put(key, finishTime) ;
		
		return took ; 
	}
	
	public static Long getElapse(Object key, boolean reset)
	{
		long finishTime, took = 0 ;
		try
		{
			finishTime = System.currentTimeMillis();
			took = (finishTime-timeHolder.get(key)) ; 

			if(reset)
				timeHolder.put(key, finishTime) ;
		}
		catch(Exception e)
		{
			System.out.println("Impossible to find " + key);
		}
		
		
		return took ; 
	}

	public static void purge() 
	{
		timeHolder = null ; 
	}
	
	
}
