/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.jface.wizard.IWizardPage;

/**
 * 
 * ISelectableIUsPage is used to get the selected or checked IUs in a
 * wizard page.
 *
 * @since 3.5
 *
 */
public interface ISelectableIUsPage extends IWizardPage {
	public Object[] getCheckedIUElements();

	public Object[] getSelectedIUElements();

	public void setCheckedElements(Object[] elements);
}
