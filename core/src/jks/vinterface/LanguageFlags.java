package jks.vinterface;

import java.util.LinkedHashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import jks.amain.Utils_Config;
import jks.index.Index_Interface;
import jks.index.Index_Text;

/**
 * The start menu's language flags (r162), in its bottom-right corner: one flag a column of
 * i18n/textes.tsv that has one, the language in force at full strength and the others dimmed.
 * A click reads the table in that language from then on, keeps it in the config for the next
 * start, and hands back to the menu to letter itself again.
 *
 * Only here: the game's text is fetched as each screen is built, so the menu is the one place
 * where words already on screen have to change. The flags are drawn by tools/make_flags.py.
 */
public class LanguageFlags extends Table
{
	/** Flags drawn for these codes, in this order; a code the table has no column for is left out. */
	public static final String[] CODES = {"fr", "en"} ;
	/** A flag's height, as a part of the window's; its width is 3:2. */
	private static final float HEIGHT = 1 / 22f ;
	private static final float DIMMED = 0.45f ;

	private final Runnable changed ;
	/** The flags, by language code, in CODES order. */
	private final Map<String, ImageButton> flags = new LinkedHashMap<>() ;

	public LanguageFlags(Runnable changed)
	{
		this.changed = changed ;
		setName("languageFlags") ;
		for(String code : CODES)
		{
			if(!Index_Text.languages().contains(code))
				continue ;
			ImageButton flag = new ImageButton(Utils_TexturesAcess.buildDrawingRegionTexture(flagPath(code))) ;
			flag.setName("flag." + code) ;
			flag.addListener(new ClickListener()
			{
				@Override
				public void clicked(InputEvent event, float x, float y)
				{choose(code) ;}
			}) ;
			flags.put(code, flag) ;
		}
		resize() ;
	}

	public static String flagPath(String code)
	{
		return Index_Interface.flags + code + ".png" ;
	}

	/** What a click on a flag does. Public so a test can make the same choice. */
	public void choose(String code)
	{
		if(code.equals(Index_Text.getLanguage()))
			return ;
		Index_Text.setLanguage(code) ;
		Utils_Config.current.language = code ;
		Utils_Config.save() ;
		shade() ;
		changed.run() ;
	}

	private void shade()
	{
		for(Map.Entry<String, ImageButton> flag : flags.entrySet())
			flag.getValue().getColor().a = flag.getKey().equals(Index_Text.getLanguage()) ? 1f : DIMMED ;
	}

	/** Sized and placed from the window, like everything on the stage; again from the screen's resize(). */
	public void resize()
	{
		float h = Gdx.graphics.getHeight() * HEIGHT, w = h * 1.5f, gap = h / 3 ;
		clearChildren() ;
		for(ImageButton flag : flags.values())
			add(flag).size(w, h).padLeft(gap) ;
		pack() ;
		setPosition(Gdx.graphics.getWidth() - getWidth() - gap * 2, gap * 2) ;
		shade() ;
	}
}
