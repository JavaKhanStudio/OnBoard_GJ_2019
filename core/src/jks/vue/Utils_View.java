package jks.vue;

import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vinterface.overlay.OverlayModel;

public class Utils_View 
{

	public static OverlayModel getOverlay()
	{return GVars_Heart.vue.overlay ;}
	
	public static void setOverlay(OverlayModel overlay)
	{
		removeCurrentOverlay() ; 
		GVars_Heart.vue.overlay = overlay ; 
		GVars_UI.mainUi.addActor(overlay) ;
		GVars_UI.activedInterface(GVars_Heart.vue.overlay);
	}
	
	public static void removeCurrentOverlay()
	{
		if(GVars_Heart.vue.overlay != null)
		{
			GVars_Heart.vue.overlay.destroy();
			GVars_Heart.vue.overlay = null ; 
		}
		else if(GVars_Heart.debug)
			System.out.println("no Overlay to remove in removeCurrentOverlay of Utils_View");
		
		GVars_UI.resetInterface() ;
	}
	
	public static void setFilter(AView_Model_Filter filter_CanBuild)
	{
		removeFilter() ;
		GVars_Heart.vue.filter = filter_CanBuild ;
	}

	public static void removeFilter() 
	{
		if(GVars_Heart.vue.filter != null)
		{GVars_Heart.vue.filter = null ;}
	}
}
