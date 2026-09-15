package jks.vinterface.tools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisTable;
import com.github.tommyettinger.textra.TypingLabel;

import jks.index.Index_Interface;
import jks.vinterface.Utils_TexturesAcess;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.font.Index_Fonts.Enum_Fonts;

public class DialogBubble extends VisTable
{

	VisImage bubbleBackground ; 
	TypingLabel typing ; 
	float decalXRight ;
	float decalXLeft ;
	float decalY ;	
	
	DialogSize dialogSize ; 
	
	boolean appearing ; 
	boolean disappearing ; 
	boolean isFinish ; 
	float alphaGrowingSpeed = 1.2f; 
	public String textWhenVisible ; 
	
	/**
	 * Typing speed as a multiple of TextraTypist's default of 0.05 s per character, so 1.5
	 * types about 30 characters a second instead of 20 (r42). It is a token and not a
	 * setter because TypingLabel.restart() puts the speed back to the default every time.
	 */
	public static final String typingSpeed = "{SPEED=1.5}" ; 
	public static final String textStartUp = typingSpeed + "{COLOR=black}{EASE}" ; 

	public DialogBubble(DialogSize size)
	{
		this.setLayoutEnabled(false);
		dialogSize = size ; 
		textWhenVisible = "" ; 
		typing = new TypingLabel("", GVars_Font.buildTextraFont(fontFor(size))) ; 
		typing.setWrap(true);
		typing.setAlignment(Align.center);
		
		Texture texture = Index_Interface.manager.get(Index_Interface.bubbleThink) ; 
		bubbleBackground = new VisImage(Utils_TexturesAcess.buildDrawingRegionTexture(texture)) ; 
		
		this.add(bubbleBackground) ;
		this.add(typing) ; 
	
		this.setColor(1, 1, 1, 0);
		resize() ; 
//		appearing = true ; 
	}
	
	public void applyText(String text) 
	{
		typing.restart("");
		textWhenVisible = text ; 
		appearing = true ;
		disappearing = false ; 
		alphaGrowingSpeed = 1 ; 
	}
	
	public void makeDisappear() {
		disappearing = true ; 
	}
	
	public DialogBubble(String text, DialogSize size, boolean onePage)
	{
		this.setLayoutEnabled(false);
		dialogSize = size ; 

		typing = new TypingLabel(typingSpeed + text, GVars_Font.buildTextraFont(fontFor(size))) ; 
		typing.setWrap(true);
		typing.setAlignment(Align.center);
		
		
		Texture texture = Index_Interface.manager.get(Index_Interface.bubbleThink) ; 
		bubbleBackground = new VisImage(Utils_TexturesAcess.buildDrawingRegionTexture(texture)) ;
		
		
		this.add(bubbleBackground) ;
		this.add(typing) ; 
	
		resize() ; 
	}
	
	public void reverse(boolean reverse) 
	{
		bubbleBackground.setSize(size * (reverse ? -1 : 1),size);
		if(reverse)
			typing.setPosition(decalXLeft - size, decalY * 1.5f);
		else
			typing.setPosition(decalXLeft, decalY * 1.5f);
	}
	
	@Override
	public void act(float delta) 
	{
		super.act(delta);

		if(appearing) 
		{
			if(this.getColor().a >= 1) 
			{
				if(textWhenVisible != null)
					typing.restart(textStartUp + textWhenVisible);
				
				appearing = false ; 
			}
			else 
			{
				this.getColor().a += alphaGrowingSpeed * delta ; 
			}
			
		} 
		else if(disappearing)
		{
			if(this.getColor().a <= 0) 
			{	
				typing.restart("") ; 
				textWhenVisible = "" ;
				disappearing = false ; 
			}
			else 
			{
				this.getColor().a -= alphaGrowingSpeed * delta ; 
			}
		}
			
		
	}

	public String getText()
	{
		return textWhenVisible ; 
	}
	
	
	public void setBubblePosition(float posX, float posY)
	{
		this.setPosition(posX, posY);
	}
	
	private static final float devisingSmall = 10 ; 
	private static final float devisingMedium = 7.2f ; 
	private static final float devisingLarge = 6.5f ; 
	
	float size ; 
	
	public void resize()
	{
		size = 0; //200
		switch(dialogSize) 
		{
			case BUBBLE_SMALL_TEXT_LARGE :
			{
				size = Gdx.graphics.getWidth()/devisingSmall ; 	
				typing.setFont(GVars_Font.buildTextraFont(Enum_Fonts.BUBBLE_SMALL_TEXT_LARGE));
				break ; 
			} 
			case BUBBLE_MEDIUM_TEXT_LARGE :
			{
				size = Gdx.graphics.getWidth()/devisingMedium ; 	
				typing.setFont(GVars_Font.buildTextraFont(Enum_Fonts.BUBBLE_MEDIUM_TEXT_LARGE));
				break ; 
			} 
			case BUBBLE_LARGE_TEXT_LARGE : 
			{
				size = Gdx.graphics.getWidth()/devisingLarge ; 	
				typing.setFont(GVars_Font.buildTextraFont(Enum_Fonts.BUBBLE_LARGE_TEXT_LARGE));
				break ; 
			} 
			case BUBBLE_SMALL_TEXT_MEDIUM  : 
			{
				size = Gdx.graphics.getWidth()/devisingSmall ; 	
				typing.setFont(GVars_Font.buildTextraFont(Enum_Fonts.BUBBLE_SMALL_TEXT_MEDIUM));
				break ; 
			}
			case BUBBLE_MEDIUM_TEXT_MEDIUM :
			{
				size = Gdx.graphics.getWidth()/devisingMedium ;
				typing.setFont(GVars_Font.buildTextraFont(Enum_Fonts.BUBBLE_MEDIUM_TEXT_MEDIUM));
				break ; 
			}
			case BUBBLE_LARGE_TEXT_MEDIUM :
			{
				size = Gdx.graphics.getWidth()/devisingLarge ; 	
				typing.setFont(GVars_Font.buildTextraFont(Enum_Fonts.BUBBLE_LARGE_TEXT_MEDIUM));
				break ; 
			}
		}
		
		decalXLeft = size/10.0f ;
		decalXRight = size/9.0f ;
		decalY = size/8.5f ;
		
		this.setSize(size,size);
		typing.setSize(size - decalXLeft - decalXRight,size - (decalY * 2));
		typing.setPosition(decalXLeft, decalY * 1.5f);
		bubbleBackground.setSize(size, size);
		
	}
	
	/**
	 * TextraTypist resolves a Skin into its own Styles.LabelStyle, and uiskin.json only
	 * declares the scene2d one, so the label is built from the font directly. resize() sets
	 * the definitive face a moment later anyway; this just gives the constructor a real font.
	 *
	 * The two enums are parallel by construction - one entry per bubble size.
	 */
	private static Enum_Fonts fontFor(DialogSize size)
	{
		return Enum_Fonts.valueOf(size.name()) ; 
	}
	
	public enum DialogSize
	{
		BUBBLE_SMALL_TEXT_LARGE,
		BUBBLE_MEDIUM_TEXT_LARGE,
		BUBBLE_LARGE_TEXT_LARGE,
		BUBBLE_SMALL_TEXT_MEDIUM,
		BUBBLE_MEDIUM_TEXT_MEDIUM,
		BUBBLE_LARGE_TEXT_MEDIUM
	}
}