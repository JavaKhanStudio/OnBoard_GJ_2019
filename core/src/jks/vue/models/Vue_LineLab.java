package jks.vue.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;

import jks.index.Index_Interface;
import jks.index.Index_Text;
import jks.vars.GVars_Serialization;
import jks.vinterface.GVars_UI;
import jks.vinterface.Utils_TexturesAcess;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.tools.DialogBubble;
import jks.vinterface.tools.DialogBubble.DialogSize;
import jks.vue.AVue_Model;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;
import jks.vue.models.game.WagonLevel;

/**
 * The line lab (r74): every item of every carriage with what Ross says about it, editable in
 * place. A row per item holds the line said when it is clicked; an item something can be used
 * on has a row more per use - the message_Crucial lines (r81). Say types the line in the
 * game's own bubble; Save writes it into the text table.
 *
 * A development screen, not part of the game. Nothing leads here; open it with
 *   ./gradlew :desktop:runGame -Donboard.start=line_lab
 *
 * It writes desktop/assets/i18n/textes.tsv, the table in the source tree, one cell at a time
 * (Index_Text.write) - every comment and every other row stays as it was. A line break in a
 * field is a \n in the table. Reload reads the file again, for an edit made by hand.
 * Lab chrome is English, like every lab; only the lines are the game's French.
 */
public class Vue_LineLab extends AVue_Model
{
	/** Narrow enough to leave the right-hand column to the bubble, which is wider since r121. */
	private static final float ICON = 44f, NAME_WIDTH = 210f, FIELD_WIDTH = 500f ;
	private final GlyphLayout measure = new GlyphLayout() ;

	/** Null on a platform with no source tree (r135): the lab then only reads. */
	private final FileHandle table = Index_Text.sourceTable() ;
	private final Map<String, Texture> icons = new HashMap<>() ;
	private final List<VisTable> carriages = new ArrayList<>() ;
	private final List<VisTextButton> tabs = new ArrayList<>() ;
	private Cell<VisTable> shown ;
	private VisLabel status ;
	private DialogBubble bubble ;
	private TextFieldStyle fieldStyle ;

	@Override
	public void init()
	{
		toRender = new ArrayList<>() ;
		Gdx.input.setInputProcessor(GVars_UI.mainUi) ;

		// The stock skin's field font is ASCII: it would leave a hole for every accent.
		fieldStyle = new TextFieldStyle(VisUI.getSkin().get(TextFieldStyle.class)) ;
		fieldStyle.font = GVars_Font.labelStyle_Second.font ;
		fieldStyle.fontColor = Color.WHITE ;

		VisTable page = new VisTable() ;
		page.setFillParent(true) ;
		page.align(Align.topLeft) ;
		page.pad(24f, 48f, 20f, 48f) ;
		page.setBackground(Utils_TexturesAcess.buildDrawingRegionTexture(Index_Interface.frame_Gray)) ;

		page.add(label("Line lab", GVars_Font.labelStyle_ScreenTitle)).left().row() ;
		page.add(label("Say types a line in the game's bubble. Save writes it into desktop/assets/i18n/textes.tsv.",
			GVars_Font.labelStyle_Second)).left().padBottom(10f).row() ;

		VisTable tabRow = new VisTable() ;
		for(int n = 1 ; n <= GVars_Game.LEVEL_COUNT ; n++)
		{
			final int carriage = n ;
			VisTextButton tab = new VisTextButton("Carriage " + n, "toggle") ;
			tab.addListener(new ChangeListener()
			{
				@Override
				public void changed(ChangeEvent event, Actor actor)
				{
					if(((VisTextButton) actor).isChecked())
						showCarriage(carriage) ;
				}
			}) ;
			tabs.add(tab) ;
			tabRow.add(tab).padRight(8f) ;
			carriages.add(buildCarriage(n)) ;
		}
		VisTextButton reload = new VisTextButton("Reload from file") ;
		reload.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{reload() ;}
		}) ;
		tabRow.add(reload).padLeft(24f) ;
		page.add(tabRow).left().padBottom(8f).row() ;

		shown = page.add((VisTable) null).left().top() ;
		page.row() ;
		status = label("", GVars_Font.labelStyle_Second) ;
		page.add(status).left().padTop(10f).row() ;

		GVars_UI.mainUi.addActor(page) ;

		bubble = new DialogBubble(DialogSize.BUBBLE_LARGE_TEXT_MEDIUM) ;
		bubble.reverse(true) ;
		GVars_UI.mainUi.addActor(bubble) ;
		placeBubble() ;

		showCarriage(startCarriage()) ;
	}

	/** -Donboard.level opens that carriage's tab, as it opens that carriage in the game. */
	private static int startCarriage()
	{
		int level = Integer.getInteger("onboard.level", 1) ;
		return level >= 1 && level <= GVars_Game.LEVEL_COUNT ? level : 1 ;
	}

	private VisTable buildCarriage(int n)
	{
		VisTable rows = new VisTable() ;
		WagonLevel level = read(n) ;
		for(GameItem item : level.listItems)
		{
			addRow(rows, icon(item), item.name, GameItem.clickKey(n, item.name)) ;
			if(item.message_Crucial_1 != null)
				addRow(rows, null, "  + " + item.name_Interaction_1, item.message_Crucial_1) ;
			if(item.message_Crucial_2 != null)
				addRow(rows, null, "  + " + item.name_Interaction_2, item.message_Crucial_2) ;
		}
		return rows ;
	}

	private void addRow(VisTable rows, Texture icon, String what, final String key)
	{
		// Enter is a line break here, as \n is in the table: Save is the button.
		final TextArea field = new TextArea(Index_Text.has(key) ? Index_Text.get(key) : "", fieldStyle) ;
		field.setMessageText("no row " + key) ;

		VisTextButton say = new VisTextButton("Say") ;
		say.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{say(field.getText()) ;}
		}) ;

		VisTextButton save = new VisTextButton("Save") ;
		save.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{save(key, field) ;}
		}) ;

		if(icon == null)
			rows.add().size(ICON) ;
		else
		{
			VisImage image = new VisImage(icon) ;
			image.setScaling(Scaling.fit) ;
			rows.add(image).size(ICON).padRight(10f) ;
		}
		rows.add(label(what, GVars_Font.labelStyle_Second)).left().width(NAME_WIDTH) ;
		rows.add(field).width(FIELD_WIDTH).height(linesFor(field.getText()) * fieldStyle.font.getLineHeight() + 6f).padRight(8f) ;
		rows.add(say).padRight(8f) ;
		rows.add(save) ;
		rows.row().padTop(3f) ;
	}

	/** As many lines as the text wraps to in the field, up to three: carriage 1 must fit at 720. */
	private int linesFor(String text)
	{
		measure.setText(fieldStyle.font, text, Color.WHITE, FIELD_WIDTH - 12f, Align.left, true) ;
		int lines = Math.round(measure.height / fieldStyle.font.getLineHeight()) ;
		return Math.max(1, Math.min(lines, 3)) ;
	}

	/** Types a line in the lab's bubble, as Ross would say it. */
	public void say(String text)
	{
		bubble.showForReading(text) ;
	}

	private void save(String key, TextArea field)
	{
		String text = field.getText() ;
		if(table == null)
		{
			status.setText("No text table to write into here") ;
			return ;
		}
		try
		{
			Index_Text.write(table, key, Index_Text.getLanguage(), text) ;
			status.setText("Saved " + key) ;
			bubble.showForReading(text) ;
		}
		catch(GdxRuntimeException e)
		{
			status.setText(e.getMessage()) ;
		}
	}

	private void reload()
	{
		if(table == null)
		{
			status.setText("No text table to read from here") ;
			return ;
		}
		try
		{
			Index_Text.reloadFrom(table) ;
		}
		catch(GdxRuntimeException e)
		{
			status.setText(e.getMessage()) ;
			return ;
		}
		int showing = 1 ;
		for(int i = 0 ; i < tabs.size() ; i++)
			if(tabs.get(i).isChecked())
				showing = i + 1 ;
		for(int n = 1 ; n <= carriages.size() ; n++)
			carriages.set(n - 1, buildCarriage(n)) ;
		showCarriage(showing) ;
		status.setText("Read " + table.path() + " again") ;
	}

	/** Fills the list with one carriage's rows, and checks only that carriage's tab. */
	public void showCarriage(int n)
	{
		for(int i = 0 ; i < tabs.size() ; i++)
		{
			VisTextButton tab = tabs.get(i) ;
			tab.setProgrammaticChangeEvents(false) ;
			tab.setChecked(i + 1 == n) ;
			tab.setProgrammaticChangeEvents(true) ;
		}
		shown.setActor(carriages.get(n - 1)) ;
	}

	/** Read straight from the .wa, not through GVars_Game: nothing of the game is started. */
	private static WagonLevel read(int n)
	{
		String path = "game/wagon/wa" + n + ".wa" ;
		try
		{
			return GVars_Serialization.objectMapper.readValue(Gdx.files.internal(path).read(), WagonLevel.class) ;
		}
		catch(Exception e)
		{
			throw new GdxRuntimeException("Could not read " + path, e) ;
		}
	}

	/** Loaded once per file, so Reload does not load them all again. */
	private Texture icon(GameItem item)
	{
		String path = "game/wagon/" + item.path + "/" + item.path_EtatDebut ;
		Texture texture = icons.get(path) ;
		if(texture == null)
		{
			texture = new Texture(Gdx.files.internal(path), true) ;
			texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear) ;
			icons.put(path, texture) ;
		}
		return texture ;
	}

	private void placeBubble()
	{
		// In the right-hand column, which the rows leave free, drawn leftwards from here.
		bubble.setPosition(Gdx.graphics.getWidth() - 40f, Gdx.graphics.getHeight() * 0.35f) ;
	}

	private static VisLabel label(String text, LabelStyle style)
	{return new VisLabel(text, style) ;}

	@Override
	public void destroy()
	{
		for(Texture icon : icons.values())
			icon.dispose() ;
		icons.clear() ;
	}

	@Override
	public void restart()
	{}

	@Override
	public void update(float delta)
	{
		GVars_UI.mainUi.act(delta) ;
		placeBubble() ;
	}

	@Override
	public void render()
	{
		Gdx.gl.glClearColor(0.12f, 0.12f, 0.12f, 1) ;
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT) ;
		renderBeforeInterface() ;
		drawInterface() ;
	}
}
