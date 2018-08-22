/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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


import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Text;

/**
 * @since 3.4
 *
 */
public class TextURLDropAdapter extends URLDropAdapter {

	Text text;

	public TextURLDropAdapter(Text text, boolean convertFileToURL) {
		super(convertFileToURL);
		this.text = text;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.dialogs.URLDropAdapter#handleURLString(java.lang.String, org.eclipse.swt.dnd.DropTargetEvent)
	 */
	@Override
	protected void handleDrop(String urlText, DropTargetEvent event) {
		text.setText(urlText);
		event.detail = DND.DROP_LINK;
	}

}
