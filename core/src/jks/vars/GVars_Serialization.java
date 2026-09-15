package jks.vars;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The game's own JSON (the .wa levels). The .plax backdrops are not read here: the parallax
 * library reads them through its own jks.tools2d.parallax.heart.GVars_Serialization.
 */
public class GVars_Serialization
{

	public static ObjectMapper objectMapper ;

	public static void init()
	{
		jks.tools2d.parallax.heart.GVars_Serialization.init() ;
		prepareJson() ;
	}

	public static ObjectMapper prepareJson()
	{
		if(objectMapper == null) {
			  objectMapper = new ObjectMapper(new JsonFactory()) ;
		}


		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) ;
		objectMapper.addMixIn(TextureRegion.class, IgnoreType.class);

		return objectMapper ;
	}

	// Was the parallax jar's MyMixInForIgnoreType, which the library no longer ships.
	@JsonIgnoreType
	private static abstract class IgnoreType {}
}
