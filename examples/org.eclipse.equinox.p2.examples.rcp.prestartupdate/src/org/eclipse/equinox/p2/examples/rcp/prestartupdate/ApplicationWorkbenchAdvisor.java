package org.eclipse.equinox.p2.examples.rcp.prestartupdate;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

/**
 * This workbench advisor creates the window advisor, and specifies
 * the perspective id for the initial window.
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.application.WorkbenchAdvisor#preStartup()
     */
    public void preStartup() {
    	// XXX check for updates before starting up.
    	// If an update is performed, restart.
    	if (P2Util.checkForUpdates())
    		PlatformUI.getWorkbench().restart();
    }
    
	public String getInitialWindowPerspectiveId() {
		return Perspective.ID;
	} 
	
}
