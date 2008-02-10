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
package org.eclipse.equinox.internal.provisional.p2.engine;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;

public abstract class ProvisioningAction {

	private Memento memento = new Memento();

	protected Memento getMemento() {
		return memento;
	}

	public abstract IStatus execute(Map parameters);

	public abstract IStatus undo(Map parameters);
}
