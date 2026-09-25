package jks.vinterface.font;


public class Index_Fonts 
{
	private static final String PATH = "ui/fonts/";
    /**
     * Ross's thoughts (r103): Permanent Marker, by Font Diner, Apache 2.0 (its licence sits
     * beside it). Its lowercase is drawn as small capitals, the hand-lettered look of a BD
     * bubble. Capitals are wider, so the sizes below are a tenth smaller than OpenSans's were:
     * at the old ones the longest carriage line (wa1.cage.message2) ran onto the cloud's edge.
     * It was chosen from the faces tools/fetch_bubble_fonts.sh fetches, drawn side by side by
     * BubbleFontLabTest.
     */
    private static final String regular = "PermanentMarker-Regular.ttf";  
    
    public enum Enum_Fonts
    {
    	BUBBLE_SMALL_TEXT_LARGE(PATH + regular,111),
    	BUBBLE_MEDIUM_TEXT_LARGE(PATH + regular,92),
    	BUBBLE_LARGE_TEXT_LARGE(PATH + regular,72),
    	BUBBLE_SMALL_TEXT_MEDIUM(PATH + regular,156),
    	BUBBLE_MEDIUM_TEXT_MEDIUM(PATH + regular,133),
    	BUBBLE_LARGE_TEXT_MEDIUM(PATH + regular,110),
    	
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

