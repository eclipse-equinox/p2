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

package org.eclipse.equinox.internal.p2.ui.viewers;

import java.util.EventListener;
import org.eclipse.jface.viewers.Viewer;

/**
 * A listening interface used to signal clients when input changes
 * in a viewer.
 * 
 * @since 3.4
 *
 */
public interface IInputChangeListener extends EventListener {
	public void inputChanged(Viewer v, Object oldInput, Object newInput);
}
