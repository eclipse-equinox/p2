/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.*;
import org.eclipse.equinox.internal.p2.engine.*;

/**
 * @since 2.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class PhaseSetFactory {

	/**
	 * A phase id (value "checkTrust") describing the certificate trust check phase.
	 * This phase examines the code signing certificates of the artifacts being installed
	 * to ensure they are signed and trusted by the running system.
	 */
	public static final String PHASE_CHECK_TRUST = "checkTrust"; //$NON-NLS-1$
	/**
	 * A phase id (value "collect") describing the collect phase.
	 * This phase gathers all the artifacts to be installed, typically by copying them
	 * from some repository into a suitable local location for the application being installed.
	 */
	public static final String PHASE_COLLECT = "collect"; //$NON-NLS-1$
	/**
	 * A phase id (value "configure") describing the configuration phase.
	 * This phase writes configuration data related to the software being provisioned.
	 * Until configuration occurs the end user of the software will be have access to
	 * the installed functionality.
	 */
	public static final String PHASE_CONFIGURE = "configure"; //$NON-NLS-1$
	/**
	 * A phase id (value "install") describing the install phase.
	 * This phase performs any necessary transformations on the downloaded
	 * artifacts to put them in the correct shape for the running application, such
	 * as decompressing or moving content, setting file permissions, etc).
	 */
	public static final String PHASE_INSTALL = "install"; //$NON-NLS-1$
	/**
	 * A phase id (value "property") describing the property modification phase.
	 * This phase performs changes to profile properties.
	 */
	public static final String PHASE_PROPERTY = "property"; //$NON-NLS-1$
	/**
	 * A phase id (value "unconfigure") describing the unconfigure phase.
	 * This phase removes configuration data related to the software being removed.
	 * This phase is the inverse of the changes performed in the configure phase.
	 */
	public static final String PHASE_UNCONFIGURE = "unconfigure"; //$NON-NLS-1$
	/**
	 * A phase id (value "uninstall") describing the uninstall phase.
	 * This phase removes artifacts from the system being provisioned that are
	 * no longer required in the new profile.
	 */
	public static final String PHASE_UNINSTALL = "uninstall"; //$NON-NLS-1$

	/**
	 * @since 2.10
	 */
	public static final String NATIVE_ARTIFACTS = "nativeArtifacts"; //$NON-NLS-1$

	private static final List<String> ALL_PHASES_LIST = Arrays.asList(new String[] {PHASE_COLLECT, PHASE_UNCONFIGURE, PHASE_UNINSTALL, PHASE_PROPERTY, PHASE_CHECK_TRUST, PHASE_INSTALL, PHASE_CONFIGURE});

	/**
	 * Creates a default phase set that covers all the provisioning operations.
	 * Phases can be specified for exclusion.
	 *
	 * @param exclude - A set of bit options that specify the phases to exclude.
	 * See {@link PhaseSetFactory} for possible options
	 * @return the {@link PhaseSet}
	 */
	public static final IPhaseSet createDefaultPhaseSetExcluding(String[] exclude) {
		if (exclude == null || exclude.length == 0)
			return createDefaultPhaseSet();
		List<String> excludeList = Arrays.asList(exclude);
		List<String> includeList = new ArrayList<>(ALL_PHASES_LIST);
		includeList.removeAll(excludeList);
		return createPhaseSetIncluding(includeList.toArray(new String[includeList.size()]));
	}

	/**
	 * Creates a default phase set that covers all the provisioning operations.
	 * Phases can be specified for inclusion.
	 *
	 * @param include - A set of bit options that specify the phases to include. See
	 *                {@link PhaseSetFactory} for possible options
	 * @return the {@link PhaseSet}
	 */
	public static final IPhaseSet createPhaseSetIncluding(String[] include) {
		if (include == null || include.length == 0) {
			return new PhaseSet(new Phase[0]);
		}
		return new PhaseSet(include);
	}

	public static IPhaseSet createDefaultPhaseSet() {
		return createPhaseSetIncluding(ALL_PHASES_LIST.toArray(String[]::new));
	}

	public static ISizingPhaseSet createSizingPhaseSet() {
		return new SizingPhaseSet();
	}
}
