package jks.vue.models.game;

public class GVars_Personnage 
{

	public static float maxVelocityY = 17f ;
	/** World px a second, each second a key is held: full walk in about a tenth of a second (r114). */
	public static float velocityAccelerationX = 3000f ;
	public static float jumpVelocity =  16f ;
	public static float velocityXRun = 3f ;
	
	
	
	// Where Ross may walk, against the art: 2400 and 199 of the 3000-wide stretched carriage,
	// scaled by 0.948 with the items when it was drawn at its painted shape (d13, r70).
	public static final float maxPositionX = 2276 ;
	public static final float minPositionX = 189 ;	
	
	// His top speed is per age now, Enum_AGE.walkSpeed (r114): it was 8 px a frame for all four.
	public static final float minVelocityX =  0.001f ;
	
}
