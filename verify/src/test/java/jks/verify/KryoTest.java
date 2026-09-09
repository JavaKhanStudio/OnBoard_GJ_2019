package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import jks.vars.GVars_Serialization;

/**
 * The project pinned kryo 5.0.0-RC1 - a release candidate. Kryo changed registration and
 * serializer APIs on the way to 5.x final, and Color is handled by a hand-written serializer
 * (Color_Serializer), so a round-trip is pinned here rather than assumed.
 */
class KryoTest
{
	@Test
	@DisplayName("the registered class set still configures cleanly")
	void kryoConfigures()
	{
		Kryo kryo = GVars_Serialization.prepareKryo();
		assertNotNull(kryo, "prepareKryo() returned null");
	}

	@Test
	@DisplayName("Color survives a round-trip through the custom serializer")
	void colorRoundTrips()
	{
		Kryo kryo = GVars_Serialization.prepareKryo();
		Color original = new Color(0.25f, 0.5f, 0.75f, 1f);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (Output out = new Output(bytes))
		{
			kryo.writeObject(out, original);
		}

		Color restored;
		try (Input in = new Input(new ByteArrayInputStream(bytes.toByteArray())))
		{
			restored = kryo.readObject(in, Color.class);
		}

		assertNotNull(restored, "Color deserialised to null");
		assertEquals(original.r, restored.r, 1e-6, "red channel");
		assertEquals(original.g, restored.g, 1e-6, "green channel");
		assertEquals(original.b, restored.b, 1e-6, "blue channel");
		assertEquals(original.a, restored.a, 1e-6, "alpha channel");
	}

	@Test
	@DisplayName("ArrayList is registered, as the parallax models rely on it")
	void arrayListRoundTrips()
	{
		Kryo kryo = GVars_Serialization.prepareKryo();

		ArrayList<String> original = new ArrayList<>();
		original.add("one");
		original.add("two");

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (Output out = new Output(bytes))
		{
			kryo.writeObject(out, original);
		}

		try (Input in = new Input(new ByteArrayInputStream(bytes.toByteArray())))
		{
			@SuppressWarnings("unchecked")
			ArrayList<String> restored = kryo.readObject(in, ArrayList.class);
			assertEquals(original, restored);
		}
	}
}
