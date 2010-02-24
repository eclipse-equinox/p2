/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import org.eclipse.equinox.p2.engine.IProfile;

import java.util.EventObject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * @since 2.0
 */
public class InstallableUnitEvent extends EventObject {
	public static final int UNINSTALL = 0;
	public static final int INSTALL = 1;
	private static final long serialVersionUID = 3318712818811459886L;

	private String phaseId;
	private boolean prePhase;

	private IProfile profile;
	private IInstallableUnit iu;
	private Touchpoint touchpoint;
	private IStatus result;
	private int type;

	public InstallableUnitEvent(String phaseId, boolean prePhase, IProfile profile, IInstallableUnit iu, int type, Touchpoint touchpoint) {
		this(phaseId, prePhase, profile, iu, type, touchpoint, null);
	}

	public InstallableUnitEvent(String phaseId, boolean prePhase, IProfile profile, IInstallableUnit iu, int type, Touchpoint touchpoint, IStatus result) {
		super(profile);
		this.phaseId = phaseId;
		this.prePhase = prePhase;
		this.profile = profile;
		this.iu = iu;
		if (type != UNINSTALL && type != INSTALL)
			throw new IllegalArgumentException(Messages.InstallableUnitEvent_type_not_install_or_uninstall);
		this.type = type;
		this.result = result;
		this.touchpoint = touchpoint;

	}

	public Touchpoint getTouchpoint() {
		return touchpoint;
	}

	public IProfile getProfile() {
		return profile;
	}

	public IInstallableUnit getInstallableUnit() {
		return iu;
	}

	public String getPhase() {
		return phaseId;
	}

	public boolean isPre() {
		return prePhase;
	}

	public boolean isPost() {
		return !prePhase;
	}

	public IStatus getResult() {
		return (result != null ? result : Status.OK_STATUS);
	}

	public boolean isInstall() {
		return type == INSTALL;
	}

	public boolean isUninstall() {
		return type == UNINSTALL;
	}
}
