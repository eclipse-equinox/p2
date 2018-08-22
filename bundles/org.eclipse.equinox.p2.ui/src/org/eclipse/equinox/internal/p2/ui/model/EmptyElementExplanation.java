/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;

/**
 * Element class representing an explanation for no children appearing
 * beneath an element.
 * 
 * @since 3.5
 */
public class EmptyElementExplanation extends ProvElement {

	String explanation;
	int severity;
	String description;

	/**
	 * Create an empty element explanation
	 * @param parent the parent of this element
	 * @param severity the severity of the explanation {@link IStatus#INFO}, 
	 * @param explanation
	 */
	public EmptyElementExplanation(Object parent, int severity, String explanation, String description) {
		super(parent);
		this.explanation = explanation;
		this.severity = severity;
		this.description = description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	@Override
	protected String getImageId(Object obj) {
		if (severity == IStatus.ERROR)
			return Dialog.DLG_IMG_MESSAGE_ERROR;
		if (severity == IStatus.WARNING)
			return Dialog.DLG_IMG_MESSAGE_WARNING;
		return Dialog.DLG_IMG_MESSAGE_INFO;
	}

	@Override
	public String getLabel(Object o) {
		return explanation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object o) {
		return new Object[0];
	}

	public String getDescription() {
		return description;
	}
}
