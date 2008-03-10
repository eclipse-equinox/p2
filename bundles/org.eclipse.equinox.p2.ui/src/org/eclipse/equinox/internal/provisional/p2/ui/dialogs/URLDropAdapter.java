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

import org.eclipse.swt.dnd.*;

/**
 * URLDropAdapter can receive URL text from a drop.
 * @since 3.4
 *
 */
public abstract class URLDropAdapter extends DropTargetAdapter {
	public void dragEnter(DropTargetEvent e) {
		if (e.detail == DND.DROP_NONE)
			e.detail = DND.DROP_LINK;
	}

	public void dragOperationChanged(DropTargetEvent e) {
		if (e.detail == DND.DROP_NONE)
			e.detail = DND.DROP_LINK;
	}

	public void drop(DropTargetEvent event) {
		if (event.data == null) {
			event.detail = DND.DROP_NONE;
			return;
		}
		handleURLString((String) event.data);
	}

	protected abstract void handleURLString(String urlText);
}
