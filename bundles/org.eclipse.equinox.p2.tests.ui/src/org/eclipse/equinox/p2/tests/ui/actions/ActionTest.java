/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.actions;

import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.jface.viewers.*;

/**
 * Abstract class for UI action tests
 */
public abstract class ActionTest extends AbstractProvisioningUITest {
	protected ISelectionProvider getSelectionProvider(final Object[] selections) {

		return new ISelectionProvider() {

			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				// Ignore because the selection won't change 
			}

			@Override
			public ISelection getSelection() {
				return new StructuredSelection(selections);
			}

			@Override
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				// ignore because the selection is static
			}

			@Override
			public void setSelection(ISelection sel) {
				throw new UnsupportedOperationException("This ISelectionProvider is static, and cannot be modified."); //$NON-NLS-1$
			}
		};
	}

	protected Object[] getEmptySelection() {
		return new Object[0];
	}

	protected Object[] getInvalidSelection() {
		return new Object[] {new Object(), new Object()};
	}

}
