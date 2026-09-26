package jks.html;

import com.google.gwt.user.client.Window;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.freetype.gwt.FreetypeInjector;
import com.badlogic.gdx.graphics.g2d.freetype.gwt.inject.OnCompletion;

import jks.amain.GameConfigs;
import jks.amain.Main_Application;
import jks.amain.Utils_Config;
import jks.sounds.GVars_Audio;
import jks.sounds.GVars_AudioManager;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.font.Index_Fonts.Enum_Fonts;
import jks.vars.GVars_Heart;

/**
 * The browser's entry point (r137), standing where desktop/src/jks/launcher/Launcher_Game does.
 *
 * FreeType is a JavaScript file here (freetype.js, the gdx-freetype-gwt port), which has to be
 * loaded before libGDX starts: onModuleLoad waits for it.
 *
 * The desktop reads its config before the window opens; a tab has no disk, and localStorage is
 * reached through Gdx.app, so the config is read in create(), before the game's own. ?mute
 * silences the tab whatever the config says (the gate runs with it). ?start=game&level=3 stands for
 * -Donboard.start and -Donboard.level.
 *
 * It also sets window.onboard, what tools/browser-gate/gate.mjs reads: frames(), vue(), music(), volume()
 * and missingGlyphs(text).
 */
public class HtmlLauncher extends GwtApplication
{
	@Override
	public GwtApplicationConfiguration getConfig()
	{
		// 1280x720, the desktop window's default: the menus and the carriages are laid out for 16:9
		return new GwtApplicationConfiguration(1280, 720);
	}

	@Override
	public void onModuleLoad()
	{
		FreetypeInjector.inject(new OnCompletion()
		{
			@Override
			public void run()
			{
				HtmlLauncher.super.onModuleLoad();
			}
		});
	}

	@Override
	public ApplicationListener createApplicationListener()
	{
		HtmlPlatform.install();
		final boolean mute = Window.Location.getParameter("mute") != null;
		// -Donboard.start and -Donboard.level, as ?start=game&level=3: the gate opens a carriage this way
		String start = Window.Location.getParameter("start");
		if(start != null)
			Main_Application.startPoint = Main_Application.StartPoint.valueOf(start.trim().toUpperCase());
		if(Window.Location.getParameter("level") != null)
			Main_Application.startLevel = Main_Application.levelFrom(Window.Location.getParameter("level"));
		exportProbe();
		return new Main_Application()
		{
			@Override
			public void create()
			{
				// What desktop's Utils_Launcher.loadConfig does, less the window: a page sets its own size
				GameConfigs config = Utils_Config.load();
				GVars_Audio.masterVolume = config.volume;
				GVars_Audio.muted = mute || config.volume <= 0f;
				GVars_Audio.effectVolume = Math.max(0f, Math.min(1f, config.effectsVolume));
				GVars_Audio.loadChoices(config);
				super.create();
			}
		};
	}

	/** Frames rendered so far: a page that is really running counts up at about 60 a second. */
	static int frames()
	{return Gdx.graphics == null ? 0 : (int) Gdx.graphics.getFrameId();}

	/** The view on screen, by its simple name: Vue_Preloading, Vue_Scenematic_Intro, Vue_StartScreen... */
	static String vue()
	{
		if(GVars_Heart.vue == null)
			return "";
		String name = GVars_Heart.vue.getClass().getName();
		return name.substring(name.lastIndexOf('.') + 1);
	}

	/** The track coming out of the speakers, or "" when none is. */
	static String music()
	{return GVars_AudioManager.isMusicPlaying() ? String.valueOf(GVars_AudioManager.currentMusic()) : "";}

	/** The music volume the config gave the game: the gate edits localStorage and reloads to see it read back. */
	static float volume()
	{return Utils_Config.current.volume;}

	/**
	 * The characters of text that a face drawn in the page has no glyph for, as "face: chars" lines,
	 * or "" when both have them all: GeosansLight (menus, ending) and Mansalva (Ross's bubbles).
	 * TextGlyphTest checks the .ttf files; this checks what FreeType made of them in the browser.
	 */
	static String missingGlyphs(String text)
	{
		String missing = missingIn("GeosansLight", GVars_Font.font_Second, text)
			+ missingIn("Mansalva", GVars_Font.buildLabel(Enum_Fonts.BUBBLE_MEDIUM_TEXT_MEDIUM).font, text);
		return missing.trim();
	}

	private static String missingIn(String face, BitmapFont font, String text)
	{
		if(font == null)
			return face + ": not built yet\n";
		String holes = "";
		for(int i = 0; i < text.length(); i++)
		{
			Glyph glyph = font.getData().getGlyph(text.charAt(i));
			if(glyph == null || glyph.width == 0)
				holes += text.charAt(i);
		}
		return holes.isEmpty() ? "" : face + ": " + holes + "\n";
	}

	private static native void exportProbe()
	/*-{
		$wnd.onboard = {
			frames: $entry(function() { return @jks.html.HtmlLauncher::frames()(); }),
			vue: $entry(function() { return @jks.html.HtmlLauncher::vue()(); }),
			music: $entry(function() { return @jks.html.HtmlLauncher::music()(); }),
			volume: $entry(function() { return @jks.html.HtmlLauncher::volume()(); }),
			missingGlyphs: $entry(function(text) { return @jks.html.HtmlLauncher::missingGlyphs(Ljava/lang/String;)(text); })
		};
	}-*/;
}
