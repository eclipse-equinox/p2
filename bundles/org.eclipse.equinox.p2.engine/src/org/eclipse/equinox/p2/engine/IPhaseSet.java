/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;


/**
 * Describes a set of provisioning phases to be performed by an {@link IEngine}.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IPhaseSet {

	/**
	 * A phase id (value "checkTrust") describing the certificate trust check phase.
	 * This phase examines the code signing certificates of the artifacts being installed
	 * to ensure they are signed and trusted by the running system.
	 */
	public static String PHASE_CHECK_TRUST = "checkTrust"; //$NON-NLS-1$
	/**
	 * A phase id (value "collect") describing the collect phase.
	 * This phase gathers all the artifacts to be installed, typically by copying them
	 * from some repository into a suitable local location for the application being installed.
	 */
	public static String PHASE_COLLECT = "collect"; //$NON-NLS-1$
	/**
	 * A phase id (value "configure") describing the configuration phase.
	 * This phase writes configuration data related to the software being provisioned.
	 * Until configuration occurs the end user of the software will be have access to
	 * the installed functionality.
	 */
	public static String PHASE_CONFIGURE = "configure"; //$NON-NLS-1$
	/**
	 * A phase id (value "install") describing the install phase.
	 * This phase performs any necessary transformations on the downloaded
	 * artifacts to put them in the correct shape for the running application, such
	 * as decompressing or moving content, setting file permissions, etc).
	 */
	public static String PHASE_INSTALL = "install"; //$NON-NLS-1$
	/**
	 * A phase id (value "property") describing the property modification phase.
	 * This phase performs changes to profile properties.
	 */
	public static String PHASE_PROPERTY = "property"; //$NON-NLS-1$
	/**
	 * A phase id (value "unconfigure") describing the unconfigure phase.
	 * This phase removes configuration data related to the software being removed.
	 * This phase is the inverse of the changes performed in the configure phase.
	 */
	public static String PHASE_UNCONFIGURE = "unconfigure"; //$NON-NLS-1$
	/**
	 * A phase id (value "uninstall") describing the uninstall phase.
	 * This phase removes artifacts from the system being provisioned that are
	 * no longer required in the new profile.
	 */
	public static String PHASE_UNINSTALL = "uninstall"; //$NON-NLS-1$

	/**
	 * Returns the ids of the phases to be performed by this phase set. The order
	 * of the returned ids indicates the order in which the phases will be run.
	 * @return The phase ids.
	 */
	public String[] getPhaseIds();
}
