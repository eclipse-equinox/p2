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
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUI;
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

	protected Object iuElement;
	private Composite composite;

	protected IUGroup(final Composite parent, Object iu, ModifyListener listener) {
		this.iuElement = iu;
		composite = createGroupComposite(parent, listener);
	}

	protected abstract Composite createGroupComposite(Composite parent, ModifyListener modifyListener);

	public Composite getComposite() {
		return composite;
	}

	public IInstallableUnit getIU() {
		return ProvUI.getAdapter(iuElement, IInstallableUnit.class);
	}

	public void updateIU() {
		// default is to do nothing
	}
}
