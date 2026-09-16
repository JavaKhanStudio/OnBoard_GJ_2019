package jks.vinterface.font;


public class Index_Fonts 
{
	private static final String PATH = "ui/fonts/";
    private static final String regular = "OpenSans-Regular.ttf";  
    
    public enum Enum_Fonts
    {
    	BUBBLE_SMALL_TEXT_LARGE(PATH + regular,101),
    	BUBBLE_MEDIUM_TEXT_LARGE(PATH + regular,84),
    	BUBBLE_LARGE_TEXT_LARGE(PATH + regular,65),
    	BUBBLE_SMALL_TEXT_MEDIUM(PATH + regular,142),
    	BUBBLE_MEDIUM_TEXT_MEDIUM(PATH + regular,121),
    	BUBBLE_LARGE_TEXT_MEDIUM(PATH + regular,100),
    	
    	/**
    	 * The card the game ends on (r60, ClosureMessage), and the FIN under it.
    	 *
    	 * GeosansLight, not OptimusPrinceps: the message is French and OptimusPrinceps has no
    	 * accented glyph, which would leave holes rather than throw (doubt d8).
    	 */
    	STORY_CLOSURE(PATH + "GeosansLight.ttf",46),
    	STORY_END(PATH + "GeosansLight.ttf",20),
    	;
    	
    	public String path ;
    	public int basedSizeDevide ; 
    	
    	Enum_Fonts(String path, int basedSize)
    	{
    		this.path = path ; 
    		this.basedSizeDevide = basedSize ; 
    	}
    	
    	 

    }
}

