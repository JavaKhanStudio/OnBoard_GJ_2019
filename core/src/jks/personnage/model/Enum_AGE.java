package jks.personnage.model;

public enum Enum_AGE 
{

	ENFANT("enfant", 0.75f,200f),
	ADO("ado", 0.67f,180f),
	SOLDAT("soldier", 0.67f,130f),
	MARIE("marie", 0.67f,130f)
	
	;
	
	public String path ; 
	public float scale ;
	public float positionY ; 
	
	Enum_AGE(String path, float scale, float positionY)
	{
		this.path = path ; 
		this.scale = scale ;
		this.positionY = positionY ; 
	}

	public static Enum_AGE getFromName(String rossAge) 
	{
		for(Enum_AGE age : Enum_AGE.values())
		{
			if(age.path.equals(rossAge))
				return age ; 
		}
		
		return null;
	}
	
}
