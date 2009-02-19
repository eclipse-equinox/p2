/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.swt.widgets.Control;

/**
 * ICopyable defines an interface for UI elements that provide
 * copy support.
 * 
 * @since 3.5
 */
public interface ICopyable {
	public void copyToClipboard(Control activeControl);
}
