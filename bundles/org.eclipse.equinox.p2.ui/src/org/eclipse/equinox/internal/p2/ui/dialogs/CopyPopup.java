/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
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
		copyItem.setImage(ProvUIImages.getImage(ProvUIImages.IMG_COPY));
		copyItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				copySource.copyToClipboard(control);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				copySource.copyToClipboard(control);
			}
		});
		copyItem.setText(JFaceResources.getString("copy")); //$NON-NLS-1$
		control.setMenu(copyMenu);
	}
}