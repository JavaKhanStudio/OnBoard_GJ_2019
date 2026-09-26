package jks.vars;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;

/**
 * The game's own JSON: the .wa levels and the config. The .plax backdrops are not read here:
 * the parallax library reads them through its own jks.tools2d.parallax.heart.GVars_Serialization.
 *
 * libGDX Json, not Jackson (r136): Jackson is reflection-driven in a way GWT cannot translate,
 * and the browser build compiles core. libGDX Json reads and writes every field that is not
 * static or transient, whatever its visibility, so a model's runtime state is marked transient
 * (WagonLevel, GameItem). SerializationDumpTest holds every field to what Jackson made of them.
 */
public class GVars_Serialization
{

	public static Json json ;

	public static void init()
	{
		jks.tools2d.parallax.heart.GVars_Serialization.init() ;
		prepareJson() ;
	}

	public static Json prepareJson()
	{
		if(json == null)
			json = newJson() ;
		return json ;
	}

	/**
	 * Plain JSON with quoted names, as Jackson wrote it, so a file this build writes still reads
	 * in the last one. A field left at its default is written too (no prototypes): the editor's
	 * files and the config keep every field, as they always had. A field the model does not
	 * have is skipped, as FAIL_ON_UNKNOWN_PROPERTIES=false did: a newer config still loads.
	 */
	public static Json newJson()
	{
		Json json = new Json(OutputType.json) ;
		json.setIgnoreUnknownFields(true) ;
		json.setUsePrototypes(false) ;
		return json ;
	}
}
