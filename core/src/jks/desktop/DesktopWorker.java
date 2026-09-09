package jks.desktop;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeBitmapFontData;
import com.badlogic.gdx.utils.Array;

import java.io.File;
import java.util.List;

public interface DesktopWorker {
    public void texturePack(Array<FileHandle> handles, FileHandle localFile, FileHandle targetFile, FileHandle settingsFile);
    public void packFontImages(Array<FileHandle> files, FileHandle saveFile);
    public void sizeWindowToFit(int maxWidth, int maxHeight, int displayBorder, Graphics graphics);
    public void centerWindow(Graphics graphics);
//    public void addFilesDroppedListener(FilesDroppedListener filesDroppedListener);
//    public void removeFilesDroppedListener(FilesDroppedListener filesDroppedListener);
//    public void setCloseListener(CloseListener closeListener);
    public void attachLogListener();
    public List<File> openMultipleDialog(String title, String defaultPath, String[] filterPatterns, String filterDescription);
    public File openDialog(String title, String defaultPath, String[] filterPatterns, String filterDescription);
    public File saveDialog(String title, String defaultPath, String[] filterPatterns, String filterDescription);
    public void closeSplashScreen();
    public char getKeyName(int keyCode);
    public void writeFont(FreeTypeBitmapFontData data, Array<PixmapPacker.Page> pages, FileHandle target);
}
