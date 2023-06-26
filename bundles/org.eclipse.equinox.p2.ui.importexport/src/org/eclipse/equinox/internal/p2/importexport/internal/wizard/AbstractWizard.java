/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal.wizard;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.importexport.internal.Constants;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;

public abstract class AbstractWizard extends Wizard {

	protected AbstractPage mainPage = null;

	public AbstractWizard() {
		super();
	}

	@Override
	public boolean performFinish() {
		try {
			mainPage.doFinish();
		} catch (InterruptedException e) {
			// cancelled by user
			return false;
		} catch (Exception e) {
			ILog.of(Platform.getBundle(Constants.Bundle_ID)).log(new Status(IStatus.ERROR, Constants.Bundle_ID, e.getMessage(), e));
			MessageBox messageBox = new MessageBox(this.getShell(), SWT.ICON_ERROR);
			messageBox.setMessage(e.getMessage() == null ? "Unknown error" : e.getMessage()); //$NON-NLS-1$
			messageBox.open();
			return false;
		}
		return true;
	}

}