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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.equinox.p2.ui.ICopyable;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;

public class CopyHandler extends AbstractHandler {

	public static final String ID = "org.eclipse.ui.edit.copy"; //$NON-NLS-1$
	ICopyable copySource;

	public CopyHandler(ICopyable copyable) {
		this.copySource = copyable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) {
		Object control = HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
		if (control instanceof Control)
			copySource.copyToClipboard((Control) control);
		return null;
	}
}