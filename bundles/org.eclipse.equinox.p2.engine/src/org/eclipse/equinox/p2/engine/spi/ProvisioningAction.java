/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine.spi;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;

/**
 * @since 2.0
 */
public abstract class ProvisioningAction {

	private Memento memento = new Memento();
	private Touchpoint touchpoint;

	protected Memento getMemento() {
		return memento;
	}

	public abstract IStatus execute(Map<String, Object> parameters);

	public abstract IStatus undo(Map<String, Object> parameters);

	// TODO: these probably should not be visible
	public void setTouchpoint(Touchpoint touchpoint) {
		this.touchpoint = touchpoint;
	}

	public Touchpoint getTouchpoint() {
		return touchpoint;
	}
}
