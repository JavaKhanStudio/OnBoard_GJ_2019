package jks.vinterface.tools;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import jks.vinterface.Utils_Board;

/**
 * A check box drawn with the pause screen's painted boxes, words first and the box at the
 * end of the row, as "Couper le son" is on the pause panel. The whole row is the button, so
 * a click on the words ticks it too.
 */
public class PaintedCheckBox extends CheckBox
{
	/** cocheVide.png and cocheOk.png are 74x62. */
	private static final float BOX_ASPECT = 74f / 62f ;

	private final Cell<Image> boxCell ;

	public PaintedCheckBox(String text, BitmapFont font, Color fontColor)
	{
		super(text, style(font, fontColor)) ;

		// CheckBox puts its box first. Put it last, and let the words take the room.
		clearChildren() ;
		add(getLabel()).expandX().fillX() ;
		getLabel().setAlignment(Align.left) ;
		// A CheckBox image does not scale by default: the painted box would stay 74x62
		// whatever size its cell is given.
		getImage().setScaling(Scaling.fit) ;
		boxCell = add(getImage()) ;
	}

	private static CheckBoxStyle style(BitmapFont font, Color fontColor)
	{
		CheckBoxStyle style = new CheckBoxStyle() ;
		style.checkboxOff = Utils_Board.boxEmpty() ;
		style.checkboxOn = Utils_Board.boxTicked() ;
		style.font = font ;
		style.fontColor = fontColor ;
		return style ;
	}

	/** After GVars_Font has rebuilt its fonts for a new window width. */
	public void setFont(BitmapFont font)
	{
		getStyle().font = font ;
		setStyle(getStyle()) ;
	}

	/** The painted box at this height, keeping its shape. */
	public void setBoxHeight(float height)
	{
		boxCell.size(height * BOX_ASPECT, height).padLeft(height / 2f) ;
		invalidateHierarchy() ;
	}
}
