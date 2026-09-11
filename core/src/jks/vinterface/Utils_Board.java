package jks.vinterface;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import jks.index.Index_Interface;
import jks.vinterface.font.GVars_Font;

/**
 * The look of the options screen: each section is a painted sign board, the one the pause
 * screen hangs, with its title written on the plank.
 *
 * Words painted on the wood are in {@link #INK}, the brown PAUSE is lettered in. Anything you
 * can press is a dark wooden tag with cream writing, so it reads as something other than the
 * board it sits on.
 *
 * Every style here is a new object. TextButton.draw() writes its font colour into its label's
 * style each frame, so a button handed a shared GVars_Font style would recolour every other
 * label drawn with it.
 */
public class Utils_Board
{
	public static final Color INK = new Color(0.24f, 0.16f, 0.06f, 1f) ;
	public static final Color CREAM = new Color(0.97f, 0.90f, 0.76f, 1f) ;
	public static final Color TAG = new Color(0.25f, 0.15f, 0.07f, 0.92f) ;
	public static final Color TAG_OVER = new Color(0.36f, 0.22f, 0.10f, 0.95f) ;
	public static final Color TAG_DOWN = new Color(0.16f, 0.09f, 0.04f, 0.95f) ;
	public static final Color AMBER = new Color(0.78f, 0.55f, 0.22f, 1f) ;

	/** panneauVide.png is 1095x1009, like the pause panel it was made from. */
	public static final float BOARD_WIDTH = 1095f, BOARD_HEIGHT = 1009f ;
	/** The plank, measured off the image: from its top edge to where the board shows below it. */
	private static final float PLANK_TOP = 40f, PLANK_BOTTOM = 215f ;
	/** Clear of the vines down both sides and the moss along the bottom. */
	private static final float SIDE = 170f, BOTTOM = 150f, BELOW_PLANK = 30f ;

	public static Drawable board()
	{
		return Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.board_Blank) ;
	}

	/** The board's scale for a given width: its own pixels to the screen's. */
	public static float scale(float boardWidth)
	{
		return boardWidth / BOARD_WIDTH ;
	}

	public static float plankHeight(float scale)
	{
		return (PLANK_BOTTOM - PLANK_TOP) * scale ;
	}

	/**
	 * The padding that keeps a board's content on the wood: the title row fills the plank,
	 * the rest sits inside the framed part of the board.
	 */
	public static void pad(com.badlogic.gdx.scenes.scene2d.ui.Table board, float scale)
	{
		board.pad(PLANK_TOP * scale, SIDE * scale, BOTTOM * scale, SIDE * scale) ;
	}

	public static float gapBelowPlank(float scale)
	{
		return BELOW_PLANK * scale ;
	}

	/** Painted on the board. A copy, so tinting it cannot reach any other label. */
	public static LabelStyle ink(LabelStyle font)
	{
		return new LabelStyle(font.font, INK) ;
	}

	private static Drawable tint(String name, Color colour)
	{
		return GVars_UI.baseSkin.newDrawable(name, colour) ;
	}

	public static TextButtonStyle tagButton()
	{
		TextButtonStyle style = new TextButtonStyle() ;
		style.up = tint("white", TAG) ;
		style.over = tint("white", TAG_OVER) ;
		style.down = tint("white", TAG_DOWN) ;
		style.checked = style.up ;   // Enter toggles a button to press it; do not leave it looking held
		style.font = GVars_Font.font_Title ;
		style.fontColor = CREAM ;
		style.overFontColor = CREAM ;
		style.downFontColor = AMBER ;
		return style ;
	}

	public static SelectBoxStyle selectBox()
	{
		SelectBoxStyle base = GVars_UI.baseSkin.get(SelectBoxStyle.class) ;

		ListStyle list = new ListStyle() ;
		list.font = GVars_Font.fontont_SelectBox ;
		list.fontColorSelected = TAG_DOWN ;
		list.fontColorUnselected = CREAM ;
		list.selection = padded(tint("white", AMBER)) ;
		list.background = padded(tint("white", TAG)) ;

		SelectBoxStyle style = new SelectBoxStyle(base) ;
		style.font = GVars_Font.fontont_SelectBox ;
		style.fontColor = CREAM ;
		style.disabledFontColor = new Color(CREAM.r, CREAM.g, CREAM.b, 0.45f) ;
		style.background = tint("default-select", TAG) ;
		style.backgroundOver = tint("default-select", TAG_OVER) ;
		style.backgroundOpen = tint("default-select", TAG_DOWN) ;
		style.backgroundDisabled = tint("default-select", new Color(TAG.r, TAG.g, TAG.b, 0.5f)) ;
		style.listStyle = list ;
		style.scrollStyle = new ScrollPaneStyle() ;
		return style ;
	}

	/** Room either side of the words in the drop-down list. */
	private static Drawable padded(Drawable drawable)
	{
		drawable.setLeftWidth(8f) ;
		drawable.setRightWidth(8f) ;
		drawable.setTopHeight(2f) ;
		drawable.setBottomHeight(2f) ;
		return drawable ;
	}

	/** A dark groove with a cream knob, sized for the row it sits in. */
	public static SliderStyle slider(float rowHeight)
	{
		SliderStyle style = new SliderStyle() ;
		BaseDrawable groove = (BaseDrawable)tint("white", TAG) ;
		groove.setMinHeight(Math.max(4f, rowHeight / 8f)) ;
		style.background = groove ;
		BaseDrawable knob = (BaseDrawable)tint("white", CREAM) ;
		knob.setMinWidth(Math.max(8f, rowHeight / 3f)) ;
		knob.setMinHeight(Math.max(16f, rowHeight * 0.7f)) ;
		style.knob = knob ;
		BaseDrawable knobOver = (BaseDrawable)tint("white", AMBER) ;
		knobOver.setMinWidth(knob.getMinWidth()) ;
		knobOver.setMinHeight(knob.getMinHeight()) ;
		style.knobOver = knobOver ;
		style.knobDown = knobOver ;
		return style ;
	}

	public static TextureRegionDrawable boxEmpty()
	{
		return Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.pause_Box_Empty) ;
	}

	public static TextureRegionDrawable boxTicked()
	{
		return Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.pause_Box_Ticked) ;
	}
}
