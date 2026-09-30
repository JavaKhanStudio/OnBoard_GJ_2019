package jks.vinterface.overlay;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

import jks.index.Index_Credits;
import jks.index.Index_Interface;
import jks.tools.Vector2Int;
import jks.vinterface.GVars_UI;
import jks.vinterface.Utils_TexturesAcess;
import jks.vinterface.font.GVars_Font;

/**
 * The credits screen.
 *
 * The button for this has been on the start menu since 2019 with nothing behind it - its
 * listener logged "Must be implement" and returned. The names were never written down
 * anywhere in the project, so this could not be finished until someone supplied them.
 *
 * Content lives in {@link Index_Credits}; this class only arranges it.
 */
public class OverlayCredits extends OverlayModel
{

	private final ImageButton retour ;
	private final VisTable mainTable ;
	private final ReplayAction backway ;

	public OverlayCredits(ReplayAction ref)
	{
		super(GVars_UI.baseSkin) ;
		backway = ref ;
		this.setLayoutEnabled(false) ;

		retour = buildReturnSign(backway) ;

		mainTable = buildContent() ;
		mainTable.setTouchable(Touchable.childrenOnly) ;
		mainTable.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.frame_Gray)) ;

		resize() ;

		this.addActor(mainTable) ;
		this.addActor(retour) ;
	}

	private VisTable buildContent()
	{
		VisTable table = new VisTable() ;
		table.align(Align.top) ;
		table.pad(24f) ;

		table.add(label(Index_Credits.title(), GVars_Font.labelStyle_ScreenTitle)).padBottom(6f).row() ;
		table.add(label(Index_Credits.TEAM, GVars_Font.labelStyle_OptionsTitle)).padBottom(20f).row() ;

		for(Index_Credits.Section section : Index_Credits.sections())
		{
			table.add(label(section.role, GVars_Font.labelStyle_OptionsTitle)).padTop(10f).padBottom(4f).row() ;

			for(String person : section.people)
				table.add(label(person, GVars_Font.labelStyle_Second)).padBottom(2f).row() ;
		}

		for(String line : Index_Credits.footer())
			table.add(label(line, GVars_Font.labelStyle_Second)).padTop(14f).row() ;

		return table ;
	}

	private VisLabel label(String text, com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle style)
	{
		VisLabel label = new VisLabel(text, style) ;
		label.setAlignment(Align.center) ;
		return label ;
	}

	@Override
	public void resize()
	{
		float sizebuttonX = Gdx.graphics.getWidth() / 7.5f ;
		float decalSideX = Gdx.graphics.getWidth() / 23f ;
		float width = Gdx.graphics.getWidth() - (decalSideX * 2) - sizebuttonX * 1.5f ;

		// Size to the content rather than to a fixed fraction of the screen. A fixed height
		// silently clipped the last line off the bottom, which for a credits screen means
		// dropping someone's name.
		mainTable.setWidth(width) ;
		mainTable.pack() ;
		float height = Math.min(mainTable.getPrefHeight(), Gdx.graphics.getHeight() * 0.92f) ;

		mainTable.setWidth(width) ;
		mainTable.setHeight(height) ;
		mainTable.setPosition((Gdx.graphics.getWidth() - width)/2, (Gdx.graphics.getHeight() - height)/2) ;
		mainTable.invalidate() ;
		mainTable.validate() ;

		// The return sign is a full-size painted asset. An ImageButton left to its own
		// devices draws it at its natural size, which is most of the screen.
		float buttonWidth = sizebuttonX ;
		float buttonHeight = buttonWidth / Index_Interface.button_Return_Aspect ;
		retour.setSize(buttonWidth, buttonHeight) ;
		retour.getImageCell().size(buttonWidth, buttonHeight) ;
		retour.setPosition(decalSideX / 2f, (Gdx.graphics.getHeight() - buttonHeight) / 2f) ;
	}

	@Override
	public ArrayList<ArrayList<Actor>> mapInterface()
	{
		ArrayList<ArrayList<Actor>> rows = new ArrayList<ArrayList<Actor>>() ;
		ArrayList<Actor> row = new ArrayList<Actor>() ;
		row.add(retour) ;
		rows.add(row) ;
		return rows ;
	}

	@Override
	public Vector2Int startAt()
	{return new Vector2Int(0,0) ;}

	@Override
	public Button getBackButton()
	{return retour ;}

}
