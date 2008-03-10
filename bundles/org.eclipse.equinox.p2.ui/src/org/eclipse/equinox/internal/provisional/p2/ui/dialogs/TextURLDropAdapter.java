/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import org.eclipse.swt.widgets.Text;

/**
 * @since 3.4
 *
 */
public class TextURLDropAdapter extends URLDropAdapter {

	Text text;

	public TextURLDropAdapter(Text text) {
		this.text = text;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.dialogs.URLDropAdapter#handleURLString(java.lang.String)
	 */
	protected void handleURLString(String urlText) {
		text.setText(urlText);
	}

}
