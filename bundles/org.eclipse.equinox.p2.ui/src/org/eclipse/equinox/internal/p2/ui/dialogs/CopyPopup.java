/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.p2.ui.ICopyable;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.*;

public class CopyPopup {

	ICopyable copySource;
	Control control;

	public CopyPopup(ICopyable copyable, final Control control) {
		this.copySource = copyable;
		this.control = control;
		Menu copyMenu = new Menu(control);
		MenuItem copyItem = new MenuItem(copyMenu, SWT.NONE);
		copyItem.addSelectionListener(new SelectionListener() {
			/*
			 * @see SelectionListener.widgetSelected (SelectionEvent)
			 */
			public void widgetSelected(SelectionEvent e) {
				copySource.copyToClipboard(control);
			}

			/*
			 * @see SelectionListener.widgetDefaultSelected(SelectionEvent)
			 */
			public void widgetDefaultSelected(SelectionEvent e) {
				copySource.copyToClipboard(control);
			}
		});
		copyItem.setText(JFaceResources.getString("copy")); //$NON-NLS-1$
		control.setMenu(copyMenu);
	}
}