package jks.verify;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeBitmapFontData;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.GdxNativesLoader;

import jks.index.Index_Text;
import jks.vinterface.font.GVars_Font;

/**
 * Every letter the text table writes can be drawn (r82). FreeType does not throw on a missing
 * glyph, it leaves a hole: "s’il" drew as "sil" in the cage's line, because the curly apostrophe
 * was neither asked of the generator nor noticed. No GL: FreeType reads the .ttf directly.
 *
 * The rows marked [title face] are OptimusPrinceps and ASCII only (TextTableTest, d8). Every
 * other row is drawn in Mansalva (the bubbles) or GeosansLight (the menus and the ending), so
 * each of its characters must be in both faces and in the set the generator is asked for - or be
 * a no-break space, which GVars_Font draws as the face's space where the face has none (r221).
 */
class TextGlyphTest
{
	private static final String[] FACES = {"ui/fonts/Mansalva-Regular.ttf", "ui/fonts/GeosansLight.ttf"} ;

	@Test
	@DisplayName("every character of the table is rasterised and exists in the faces that draw it")
	void everyCharacterHasAGlyph()
	{
		TreeSet<Integer> used = new TreeSet<>() ;
		for (Map<String, String> row : Index_Text.rows().values())
			for (String text : row.values())
				text.codePoints().filter(c -> c > 127 && GVars_Font.NO_BREAK_SPACES.indexOf(c) < 0).forEach(used::add) ;
		assertTrue(used.contains((int) '’'), "the table no longer has a curly apostrophe - is this still testing anything?") ;

		List<String> missing = new ArrayList<>() ;
		for (int c : used)
			if (GVars_Font.CHARACTERS.indexOf(c) < 0)
				missing.add(name(c) + " not in GVars_Font.CHARACTERS") ;

		FreeType.Library library = FreeType.initFreeType() ;
		try
		{
			for (String path : FACES)
			{
				FreeType.Face face = library.newFace(new FileHandle(new File(Assets.DIR, path)), 0) ;
				try
				{
					for (int c : used)
						if (face.getCharIndex(c) == 0)
							missing.add(name(c) + " has no glyph in " + path) ;
				}
				finally
				{
					face.dispose() ;
				}
			}
		}
		finally
		{
			library.dispose() ;
		}

		assertTrue(missing.isEmpty(), "these would draw as holes: " + missing) ;
	}

	/** The extra punctuation is asked for, and every face that draws the table has it. */
	@Test
	@DisplayName("the added punctuation exists in both text faces")
	void addedPunctuationExists()
	{
		String added = GVars_Font.CHARACTERS.substring(com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.DEFAULT_CHARS.length()) ;
		List<String> missing = new ArrayList<>() ;
		FreeType.Library library = FreeType.initFreeType() ;
		try
		{
			for (String path : FACES)
			{
				FreeType.Face face = library.newFace(new FileHandle(new File(Assets.DIR, path)), 0) ;
				try
				{
					added.codePoints().filter(c -> face.getCharIndex(c) == 0).forEach(c -> missing.add(name(c) + " in " + path)) ;
				}
				finally
				{
					face.dispose() ;
				}
			}
		}
		finally
		{
			library.dispose() ;
		}
		assertTrue(missing.isEmpty(), "asked for but not in the face: " + missing) ;
	}

	/** "déjà ?" (r221): a no-break space has the space's width in every face, never a hole. */
	@Test
	@DisplayName("every text face draws a no-break space as wide as its space")
	void noBreakSpacesAreSpaces()
	{
		GdxNativesLoader.load() ;
		List<String> wrong = new ArrayList<>() ;
		for (String path : FACES)
		{
			FreeTypeFontGenerator generator = new FreeTypeFontGenerator(new FileHandle(new File(Assets.DIR, path))) ;
			PixmapPacker packer = new PixmapPacker(512, 512, Pixmap.Format.RGBA8888, 1, false) ;
			try
			{
				FreeTypeFontParameter parameter = new FreeTypeFontParameter() ;
				parameter.characters = GVars_Font.CHARACTERS ;
				parameter.size = 24 ;
				parameter.packer = packer ;   // its own packer: pixmaps only, no texture, no GL
				FreeTypeBitmapFontData data = generator.generateData(parameter) ;
				GVars_Font.drawNoBreakSpaces(data) ;
				int space = data.getGlyph(' ').xadvance ;
				for (char c : GVars_Font.NO_BREAK_SPACES.toCharArray())
				{
					BitmapFont.Glyph glyph = data.getGlyph(c) ;
					if (glyph == null)
						wrong.add(name(c) + " has no glyph in " + path) ;
					else if (glyph.xadvance < space / 2)
						wrong.add(name(c) + " is " + glyph.xadvance + "px in " + path + ", its space " + space + "px") ;
				}
			}
			finally
			{
				packer.dispose() ;
				generator.dispose() ;
			}
		}
		assertTrue(wrong.isEmpty(), "would close up 'déjà ?': " + wrong) ;
	}

	private static String name(int c)
	{
		return String.format("'%s' (U+%04X)", new String(Character.toChars(c)), c) ;
	}
}
