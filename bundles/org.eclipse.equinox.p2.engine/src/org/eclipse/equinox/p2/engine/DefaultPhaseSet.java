/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.p2.engine.phases.*;

/**
 * @since 2.0
 */
public class DefaultPhaseSet extends PhaseSet {

	private static final boolean forcedUninstall = Boolean.valueOf(EngineActivator.getContext().getProperty("org.eclipse.equinox.p2.engine.forcedUninstall")).booleanValue(); //$NON-NLS-1$

	public static int PHASE_COLLECT = 0x02;
	public static int PHASE_UNCONFIGURE = 0x20;
	public static int PHASE_UNINSTALL = 0x40;
	public static int PHASE_PROPERTY = 0x10;
	public static int PHASE_CHECK_TRUST = 0x01;
	public static int PHASE_INSTALL = 0x08;
	public static int PHASE_CONFIGURE = 0x04;

	public DefaultPhaseSet() {
		this(new Phase[] {new Collect(100), new Unconfigure(10, forcedUninstall), new Uninstall(50, forcedUninstall), new Property(1), new CheckTrust(10), new Install(50), new Configure(10)});
	}

	private DefaultPhaseSet(Phase[] phases) {
		super(phases);
	}

	/**
	 * Creates a default phase set that covers all the provisioning operations.
	 * Phases can be specified for exclusion.
	 * 
	 * @param exclude - A set of bit options that specify the phases to exclude.
	 * See {@link DefaultPhaseSet} for possible options
	 * @return the {@link PhaseSet}
	 */
	public static final PhaseSet createDefaultPhaseSet(int exclude) {
		ArrayList<Phase> phases = new ArrayList<Phase>();
		if ((PHASE_COLLECT & exclude) != PHASE_COLLECT)
			phases.add(new Collect(100));
		if ((PHASE_UNCONFIGURE & exclude) != PHASE_UNCONFIGURE)
			phases.add(new Unconfigure(10, forcedUninstall));
		if ((PHASE_UNINSTALL & exclude) != PHASE_UNINSTALL)
			phases.add(new Uninstall(50, forcedUninstall));
		if ((PHASE_PROPERTY & exclude) != PHASE_PROPERTY)
			phases.add(new Property(1));
		if ((PHASE_CHECK_TRUST & exclude) != PHASE_CHECK_TRUST)
			phases.add(new CheckTrust(10));
		if ((PHASE_INSTALL & exclude) != PHASE_INSTALL)
			phases.add(new Install(50));
		if ((PHASE_CONFIGURE & exclude) != PHASE_CONFIGURE)
			phases.add(new Configure(10));
		return new DefaultPhaseSet(phases.toArray(new Phase[phases.size()]));
	}
}
