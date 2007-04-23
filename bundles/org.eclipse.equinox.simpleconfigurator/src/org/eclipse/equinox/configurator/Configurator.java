package org.eclipse.equinox.configurator;

import java.io.IOException;
import java.net.URL;

/**
 * The implementation of this interface will be registered into a service registry
 * by a Configurator Bundle.
 * 
 * The client bundle can apply configuration which can be interpreted by refering
 * the specified location to the current running OSGi environment. In addition, 
 * the client can expect bundle state in advance .  
 * 
 * TODO: this interface might not be required to be defined.
 * 
 * 
 * **********************************************
 * Current Definition of Configurator Bundle: 
 * 
 * Configurator Bundle will do the followinig operation at its startup.
 * 
 * 1. Create a Configurator object.
 * 2. Register it as a service to the service registry.
 * 3. Get where to read for knowing what kinds of bundles in its implementation dependend way.
 * 4. Call {@link Configurator#applyConfiguration(URL)} with the URL.
 * 
 * At its stopping, the service registered will be unregistered.
 * 
 * @see ConfiguratorManipulator
 *
 */
public interface Configurator {

	/**
	 * Apply configuration read from the specified url to the OSGi 
	 * environment currently running.
	 * 
	 * @param url URL to be read.
	 * @throws IOException - If reading information from the specified url fails. 
	 */
	void applyConfiguration(URL url) throws IOException;

	/**
	 * Apply configuration read from the previously used url to the OSGi 
	 * environment currently running. If it is never used, do nothing.
	 * 
	 * @throws IOException - If reading information from the specified url fails. 
	 */
	void applyConfiguration() throws IOException;

	/**
	 * Return the url in use.
	 * If it is never used, return null.
	 * 
	 * @return
	 */
	URL getUrlInUse();

	
//	/**
//	 * Return expected bundle states (as an array of BundleInfo) 
//	 * as if {@link Configurator#applyConfiguration(URL)} with an argument of the specified
//	 * url will be called now.
//	 * 
//	 * If there are no ManipulatorAdmin services which create a Manipulator initialized according to
//	 * the running fw state, null will be returned.
//	 * 
//	 * This method can be used to check what kinds of bundles will be 
//	 * installed on the system in advance to calling {@link Configurator#applyConfiguration(URL)}.
//	 *  
//	 * @param url URL to be referred.
//	 * @return Array of BundleInfo representing the expected state. null 
//	 * if there are no ManipulatorAdmin services which create a Manipulator initialized according to
//	 * the running fw state.
//	 * @throws IOException - If reading information from the specified url fails. 
//	 */
//	BundleInfo[] getExpectedStateRuntime(URL url) throws IOException;
}
