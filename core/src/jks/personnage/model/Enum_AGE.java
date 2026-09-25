package jks.personnage.model;

public enum Enum_AGE 
{

	ENFANT("enfant", 0.75f,200f, 74f),
	ADO("ado", 0.67f,180f, 103f),
	SOLDAT("soldier", 0.67f,130f, 114f),
	MARIE("marie", 0.67f,130f, 116f)
	
	;
	
	public String path ; 
	public float scale ;
	public float positionY ; 
	/**
	 * How far one step of his walk carries him, in world px: heel to heel where the front heel
	 * strikes, measured drawn at this age's scale by RossStrideTest (r114). Grows with him.
	 */
	public float stepLength ; 
	
	Enum_AGE(String path, float scale, float positionY, float stepLength)
	{
		this.path = path ; 
		this.scale = scale ;
		this.positionY = positionY ; 
		this.stepLength = stepLength ; 
	}
	
	/**
	 * World px a second he walks at: one step every SIW_Data.FRAMES_PER_STEP frames of 'move',
	 * so a boot that is down stays where it landed instead of skating (r114).
	 */
	public float walkSpeed()
	{
		return stepLength / (SIW_Data.FRAMES_PER_STEP * SIW_Data.WALK_FRAME_SECONDS) ; 
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
