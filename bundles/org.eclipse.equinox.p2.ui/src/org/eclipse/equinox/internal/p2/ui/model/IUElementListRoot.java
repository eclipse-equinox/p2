/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * Element class representing a fixed set of IU's. This element should never
 * appear in a list, but can be used as a parent in a list.
 *
 * @since 3.5
 */
public class IUElementListRoot extends QueriedElement {
	Object[] children;
	private ProvisioningUI ui;

	public IUElementListRoot(Object[] children) {
		super(null);
		this.children = children;
	}

	public IUElementListRoot() {
		this(new Object[0]);
	}

	public IUElementListRoot(ProvisioningUI ui) {
		this(new Object[0]);
		this.ui = ui;
	}

	public void setChildren(Object[] children) {
		this.children = children;
	}

	@Override
	protected String getImageId(Object obj) {
		return null;
	}

	@Override
	public String getLabel(Object o) {
		return null;
	}

	@Override
	public Object[] getChildren(Object o) {
		return children;
	}

	@Override
	protected int getDefaultQueryType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Policy getPolicy() {
		return getProvisioningUI().getPolicy();
	}

	@Override
	public ProvisioningUI getProvisioningUI() {
		if (ui != null) {
			return ui;
		}
		return super.getProvisioningUI();
	}
}
