package jks.vinterface.tools;

import com.kotcrab.vis.ui.widget.VisImageButton;

public abstract class DialogBubbleWithOptions extends DialogBubble
{

	VisImageButton yes ; 
	VisImageButton no ; 
	
	public DialogBubbleWithOptions(String text, String pathYes, String pathNo) 
	{
		super(DialogSize.BUBBLE_MEDIUM_TEXT_MEDIUM) ; 
		
		
		
	}

	public abstract void yesAction() ;
	public abstract void noAction() ;
	
}
