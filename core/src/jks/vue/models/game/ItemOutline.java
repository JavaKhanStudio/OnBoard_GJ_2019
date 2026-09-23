package jks.vue.models.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * How a carriage item under the mouse lights (d10, replacing r40's additive glow): a thin,
 * half-transparent yellow line just outside the item's own shape, broken into short strokes
 * that drift slowly along, like a shine. The item itself is drawn untouched, so a pale item
 * (the bundle, the papers) gains an edge instead of washing out.
 *
 * The line follows the texture's alpha, not its box, and is drawn on a quad grown by the line's
 * width so it can sit outside an item painted right to the edge of its image (cube.png).
 *
 * Its colour says what the item gives (r71), see {@link Kind}: gold and unbroken for a piece of
 * the key, blue or red strokes for the two sides of a choice, d10's yellow for the rest.
 *
 * The numbers are static and not saved: the item lab (-Donboard.start=item_lab, Vue_Game with
 * ItemOutlineLab over it) moves them live to choose them. Pin a choice here.
 */
public final class ItemOutline
{
	/** Line thickness, in texture pixels. */
	public static float width = 2.5f ;
	/** 0 to 1, applied over the colour's own alpha. */
	public static float opacity = 0.7f ;
	/** One stroke and its gap, in screen pixels; 0 draws a solid line. */
	public static float strokeLength = 12f ;
	/** How much of strokeLength is drawn, 0 to 1. */
	public static float strokeFill = 0.6f ;
	/** How fast the strokes travel, in screen pixels per second; 0 holds them still. */
	public static float speed = 18f ;
	public static final Color color = new Color(1f, 0.86f, 0.25f, 1f) ;
	/** Lab only: every unpicked item outlined, hovered or not, to compare them side by side. */
	public static boolean lightAll ;
	
	/**
	 * What an item gives, which picks its line (r71). Worked out from the carriage's data by
	 * {@link #of}, so the .wa files carry nothing new.
	 */
	public enum Kind
	{
		/** Hands over a piece of the key when picked up: an unbroken gold line. */
		KEY(new Color(1f, 0.72f, 0.05f, 1f), true, 1f),
		/** Used on something, the side of a choice that counts no karma: blue strokes. */
		GOOD(new Color(0.1f, 0.35f, 1f, 1f), false, 1f),
		/** Used on something, the side that counts a point of karma, towards Ross leaving his son: red strokes. */
		BAD(new Color(1f, 0.15f, 0.1f, 1f), false, 1f),
		/** Used on something with no choice attached, and everything else: d10's yellow strokes. */
		NEUTRAL(ItemOutline.color, false, -1f) ;
		
		public final Color color ; 
		/** An unbroken line rather than strokes. */
		public final boolean solid ; 
		/**
		 * Its own opacity, or negative to follow {@link ItemOutline#opacity}. The coloured lines
		 * say something, so they are drawn whole: at 0.7 the blue vanished on carriage 1's blue floor.
		 */
		public final float opacity ; 
		
		Kind(Color color, boolean solid, float opacity)
		{
			this.color = color ; 
			this.solid = solid ; 
			this.opacity = opacity ; 
		}
	}
	
	/**
	 * The kind of an item among the others of its carriage. An item used on something is
	 * named by that thing's name_Interaction_1 or _2; where both exist the thing is a choice,
	 * and _2 is the one GVars_Game.applyItem counts a point of karma for. More than
	 * GVars_Game.KARMA_TO_LEAVE of those and Ross walks out on his son, so _2 is the bad side.
	 */
	public static Kind of(GameItem item, Iterable<GameItem> carriage)
	{
		if(item.giveKey_take)
			return Kind.KEY ; 
		
		for(GameItem target : carriage)
		{
			boolean choice = target.name_Interaction_1 != null && target.name_Interaction_2 != null ; 
			if(choice && item.name.equals(target.name_Interaction_2))
				return Kind.BAD ; 
			if(choice && item.name.equals(target.name_Interaction_1))
				return Kind.GOOD ; 
		}
		return Kind.NEUTRAL ; 
	}

	private static final String VERTEX =
		"attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
		+ "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
		+ "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
		+ "uniform mat4 u_projTrans;\n"
		+ "varying vec4 v_color;\n"
		+ "varying vec2 v_texCoords;\n"
		+ "void main()\n"
		+ "{\n"
		+ "	v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
		+ "	v_color.a = v_color.a * (255.0/254.0);\n"
		+ "	v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
		+ "	gl_Position = u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
		+ "}\n" ;

	// Sixteen directions at the full width and at half of it: enough that a thin stalk of an
	// item (the key, the pencil) still gets a closed line around it.
	private static final String FRAGMENT =
		"#ifdef GL_ES\n"
		+ "precision mediump float;\n"
		+ "#endif\n"
		+ "varying vec4 v_color;\n"
		+ "varying vec2 v_texCoords;\n"
		+ "uniform sampler2D u_texture;\n"
		+ "uniform vec2 u_texel;\n"
		+ "uniform float u_width;\n"
		+ "uniform vec4 u_lineColor;\n"
		+ "uniform float u_strokeLength;\n"
		+ "uniform float u_strokeFill;\n"
		+ "uniform float u_offset;\n"
		+ "float alphaAt(vec2 uv)\n"
		+ "{\n"
		+ "	vec2 inside = step(vec2(0.0), uv) * step(uv, vec2(1.0));\n"
		+ "	return texture2D(u_texture, uv).a * inside.x * inside.y;\n"
		+ "}\n"
		+ "void main()\n"
		+ "{\n"
		+ "	float near = 0.0;\n"
		+ "	for (int i = 0; i < 16; i++)\n"
		+ "	{\n"
		+ "		float angle = float(i) * 0.3926991;\n"
		+ "		vec2 dir = vec2(cos(angle), sin(angle)) * u_texel;\n"
		+ "		near = max(near, alphaAt(v_texCoords + dir * u_width));\n"
		+ "		near = max(near, alphaAt(v_texCoords + dir * u_width * 0.5));\n"
		+ "	}\n"
		+ "	float edge = near * (1.0 - alphaAt(v_texCoords));\n"
		+ "	float stroke = 1.0;\n"
		+ "	if (u_strokeLength > 0.0)\n"
		+ "	{\n"
		+ "		float along = fract((gl_FragCoord.x + gl_FragCoord.y - u_offset) / u_strokeLength);\n"
		+ "		stroke = 1.0 - smoothstep(u_strokeFill - 0.08, u_strokeFill, along);\n"
		+ "	}\n"
		+ "	gl_FragColor = vec4(u_lineColor.rgb, u_lineColor.a * edge * stroke * v_color.a);\n"
		+ "}\n" ;

	private static ShaderProgram shader ;
	private static float clock ;

	private ItemOutline()
	{}

	/** Once a frame, before the items draw: the strokes move on the game's own clock. */
	public static void advance(float delta)
	{
		clock = (clock + delta) % 3600f ;
	}

	/** Draws d10's yellow line around a texture drawn at x, y at its own size. Call between begin and end. */
	public static void draw(Batch batch, Texture texture, float x, float y)
	{
		draw(batch, texture, x, y, Kind.NEUTRAL) ; 
	}
	
	/** Draws the line of that kind around a texture drawn at x, y at its own size. Call between begin and end. */
	public static void draw(Batch batch, Texture texture, float x, float y, Kind kind)
	{
		Color color = kind.color ; 
		float w = texture.getWidth(), h = texture.getHeight() ;
		float grow = (float) Math.ceil(width) + 1f ;
		float du = grow / w, dv = grow / h ;

		ShaderProgram previous = batch.getShader() ;
		batch.setShader(shader()) ;
		shader.setUniformf("u_texel", 1f / w, 1f / h) ;
		shader.setUniformf("u_width", width) ;
		shader.setUniformf("u_lineColor", color.r, color.g, color.b, color.a * (kind.opacity < 0 ? opacity : kind.opacity)) ;
		shader.setUniformf("u_strokeLength", kind.solid ? 0f : strokeLength) ;
		shader.setUniformf("u_strokeFill", strokeFill) ;
		shader.setUniformf("u_offset", clock * speed) ;
		// v runs top to bottom in the texture, as in Batch.draw(texture, x, y, w, h).
		batch.draw(texture, x - grow, y - grow, w + 2 * grow, h + 2 * grow, -du, 1f + dv, 1f + du, -dv) ;
		// The uniforms belong to this item only: send its quad before the next one sets them.
		batch.flush() ;
		batch.setShader(previous) ;
	}

	private static ShaderProgram shader()
	{
		if(shader == null)
		{
			shader = new ShaderProgram(VERTEX, FRAGMENT) ;
			if(!shader.isCompiled())
				throw new GdxRuntimeException("The item outline shader did not compile:\n" + shader.getLog()) ;
		}
		return shader ;
	}
}
