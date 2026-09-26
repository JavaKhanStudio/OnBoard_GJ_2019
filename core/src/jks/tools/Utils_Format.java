package jks.tools;

/**
 * Numbers as text for the labs, the way String.format(Locale.ROOT, "%.2f") wrote them. GWT has no
 * String.format, and the browser build compiles core (r137).
 */
public class Utils_Format
{
	/** value with exactly this many decimals, rounded half up, with a point: fixed(0.5f, 2) is "0.50". */
	public static String fixed(double value, int decimals)
	{
		long scale = 1 ;
		for(int i = 0 ; i < decimals ; i++)
			scale *= 10 ;
		long scaled = Math.round(Math.abs(value) * scale) ;
		String digits = String.valueOf(scaled % scale + scale).substring(1) ;
		// a negative that rounds to nothing keeps its sign, as "%.1f" writes -0.04 as "-0.0"
		String sign = value < 0 ? "-" : "" ;
		return sign + (scaled / scale) + (decimals > 0 ? "." + digits : "") ;
	}

	/** At least two digits: 7 is "07", as %02d wrote it. */
	public static String twoDigits(int value)
	{
		return value >= 0 && value < 10 ? "0" + value : String.valueOf(value) ;
	}
}
