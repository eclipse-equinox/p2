/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.actions;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Control;

/**
 * @since 3.4
 *
 */
public abstract class RefreshAction extends ProvisioningAction {

	/**
	 */
	public RefreshAction(ProvisioningUI ui, ISelectionProvider selectionProvider, Control control) {
		super(ui, ProvUIMessages.RefreshAction_Label, selectionProvider);
		setToolTipText(ProvUIMessages.RefreshAction_Tooltip);
		hookKeyListener(control);
		init();
	}

	private void hookKeyListener(Control control) {
		control.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				handleKeyReleased(e);
			}
		});
	}

	public void run() {
		refresh();
	}

	protected abstract void refresh();

	/**
	 * Handle a key released event.  Used internally and also
	 * made available so that clients can watch key events from
	 * any other controls and dispatch to this action.
	 * 
	 * @param event the key event
	 */
	public void handleKeyReleased(KeyEvent event) {
		if (event.keyCode == SWT.F5 && event.stateMask == 0) {
			refresh();
		}
	}
}
