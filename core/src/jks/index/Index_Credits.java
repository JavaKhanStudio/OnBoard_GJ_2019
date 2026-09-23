package jks.index;

/**
 * Who made On Board.
 *
 * This is the whole of the credits content - edit here and the screen follows. Roles are
 * deliberately plain strings rather than an enum, because they are prose and will want
 * rewording ("Art visuel" is a placeholder for something more precise per person).
 * The words are rows of i18n/textes.tsv (r76); the names are not translated and stay here.
 */
public class Index_Credits 
{

	/** Unaccented on purpose: the title face cannot draw é, and "Credits" was chosen (d8). */
	public static final String TITLE = Index_Text.get("credits.title") ;
	
	/** The team, as the logo that plays before the game says it. */
	public static final String TEAM = "PIX MEN" ;
	
	public static final Section[] SECTIONS = new Section[]
	{
		new Section(Index_Text.get("credits.programming"), new String[]
		{
			"Simon Bédard",
		}),
		
		new Section(Index_Text.get("credits.art"), new String[]
		{
			"Carole Virginie",
			"Claire Montagut",
			"Clement Diolot",
		}),
	} ;
	
	/**
	 * Where the game came from, and what it was built on.
	 */
	public static final String[] FOOTER = new String[]
	{
		"Jamming Assembly - 2019",
		Index_Text.get("credits.made_with"),
	} ;

	public static class Section
	{
		public final String role ;
		public final String[] people ;
		
		public Section(String role, String[] people)
		{
			this.role = role ;
			this.people = people ;
		}
	}
	
}
