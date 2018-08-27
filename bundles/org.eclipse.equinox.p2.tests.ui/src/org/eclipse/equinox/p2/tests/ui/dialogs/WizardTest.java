/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.dialogs.ResolutionResultsWizardPage;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.swt.widgets.*;

/**
 * Generic wizard test methods
 */
public abstract class WizardTest extends AbstractProvisioningUITest {

	protected Tree findTree(ResolutionResultsWizardPage page) {
		return findTree(page.getControl());
	}

	protected Tree findTree(Control control) {
		if (control instanceof Tree)
			return (Tree) control;
		if (control instanceof Composite) {
			Control[] children = ((Composite) control).getChildren();
			for (int i = 0; i < children.length; i++) {
				Tree tree = findTree(children[i]);
				if (tree != null)
					return tree;
			}

		}
		return null;
	}
}
