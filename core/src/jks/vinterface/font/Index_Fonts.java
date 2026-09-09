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

