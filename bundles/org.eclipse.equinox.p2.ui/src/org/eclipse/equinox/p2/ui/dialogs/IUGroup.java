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
package org.eclipse.equinox.p2.ui.dialogs;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;

/**
 * An IUGroup is a reusable UI component that displays properties of an IU. It can be used in 
 * different dialogs that manipulate or define IU's.
 * 
 * @since 3.4
 */
public abstract class IUGroup {

	protected IInstallableUnit iu;
	private Composite composite;

	IUGroup(final Composite parent, IInstallableUnit iu, ModifyListener listener) {
		this.iu = iu;
		composite = createGroupComposite(parent, listener);
	}

	protected abstract Composite createGroupComposite(Composite parent, ModifyListener modifyListener);

	public Composite getComposite() {
		return composite;
	}

	public IInstallableUnit getIU() {
		return iu;
	}

	public void updateIU() {
		// default is to do nothing
	}
}
