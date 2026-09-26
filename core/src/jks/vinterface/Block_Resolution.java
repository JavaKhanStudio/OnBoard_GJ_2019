package jks.vinterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

import jks.index.Index_Text;
import jks.vars.GVars_Heart;
import jks.amain.GVars_Platform;
import jks.amain.Utils_Config;
import jks.index.Index_Interface;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.tools.PaintedCheckBox;

public class Block_Resolution extends VisTable
{

	VisLabel graphicLabel,resolutionLabel,fpsLabel ;
	PaintedCheckBox vSynchCheckBox ; 
	PaintedCheckBox fullScreenCheckBox ;
	PaintedCheckBox mipmapsCheckBox ; 
	SelectBox<String> selectBox_Resolution ; 
	SelectBox<String> selectBox_FPS ;
	TextButton apply ;

	/** Window sizes a 16:9 game can sensibly be shown in, whatever the monitor lists. */
	static final String[] STANDARD_SIZES = {"1280x720", "1366x768", "1600x900", "1920x1080", "2560x1440", "3840x2160"} ;
	/**
	 * 30 and 60, and deliberately NO "unlimited". Simon settled that 60 is more than enough
	 * (doubt d5, raised from r14), so do not add one back. A config file with fps = 0 still
	 * runs uncapped - the launcher only calls setForegroundFPS when fps > 0 - but that is an
	 * escape hatch for a hand-edited file, not a choice this screen offers, and opening
	 * Options over it selects 30 (SelectBox falls back to the first item) and Apply saves it.
	 */
	static final String[] FPS_CHOICES = {"30", "60"} ;
	
	// Copies of GVars_Font's styles in ink, refreshed in resize() when the fonts are rebuilt.
	private final LabelStyle titleStyle = Utils_Board.ink(GVars_Font.labelStyle_ScreenTitle) ;
	private final LabelStyle textStyle = Utils_Board.ink(GVars_Font.labelStyle_Second) ;
	private final SelectBoxStyle selectStyle = Utils_Board.selectBox() ;
	
	private Cell<VisLabel> titleCell ;
	private final ArrayList<Cell<?>> rowCells = new ArrayList<>() ;
	private Cell<TextButton> applyCell ;
	private float rowHeight ;
	
	public Block_Resolution()
	{		
		setBackground(Utils_Board.board()) ;
		align(Align.top) ;
		
		graphicLabel = new VisLabel(Index_Text.get("options.graphics"),titleStyle) ;
		graphicLabel.setAlignment(Align.center);
		resolutionLabel = new VisLabel(Index_Text.get("options.resolution"),textStyle) ; 
		fpsLabel = new VisLabel(Index_Text.get("options.fps"),textStyle) ; 
		
		selectBox_Resolution = buildResolutionBox() ; 	
		selectBox_FPS = buildFpsBox() ;
		
		vSynchCheckBox = new PaintedCheckBox(Index_Text.get("options.vsync"), GVars_Font.font_Second, Utils_Board.INK) ;
		vSynchCheckBox.setChecked(Utils_Config.current.useVsynch);

		fullScreenCheckBox = new PaintedCheckBox(Index_Text.get("options.fullscreen"), GVars_Font.font_Second, Utils_Board.INK) ;
		// ChangeListeners, not touchUp: a keyboard press has to reach them too, and touchUp
		// also fired when the press started on the button and ended somewhere else.
		fullScreenCheckBox.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				setMaxResolution(fullScreenCheckBox.isChecked()) ;
			}
		}) ;
		// Every widget starts on the settings in force. They used to start on the first
		// resolution with vsync and full screen off, so Apply, pressed to change one thing,
		// quietly changed the others back.
		fullScreenCheckBox.setChecked(Utils_Config.current.isFullScreen);
		
		// Applied and saved the moment it is ticked, like the sound block, rather than waiting
		// for Apply.
		mipmapsCheckBox = new PaintedCheckBox(Index_Text.get("options.mipmaps"), GVars_Font.font_Second, Utils_Board.INK) ; 
		mipmapsCheckBox.setChecked(Utils_Config.current.useMipmaps);
		mipmapsCheckBox.setName("mipmaps");
		mipmapsCheckBox.addListener(new ChangeListener()
		{
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{
				Utils_Config.current.useMipmaps = mipmapsCheckBox.isChecked() ;
				Index_Interface.applyMipmaps(mipmapsCheckBox.isChecked()) ;
				Utils_Config.save() ;
			}
		}) ; 
		
		apply = new TextButton(Index_Text.get("options.apply"),Utils_Board.tagButton()) ;
		apply.addListener(new ChangeListener()
		{		
			@Override
			public void changed(ChangeEvent event, Actor actor)
			{applyNewResolution() ;}
		}) ; 
		
		// On the plank, then a two-column form on the board: words left, controls right.
		titleCell = this.add(graphicLabel).colspan(2).expandX().fillX() ;
		this.row() ; 
		
		// In a browser tab only the mipmaps row (r137): the window rows are built all the same,
		// so the rest of this class need not ask, but never shown.
		if(sizesWindow)
		{
			rowCells.add(this.add(resolutionLabel).left().expandX()) ; 
			this.add(selectBox_Resolution).right() ;
			this.row() ; 
			
			rowCells.add(this.add(fpsLabel).left().expandX()) ; 
			this.add(selectBox_FPS).right() ;
			this.row() ;
			
			rowCells.add(this.add(fullScreenCheckBox).colspan(2).fillX()) ; 
			this.row() ;
			
			rowCells.add(this.add(vSynchCheckBox).colspan(2).fillX()) ; 
			this.row() ;
		}
		
		rowCells.add(this.add(mipmapsCheckBox).colspan(2).fillX()) ; 
		this.row() ;
		
		if(sizesWindow)
			applyCell = this.add(apply).colspan(2).center() ; 
	}
	
	/** False in a browser tab, where only the mipmaps row is shown (Platform.choosesWindowSize). */
	private final boolean sizesWindow = GVars_Platform.current.choosesWindowSize() ;
	
	/** Fits the block to a board of this size: the title on the plank, the rest on the wood. */
	public void resize(float width, float height)
	{
		refreshFonts() ;
		setSize(width, height) ;
		
		float scale = Utils_Board.scale(width) ;
		Utils_Board.pad(this, scale) ;
		float plank = Utils_Board.plankHeight(scale) ;
		titleCell.height(plank).padBottom(Utils_Board.gapBelowPlank(scale)) ;
		
		// Five rows and the Apply tag, which gets a row and a half.
		float body = height - getPadTop() - getPadBottom() - plank - Utils_Board.gapBelowPlank(scale) ;
		float row = body / 6.5f ;
		rowHeight = row ;
		for(Cell<?> cell : rowCells)
			cell.height(row) ;
		for(PaintedCheckBox box : new PaintedCheckBox[] {fullScreenCheckBox, vSynchCheckBox, mipmapsCheckBox})
			box.setBoxHeight(row * 0.8f) ;
		if(applyCell != null)
			applyCell.padTop(row * 0.5f).height(row).minWidth(width / 3f) ;
		invalidateHierarchy() ;
	}
	
	/** How tall resize() made each row, so the sound board can line its rows up with these. */
	public float rowHeight()
	{return rowHeight ;}
	
	/**
	 * GVars_Font builds new fonts when the window width changes - which is what Apply does -
	 * and these styles are copies, so they have to be pointed at the new ones.
	 */
	private void refreshFonts()
	{
		titleStyle.font = GVars_Font.font_MainMenu ;
		graphicLabel.setStyle(titleStyle) ;
		textStyle.font = GVars_Font.font_Second ;
		resolutionLabel.setStyle(textStyle) ;
		fpsLabel.setStyle(textStyle) ;
		for(PaintedCheckBox box : new PaintedCheckBox[] {fullScreenCheckBox, vSynchCheckBox, mipmapsCheckBox})
			box.setFont(GVars_Font.font_Second) ;
		selectStyle.font = selectStyle.listStyle.font = GVars_Font.fontont_SelectBox ;
		selectBox_Resolution.setStyle(selectStyle) ;
		selectBox_FPS.setStyle(selectStyle) ;
		apply.getStyle().font = GVars_Font.font_Title ;
		apply.setStyle(apply.getStyle()) ;
	}
	
	/** The order the keyboard walks this block in, top to bottom. */
	public ArrayList<Actor> focusOrder()
	{
		ArrayList<Actor> order = new ArrayList<>() ;
		if(sizesWindow)
		{
			order.add(selectBox_Resolution) ;
			order.add(selectBox_FPS) ;
			order.add(fullScreenCheckBox) ;
			order.add(vSynchCheckBox) ;
		}
		order.add(mipmapsCheckBox) ;
		if(sizesWindow)
			order.add(apply) ;
		return order ;
	}
	
	public SelectBox<String> buildResolutionBox()
	{
		SelectBox<String> selectBox = new SelectBox<String>(selectStyle);
		List<String> resolutions = resolutionChoices(Gdx.graphics.getDisplayModes(), Gdx.graphics.getDisplayMode(),
				Utils_Config.current.width, Utils_Config.current.height) ;
		selectBox.setItems(resolutions.toArray(new String[0]));
		selectBox.setSelected(savedResolution());
		return selectBox ;
	}

	public SelectBox<String> buildFpsBox()
	{
		SelectBox<String> returning = new SelectBox<String>(selectStyle);

		ArrayList<String> fpsChoice = new ArrayList<>(Arrays.asList(FPS_CHOICES)) ;
		String saved = String.valueOf(Utils_Config.current.fps) ;
		// A value written by hand, or by a later version, is shown rather than silently replaced.
		if(Utils_Config.current.fps > 0 && !fpsChoice.contains(saved))
			fpsChoice.add(saved) ;
		returning.setItems(fpsChoice.toArray(new String[0]));
		returning.setSelected(saved);

		return returning ;
	}

	private String savedResolution()
	{
		return Utils_Config.current.width + "x" + Utils_Config.current.height ;
	}

	private void setMaxResolution(boolean settingMax)
	{
		if(settingMax)
		{
			// Full screen takes the desktop's mode, so that is what the box shows.
			DisplayMode desktop = Gdx.graphics.getDisplayMode() ;
			String desktopSize = desktop.width + "x" + desktop.height ;
			if(selectBox_Resolution.getItems().contains(desktopSize, false))
				selectBox_Resolution.setSelected(desktopSize);
			else
				selectBox_Resolution.setSelectedIndex(selectBox_Resolution.getItems().size - 1);
			selectBox_Resolution.setDisabled(true);
		}
		else
		{
			// Back to the window you had, not a window the size of the monitor.
			selectBox_Resolution.setSelected(savedResolution());
			selectBox_Resolution.setDisabled(false);
		}
	}

	public void applyNewResolution()
	{
		// Never trust the box to hold something: it held nothing on every monitor without a
		// 16:9 mode at exactly 60 Hz, and Apply died on it.
		int[] size = parseResolution(selectBox_Resolution.getSelected()) ;
		if(size == null)
			size = new int[] {Utils_Config.current.width, Utils_Config.current.height} ;
		int width = size[0] ;
		int height = size[1] ;

		Gdx.graphics.setVSync(vSynchCheckBox.isChecked());

		int fps = Utils_Config.current.fps ;
		try
		{fps = Integer.parseInt(selectBox_FPS.getSelected()) ;}
		catch(NumberFormatException | NullPointerException keepSaved)
		{}
		Gdx.graphics.setForegroundFPS(fps);

		Monitor currMonitor = Gdx.graphics.getMonitor();

		if(fullScreenCheckBox.isChecked())
		{
			DisplayMode displayMode = Gdx.graphics.getDisplayMode(currMonitor);
			Gdx.graphics.setFullscreenMode(displayMode) ;
			GVars_Font.resize();
			GVars_Heart.vue.resize(displayMode.width, displayMode.height);
		}
		else
		{
			Gdx.graphics.setWindowedMode(width, height);
			GVars_Platform.current.centreWindow() ;
			GVars_Font.resize();
			GVars_Heart.vue.resize(width, height);

			// Remember it. This screen applied a resolution and then forgot it the moment the
			// game closed, so choosing one was something you had to do again on every launch.
			// Only when windowed: in full screen the box shows the monitor, and saving that
			// would make the window as big as the monitor once full screen was turned off.
			Utils_Config.current.width = width ;
			Utils_Config.current.height = height ;
		}

		GVars_Heart.isFullScreen = fullScreenCheckBox.isChecked() ;
		Utils_Config.current.isFullScreen = fullScreenCheckBox.isChecked() ;
		Utils_Config.current.useVsynch = vSynchCheckBox.isChecked() ;
		Utils_Config.current.fps = fps ;
		Utils_Config.save() ;
	}

	/**
	 * The sizes the resolution box offers, smallest first, as "WIDTHxHEIGHT".
	 *
	 * This used to keep only the monitor's own 16:9 modes at exactly 60 Hz, so a 59.94, 144
	 * or 165 Hz screen, or a 16:10 one, got an empty box and Apply threw. The refresh rate
	 * says nothing about a window size, and a window does not need to match a display mode
	 * at all: it only has to fit on the desktop. So: every 16:9 size, listed or standard,
	 * that fits, plus the saved size so the box can always show what is in force.
	 */
	public static List<String> resolutionChoices(DisplayMode[] modes, DisplayMode desktop, int savedWidth, int savedHeight)
	{
		int maxWidth = Integer.MAX_VALUE, maxHeight = Integer.MAX_VALUE ;
		if(desktop != null && desktop.width > 0 && desktop.height > 0)
		{
			maxWidth = desktop.width ;
			maxHeight = desktop.height ;
		}

		TreeSet<int[]> sizes = new TreeSet<>(Comparator.<int[]>comparingInt(s -> s[0]).thenComparingInt(s -> s[1])) ;
		ArrayList<int[]> candidates = new ArrayList<>() ;
		if(modes != null)
			for(DisplayMode mode : modes)
				candidates.add(new int[] {mode.width, mode.height}) ;
		for(String standard : STANDARD_SIZES)
			candidates.add(parseResolution(standard)) ;

		for(int[] size : candidates)
		{
			if(size[0] >= 800 && size[0] <= maxWidth && size[1] <= maxHeight && isSixteenByNine(size[0], size[1]))
				sizes.add(size) ;
		}
		if(savedWidth > 0 && savedHeight > 0)
			sizes.add(new int[] {savedWidth, savedHeight}) ;

		ArrayList<String> choices = new ArrayList<>() ;
		for(int[] size : sizes)
			choices.add(size[0] + "x" + size[1]) ;
		return choices ;
	}

	/** 1366x768 and 854x480 are 16:9 too, near enough; the old test cut them off at 1.778. */
	static boolean isSixteenByNine(int width, int height)
	{
		return height > 0 && Math.abs((float)width / height - 16f / 9f) < 0.01f ;
	}

	/** "1600x900" to {1600, 900}; null for anything else. */
	public static int[] parseResolution(String resolution)
	{
		if(resolution == null)
			return null ;
		int x = resolution.indexOf('x') ;
		if(x <= 0)
			return null ;
		try
		{
			int width = Integer.parseInt(resolution.substring(0, x).trim()) ;
			int height = Integer.parseInt(resolution.substring(x + 1).trim()) ;
			return width > 0 && height > 0 ? new int[] {width, height} : null ;
		}
		catch(NumberFormatException e)
		{
			return null ;
		}
	}
	
//	graphicLabel.setStyle(GVars_UI.labelStyle_Title) ; 
//	
//	vSynchCheckBox.getLabel().getStyle().font = GVars_UI.font_Main ; 
//	fullScreenCheckBox.getLabel().getStyle().font = GVars_UI.font_Main ; 
//	selectBox_Resolution.getStyle().font = GVars_UI.font_Main ; 
//	selectBox_FPS.getStyle().font = GVars_UI.font_Main ;
//	
	
//	VisCheckBox vSynchCheckBox ; 
//	VisCheckBox fullScreenCheckBox ;
//	SelectBox<String> selectBox_Resolution ; 
//	SelectBox<String> selectBox_FPS ;
//	
//	HashMap<String,DisplayMode> displayMap ;
//	ArrayList<String> displayList ;
}