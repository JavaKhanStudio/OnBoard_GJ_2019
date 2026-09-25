package jks.vue.models.game;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;

/**
 * The row of what the player is carrying, centred on the black bar along the bottom of a
 * carriage (r59).
 *
 * It empties when the carriage changes. Every interaction in a carriage names only that
 * carriage's items, so anything picked up before is of no use in the next one, and a row of
 * leftovers reads as something still to be used.
 *
 * The items used to be laid out from the left edge, one bar-height square after another, which
 * put them in the corner under Ross rather than anywhere the eye goes.
 */
public class InventoryBar extends Group
{
	/** Each item is a square of this much of the bar's height, so the row does not touch its edges. */
	private static final float ITEM_OF_BAR = 0.8f ;
	/** The space between two items, as a fraction of one item. */
	private static final float GAP_OF_ITEM = 0.2f ;

	/**
	 * The item in hand sits on a pale square with a white edge (r84), a little larger than the
	 * item, so the player sees what a click in the carriage will use. Nothing else is marked.
	 */
	private static final float MARK_OF_ITEM = 0.12f ;
	private static final Color MARK_FILL = new Color(1f, 1f, 1f, 0.22f) ;
	private static final Color MARK_EDGE = new Color(1f, 1f, 1f, 0.9f) ;
	private static final float MARK_EDGE_PX = 2f ;
	private static Texture white ;

	/**
	 * An item arriving in the row for the first time flashes (r93): a white glow behind it, the
	 * held mark's box, rises and falls FLASH_COUNT times. Same rhythm as the key's pieces, which
	 * flash at the same pickup (r91, Clef.FLASH_*), so the two pulse together.
	 */
	static final float FLASH_PEAK = 0.6f ;
	static final float FLASH_PULSE_SECONDS = 2 * Clef.FLASH_SECONDS ;
	static final int FLASH_COUNT = Clef.FLASH_COUNT ;

	/** The carried actors still flashing, and for how long they have. */
	private final Map<Actor, Float> flashing = new IdentityHashMap<>() ;
	/** What has already flashed in this carriage: "the first time" is once per item. */
	private final Set<Object> flashed = new HashSet<>() ;

	/** Adds one item to the row and re-centres what is now in it. */
	public void carry(Actor item)
	{
		addActor(item);
		place() ;
		Object key = item.getUserObject() != null ? item.getUserObject() : item ;
		if(flashed.add(key))
			flashing.put(item, 0f) ;
	}

	/** The carried item is flashing now. For the render tests. */
	public boolean isFlashing(Actor item)
	{
		return flashing.containsKey(item) ;
	}

	@Override
	public void act(float delta)
	{
		super.act(delta) ;
		float length = FLASH_PULSE_SECONDS * FLASH_COUNT ;
		for(Iterator<Map.Entry<Actor, Float>> it = flashing.entrySet().iterator() ; it.hasNext() ; )
		{
			Map.Entry<Actor, Float> flash = it.next() ;
			float t = flash.getValue() + delta ;
			if(t >= length || flash.getKey().getParent() != this)
				it.remove() ;
			else
				flash.setValue(t) ;
		}
	}

	/** Takes the one carried item out of the row, and closes the gap it leaves (r77). */
	public void drop(GameItem item)
	{
		for(Actor carried : getChildren())
			if(carried.getUserObject() == item)
			{
				removeActor(carried) ;
				place() ;
				return ;
			}
	}

	/**
	 * The carried item that is in hand, or null. Read from GVars_Game.selectedItem every time,
	 * so whatever clears the selection - a second click, a use (r77), a new carriage - clears
	 * the mark with it.
	 */
	public Actor held()
	{
		if(GVars_Game.selectedItem == null)
			return null ;
		for(Actor carried : getChildren())
			if(carried.getUserObject() == GVars_Game.selectedItem)
				return carried ;
		return null ;
	}

	@Override
	public void draw(Batch batch, float parentAlpha)
	{
		for(Map.Entry<Actor, Float> flash : flashing.entrySet())
		{
			// Each pulse a smooth rise and fall, from nothing back to nothing.
			float pulse = MathUtils.sin(MathUtils.PI * (flash.getValue() % FLASH_PULSE_SECONDS) / FLASH_PULSE_SECONDS) ;
			float alpha = FLASH_PEAK * pulse * pulse ;
			drawBox(batch, flash.getKey(), 1f, 1f, 1f, alpha * parentAlpha, 0f) ;
		}

		Actor held = held() ;
		if(held != null)
		{
			drawBox(batch, held, MARK_FILL.r, MARK_FILL.g, MARK_FILL.b, MARK_FILL.a * parentAlpha, 0f) ;
			drawBox(batch, held, MARK_EDGE.r, MARK_EDGE.g, MARK_EDGE.b, MARK_EDGE.a * parentAlpha, MARK_EDGE_PX) ;
		}

		super.draw(batch, parentAlpha) ;
	}

	/** The square a little larger than a carried item: filled, or only its edge when edge > 0. */
	private void drawBox(Batch batch, Actor carried, float r, float g, float b, float a, float edge)
	{
		if(white == null)
		{
			Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888) ;
			pixel.setColor(Color.WHITE) ;
			pixel.fill() ;
			white = new Texture(pixel) ;
			pixel.dispose() ;
		}

		float out = carried.getWidth() * MARK_OF_ITEM ;
		float x = getX() + carried.getX() - out, y = getY() + carried.getY() - out ;
		float w = carried.getWidth() + 2 * out, h = carried.getHeight() + 2 * out ;
		Color was = batch.getColor().cpy() ;
		batch.setColor(r, g, b, a) ;
		if(edge <= 0f)
			batch.draw(white, x, y, w, h) ;
		else
		{
			batch.draw(white, x, y, w, edge) ;
			batch.draw(white, x, y + h - edge, w, edge) ;
			batch.draw(white, x, y, edge, h) ;
			batch.draw(white, x + w - edge, y, edge, h) ;
		}
		batch.setColor(was) ;
	}

	/** Drops the lot: nothing is carried from one carriage into the next. */
	public void empty()
	{
		clearChildren();
		flashing.clear() ;
		flashed.clear() ;
	}

	/**
	 * Lays the row out from its own width against the middle of the bar. The stage is in
	 * screen pixels, and so is the bar the level draws, so the two agree without conversion.
	 */
	public void place()
	{
		if(GVars_Game.currentLevel == null)
			return ;

		float bar = GVars_Game.currentLevel.decalYBot ;
		float item = bar * ITEM_OF_BAR ;
		float gap = item * GAP_OF_ITEM ;

		int count = getChildren().size ;
		float row = count * item + Math.max(0, count - 1) * gap ;
		float across = getStage() != null ? getStage().getWidth() : Gdx.graphics.getWidth() ;
		float x = (across - row) / 2f ;
		float y = (bar - item) / 2f ;

		for(int i = 0 ; i < count ; i++)
		{
			Actor carried = getChildren().get(i) ;
			carried.setSize(item, item);
			carried.setPosition(x + i * (item + gap), y);
		}
	}
}
