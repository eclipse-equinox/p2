/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
			 */
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				// Ignore because the selection won't change 
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
			 */
			public ISelection getSelection() {
				return new StructuredSelection(selections);
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
			 */
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				// ignore because the selection is static
			}

			/* (non-Javadoc)
			 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
			 */
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
