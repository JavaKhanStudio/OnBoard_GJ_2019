package jks.vue.models.game;

import com.badlogic.gdx.Gdx;
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

	/** Adds one item to the row and re-centres what is now in it. */
	public void carry(Actor item)
	{
		addActor(item);
		place() ;
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

	/** Drops the lot: nothing is carried from one carriage into the next. */
	public void empty()
	{
		clearChildren();
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
