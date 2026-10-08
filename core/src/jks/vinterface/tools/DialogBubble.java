package jks.vinterface.tools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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
		build(size, "") ; 
		textWhenVisible = "" ; 
	
		this.setColor(1, 1, 1, 0);
		resize() ; 
	}
	
	/** The cloud and the label typed in it, as both constructors make them. */
	private void build(DialogSize size, String text)
	{
		this.setLayoutEnabled(false);
		dialogSize = size ; 
		typing = new TypingLabel(text, GVars_Font.buildTextraFont(fontFor(size))) ; 
		typing.setWrap(true);
		typing.setAlignment(Align.center);
		
		Texture texture = Index_Interface.manager.get(Index_Interface.bubbleThink) ; 
		bubbleBackground = new VisImage(Utils_TexturesAcess.buildDrawingRegionTexture(texture)) ; 
		int split = Math.round(texture.getHeight() * TAIL_TOP) ; 
		cloudAbove = new TextureRegionDrawable(new TextureRegion(texture, 0, 0, texture.getWidth(), split)) ; 
		tail = new TextureRegion(texture, 0, split, texture.getWidth(), texture.getHeight() - split) ; 
		
		this.add(bubbleBackground) ;
		this.add(typing) ; 
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
		sunk = 0 ; 
		typing.restart(textStartUp + gentle(text));
		fitCloud() ; 
	}
	
	/**
	 * TextraTypist's own {WAVE} lifts a word a third of a line: caught high, it reads as a word
	 * printed on the line above (r163). Half that still waves (D4). The table keeps {WAVE}.
	 */
	public static final String GENTLE_WAVE = "{WAVE=0.5}" ; 
	
	static String gentle(String text)
	{
		return text.replace("{WAVE}", GENTLE_WAVE) ; 
	}
	
	/**
	 * A long line gets a bigger cloud rather than smaller letters or a rim it runs into (r163):
	 * the cloud grows by GROWTH, up to MAX_GROWTH, until the typed text fits its body. The
	 * letters stay the size they are; only the room around them, and so the wrap, grows.
	 */
	private void fitCloud()
	{
		for(scale = 1f ; ; scale *= GROWTH)
		{
			layoutCloud() ; 
			if(textFits() || scale * GROWTH > MAX_GROWTH + 0.001f)
				return ; 
		}
	}
	
	/** The text, wrapped at the body's width, is no taller than the body. */
	public boolean textFits()
	{
		typing.invalidate() ; 
		return typing.getPrefHeight() <= typing.getHeight() + 1 ; 
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
		build(size, typingSpeed + text) ; 
		resize() ; 
	}
	
	boolean reversed ; 
	
	/**
	 * Where the tail is cut from the cloud, as a fraction of bubble_think_2.png from its top: under
	 * the text's body (BODY_BOTTOM), so the cut never passes behind a letter. What is under it -
	 * the cloud's lower scallops and the puffs trailing to Ross - is drawn behind him (r249).
	 */
	static final float TAIL_TOP = 800f / 1200f ; 
	/** The cloud above TAIL_TOP, which the stage draws once the tail is drawn apart. */
	private TextureRegionDrawable cloudAbove ; 
	/** The cloud under TAIL_TOP, which {@link #drawTail} draws before Ross. */
	private TextureRegion tail ; 
	/** Whether the tail is left to {@link #drawTail}: in a carriage, not in the line lab. */
	boolean tailApart ; 
	
	/**
	 * Leaves the tail to {@link #drawTail}, for a view that draws Ross: the stage is drawn over
	 * the whole level, so a tail it drew would cross his face whenever the cloud slides down.
	 */
	public void drawTailApart()
	{
		tailApart = true ; 
		bubbleBackground.setDrawable(cloudAbove) ; 
		reverse(reversed) ; 
	}
	
	/**
	 * The tail, at the cloud's place and fade, on the stage's screen pixels: drawn by the view after
	 * the level and before Ross, so he stands in front of it. The rest of the cloud and its text
	 * stay on the stage, in front of the key (r106).
	 */
	public void drawTail(Stage stage)
	{
		float alpha = getColor().a ; 
		if(!tailApart || !isVisible() || getStage() == null || alpha <= 0)
			return ; 
		Batch batch = stage.getBatch() ; 
		stage.getCamera().update() ; 
		batch.setProjectionMatrix(stage.getCamera().combined) ; 
		batch.begin() ; 
		for(int band = sunk > 0 ? 0 : BANDS ; band <= BANDS ; band++)
		{
			float share = bandShare(band) ; 
			if(share <= 0 || !clipBand(batch, band))
				continue ; 
			batch.setColor(1, 1, 1, alpha * share) ; 
			batch.draw(tail, getX(), getY(), reversed ? -width : width, height * (1 - TAIL_TOP)) ; 
		}
		unclip(batch) ; 
		batch.setColor(1, 1, 1, 1) ; 
		batch.end() ; 
	}
	
	/**
	 * How far the cloud has dissolved, from its bottom up (r262): 0 whole, 1 gone. The steam
	 * leaving a carriage sets it as it closes, so the cloud fades away under the rising steam
	 * before the steam covers it, instead of standing over the covered screen. A new line puts
	 * it back to 0.
	 */
	float sunk ; 
	/** The soft edge of the dissolve, as a share of the cloud's height, cut in BANDS steps. */
	static final float SOFT_EDGE = 0.4f ; 
	static final int BANDS = 32 ; 
	
	public void sink(float sunk)
	{
		this.sunk = Math.max(0, Math.min(1, sunk)) ; 
	}
	
	public float sunk()
	{
		return sunk ; 
	}
	
	/** Where the gone part ends, from the cloud's bottom, as a share of its height. */
	private float sinkLine()
	{
		return -SOFT_EDGE + sunk * (1 + SOFT_EDGE) ; 
	}
	
	/**
	 * The share of the cloud a band shows: bands 0 to BANDS-1 cut the soft edge from the gone
	 * part up, band BANDS is everything above it, whole.
	 */
	private float bandShare(int band)
	{
		if(sunk <= 0)
			return band == BANDS ? 1 : 0 ; 
		return band == BANDS ? 1 : (band + 0.5f) / BANDS ; 
	}
	
	/** Clips the batch to a band of the cloud, in the stage's screen pixels; false if it is off the cloud. */
	private boolean clipBand(Batch batch, int band)
	{
		if(sunk <= 0)
			return true ; 
		float step = SOFT_EDGE / BANDS ; 
		float from = Math.max(0, sinkLine() + band * step) ; 
		float to = band == BANDS ? 1 : Math.min(1, sinkLine() + (band + 1) * step) ; 
		if(to <= from)
			return false ; 
		batch.flush() ; 
		Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST) ; 
		int y0 = Math.round(getY() + from * height), y1 = Math.round(getY() + to * height) ; 
		HdpiUtils.glScissor(0, y0, Gdx.graphics.getWidth(), Math.max(0, y1 - y0)) ; 
		return true ; 
	}
	
	private void unclip(Batch batch)
	{
		if(sunk <= 0)
			return ; 
		batch.flush() ; 
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST) ; 
	}
	
	/** The cloud and its text, dissolved from the bottom up as far as {@link #sunk} says. */
	@Override
	public void draw(Batch batch, float parentAlpha)
	{
		if(sunk <= 0)
		{
			super.draw(batch, parentAlpha) ; 
			return ; 
		}
		if(sunk >= 1)
			return ; 
		for(int band = 0 ; band <= BANDS ; band++)
			if(clipBand(batch, band))
				super.draw(batch, parentAlpha * bandShare(band)) ; 
		unclip(batch) ; 
	}
	
	public void reverse(boolean reverse) 
	{
		reversed = reverse ; 
		if(tailApart)
		{
			bubbleBackground.setSize(width * (reverse ? -1 : 1), height * TAIL_TOP);
			bubbleBackground.setPosition(0, height * (1 - TAIL_TOP));
		}
		else
		{
			bubbleBackground.setSize(width * (reverse ? -1 : 1),height);
			bubbleBackground.setPosition(0, 0);
		}
		// Mirrored, the cloud's body is mirrored too: its right margin is on the left.
		if(reverse)
			typing.setPosition(width * (1 - BODY_RIGHT) - width, height * (1 - BODY_BOTTOM));
		else
			typing.setPosition(width * BODY_LEFT, height * (1 - BODY_BOTTOM));
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
				sunk = 0 ; 
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
	
	
	/** Whether the cloud is mirrored, its tail at its lower right, as when Ross faces left. */
	public boolean isReversed()
	{
		return reversed ; 
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
	/**
	 * Bigger at r163 (6.5 to 5.8), with letters to match: Index_Fonts.BUBBLE_LARGE_TEXT_MEDIUM.
	 * Back to 6.5 at r249, letters unchanged: Simon found the square cloud too big. A line wraps
	 * narrower, and the few that then need five rows grow the cloud by fitCloud's finer steps.
	 */
	private static final float devisingLarge = 6.5f ; 
	
	/**
	 * Where the text goes in bubble_think_2.png (r163), as fractions of it from its top-left: the
	 * round body of the cloud, clear of its pointed top and of the tail. The text box used to be
	 * nearly the whole square, and a line of five rows ran into the rim at both ends. Between
	 * rows 230 and 780 of the 1200, the cloud is white from at most 0.12 to at least 0.85 across.
	 */
	static final float BODY_TOP = 230f / 1200f, BODY_BOTTOM = 780f / 1200f ; 
	static final float BODY_LEFT = 0.14f, BODY_RIGHT = 0.84f ; 
	
	/**
	 * A step of {@link #fitCloud()}, and the most it grows: a cloud a third bigger at worst. 1.05,
	 * not 1.15 (r249): the smaller cloud's five-row lines need a tenth more, and a 1.15 step made
	 * them bigger than the cloud they replaced.
	 */
	static final float GROWTH = 1.05f, MAX_GROWTH = 1.33f ; 
	/** How much bigger than its usual size the cloud is for the line it holds now. */
	float scale = 1f ; 
	
	/**
	 * The cloud's side, in base sizes. r121 stretched it 1.6 wide and 1.2 tall so the longer
	 * lines went into the width; the painting is square, and it is drawn square now (d14, D3).
	 * 1.4, not 1.6: there is little room above Ross, and setBubblePosition slides a taller cloud
	 * down to keep it on the screen - at 1.6 its tail pointed at the post box beside him, not
	 * at him. A line wraps a little narrower, and fitCloud grows the cloud for the longest.
	 */
	private static final float SIDE = 1.4f ; 
	
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
		
		baseSize = size ; 
		if(textWhenVisible != null && !textWhenVisible.isEmpty())
			fitCloud() ; 
		else
			layoutCloud() ; 
	}
	
	/** The size resize() works out for the window, before fitCloud() grows it for a line. */
	float baseSize ; 
	
	/** The cloud at baseSize times scale, the text in its body (BODY_*). */
	private void layoutCloud()
	{
		float size = baseSize * scale ; 
		width = size * SIDE ; 
		height = size * SIDE ; 
		
		this.setSize(width,height);
		typing.setSize(width * (BODY_RIGHT - BODY_LEFT), height * (BODY_BOTTOM - BODY_TOP));
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