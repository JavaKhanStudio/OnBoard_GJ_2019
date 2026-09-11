package jks.index;

/**
 * Who made On Board.
 *
 * This is the whole of the credits content - edit here and the screen follows. Roles are
 * deliberately plain strings rather than an enum, because they are prose and will want
 * rewording ("Art visuel" is a placeholder for something more precise per person).
 */
public class Index_Credits 
{

	public static final String TITLE = "Credits" ;
	
	/** The team, as the logo that plays before the game says it. */
	public static final String TEAM = "PIX MEN" ;
	
	public static final Section[] SECTIONS = new Section[]
	{
		new Section("Programmation", new String[]
		{
			"Simon Bédard",
		}),
		
		new Section("Art visuel", new String[]
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
		"Réalisé avec libGDX",
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
