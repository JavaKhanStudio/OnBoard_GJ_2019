package jks.vinterface.font;


public class Index_Fonts 
{
	private static final String PATH = "ui/fonts/";
    /**
     * Ross's thoughts (r103): Mansalva, by Carolina Short, SIL Open Font License (its licence
     * sits beside it). A quick felt-pen hand, the lettering of a BD bubble. Simon's pick from
     * the faces tools/fetch_bubble_fonts.sh fetches, drawn side by side by BubbleFontLabTest.
     * Its lines run wider than the plainer sans the bubbles used before r103: at that face's
     * capital height, carriage 3's wa3.trou.message2 ran past the round cloud's edges. So the
     * sizes below are the pre-r103 ones a twelfth smaller, and DialogBubble wraps them narrower than it used to.
     * Then they were too small to read (r121): all six are a third bigger again (divided by
     * 1.35), and DialogBubble stretches its cloud wider to hold them.
     */
    private static final String regular = "Mansalva-Regular.ttf";  
    
    public enum Enum_Fonts
    {
    	BUBBLE_SMALL_TEXT_LARGE(PATH + regular,81),
    	BUBBLE_MEDIUM_TEXT_LARGE(PATH + regular,67),
    	BUBBLE_LARGE_TEXT_LARGE(PATH + regular,52),
    	BUBBLE_SMALL_TEXT_MEDIUM(PATH + regular,113),
    	BUBBLE_MEDIUM_TEXT_MEDIUM(PATH + regular,97),
    	BUBBLE_LARGE_TEXT_MEDIUM(PATH + regular,80),
    	
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

