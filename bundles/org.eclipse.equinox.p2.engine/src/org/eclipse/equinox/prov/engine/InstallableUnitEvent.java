/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.prov.engine;

import java.util.EventObject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class InstallableUnitEvent extends EventObject {
	private static final long serialVersionUID = 3318712818811459886L;

	private String phaseId;
	private boolean prePhase;

	private Profile profile;
	private Operand operand;
	private ITouchpoint touchpoint;
	private IStatus result;

	public InstallableUnitEvent(String phaseId, boolean prePhase, Profile profile, Operand operand, ITouchpoint touchpoint) {
		this(phaseId, prePhase, profile, operand, touchpoint, null);
	}

	public InstallableUnitEvent(String phaseId, boolean prePhase, Profile profile, Operand operand, ITouchpoint touchpoint, IStatus result) {
		super(touchpoint); //TODO not sure if the touchpoint should be the source
		this.phaseId = phaseId;
		this.prePhase = prePhase;
		this.profile = profile;
		this.operand = operand;
		this.result = result;
	}

	public ITouchpoint getTouchpoint() {
		return touchpoint;
	}

	public Profile getProfile() {
		return profile;
	}

	public Operand getOperand() {
		return operand;
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
}
