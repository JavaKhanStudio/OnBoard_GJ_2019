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
	/**
	 * Alpha per second, so the cloud is fully there a quarter of a second after the mouse
	 * lands on what it explains. It used to take a whole second (r57).
	 */
	float alphaGrowingSpeed = 4f; 
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
	
	/**
	 * Types straight away rather than once the cloud has finished fading in. A hint is read
	 * while the mouse is held on the key part, so every moment spent fading is a moment spent
	 * looking at an empty cloud - the text now fades up inside it (r57).
	 */
	public void applyText(String text) 
	{
		secondsLeft = -1 ; 
		waiting = null ; 
		textWhenVisible = text ; 
		appearing = true ;
		disappearing = false ; 
		typing.restart(textStartUp + text);
	}
	
	/**
	 * Seconds until a text shown by {@link #showForReading} fades by itself; negative while the
	 * text stays until someone takes it away, as a key hint does when the mouse leaves (r81).
	 */
	float secondsLeft = -1 ; 
	
	/** Time to read what is left once the typing is done, on top of the typing itself. */
	public static final float READING_SECONDS = 3f ; 
	/** Characters typed per second at {@link #typingSpeed}: 1.5 x TextraTypist's 20. */
	private static final float CHARACTERS_PER_SECOND = 30f ; 
	
	/**
	 * Types the text and fades it again once it has had time to be read (r81): an item's line
	 * when something is used on it, which nothing is held over to take away. Anything shown
	 * after it - a key hint under the mouse - replaces it and keeps its own rules.
	 */
	public void showForReading(String text)
	{
		applyText(text) ; 
		secondsLeft = text.length() / CHARACTERS_PER_SECOND + READING_SECONDS ; 
	}
	
	/** Seconds until a text shown by {@link #showForReading} has faded away; 0 when there is none. */
	public float secondsBusy()
	{
		return secondsLeft < 0 ? 0 : secondsLeft + 1f / alphaGrowingSpeed ; 
	}
	
	/** A text {@link #showForReading} will show once {@link #waitingSeconds} have passed. */
	String waiting ; 
	float waitingSeconds ; 
	
	/**
	 * {@link #showForReading}, after a pause (r73): a carriage's first thought waits for the
	 * carriage to fade in. Anything shown in the meantime - a hint under the mouse - cancels it.
	 */
	public void showForReading(String text, float afterSeconds)
	{
		waiting = text ; 
		waitingSeconds = afterSeconds ; 
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
	
	boolean reversed ; 
	
	public void reverse(boolean reverse) 
	{
		reversed = reverse ; 
		bubbleBackground.setSize(width * (reverse ? -1 : 1),height);
		if(reverse)
			typing.setPosition(decalXLeft - width, decalY * 1.5f);
		else
			typing.setPosition(decalXLeft, decalY * 1.5f);
	}
	
	@Override
	public void act(float delta) 
	{
		super.act(delta);

		if(waiting != null)
		{
			waitingSeconds -= delta ; 
			if(waitingSeconds <= 0)
				showForReading(waiting) ; 
		}
		
		if(secondsLeft >= 0)
		{
			secondsLeft -= delta ; 
			if(secondsLeft < 0)
				makeDisappear() ; 
		}
		
		// Clamped in the frame it steps, not the next: SpriteBatch packs alpha into a byte without
		// clamping, so 1.01 draws as 0 and -0.05 as 0.96. The cloud blinked out for one frame as
		// it finished fading in, and flashed back as it finished fading out, whenever a frame was
		// long enough to overshoot by more than 1/255: nearly every fade at 60 fps (r145).
		if(appearing) 
		{
			if(this.getColor().a >= 1) 
			{
				this.getColor().a = 1 ; 
				appearing = false ; 
			}
			else 
			{
				this.getColor().a = Math.min(1, this.getColor().a + alphaGrowingSpeed * delta) ; 
			}
			
		} 
		else if(disappearing)
		{
			if(this.getColor().a <= 0) 
			{	
				this.getColor().a = 0 ; 
				typing.restart("") ; 
				textWhenVisible = "" ;
				disappearing = false ; 
			}
			else 
			{
				this.getColor().a = Math.max(0, this.getColor().a - alphaGrowingSpeed * delta) ; 
			}
		}
			
		
	}

	public String getText()
	{
		return textWhenVisible ; 
	}
	
	
	/** Where the cloud is drawn on the stage: x, y, width, height; reverse() draws it left of getX(). */
	public float[] cloudBox()
	{
		return new float[] {reversed ? getX() - width : getX(), getY(), width, height * (1 - EMPTY_TOP)} ; 
	}
	
	/** bubble_think_2.png is empty above its cloud for 73 of its 1200 rows: that may leave the screen. */
	private static final float EMPTY_TOP = 73f / 1200f ; 
	
	/**
	 * Places the tail's corner at posX, posY, but never lets the cloud leave the screen: above a
	 * grown-up Ross it used to reach the top edge already, and it is bigger since r121. It slides
	 * down or sideways instead, its tail still pointing at him.
	 */
	public void setBubblePosition(float posX, float posY)
	{
		float left = reversed ? posX - width : posX ; 
		left = Math.max(0, Math.min(left, Gdx.graphics.getWidth() - width)) ; 
		posY = Math.max(0, Math.min(posY, Gdx.graphics.getHeight() - height * (1 - EMPTY_TOP))) ; 
		this.setPosition(reversed ? left + width : left, posY);
	}
	
	private static final float devisingSmall = 10 ; 
	private static final float devisingMedium = 7.2f ; 
	private static final float devisingLarge = 6.5f ; 
	
	/**
	 * The cloud is stretched wider than it is tall (r121). Its letters were made bigger, and a
	 * cloud grown the same in both directions would run off the top of the screen above
	 * Ross, so the longer lines go into the width instead.
	 */
	private static final float WIDE = 1.6f ; 
	private static final float TALL = 1.2f ; 
	
	float size ; 
	float width ; 
	float height ; 
	
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
		
		// The cloud is round: a line as wide as its middle runs onto its edge higher up.
		// Narrowed for Mansalva's wider lines (r103), from size/10 and size/9.
		width = size * WIDE ; 
		height = size * TALL ; 
		decalXLeft = width/7.0f ;
		decalXRight = width/6.5f ;
		decalY = height/8.5f ;
		
		this.setSize(width,height);
		typing.setSize(width - decalXLeft - decalXRight,height - (decalY * 2));
		reverse(reversed) ; 
		
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