/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.net.URL;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * PropertyPage that shows an IU's properties
 * 
 * @since 3.4
 */
public abstract class IUPropertyPage extends PropertyPage {

	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		IInstallableUnit iu = ProvUI.getAdapter(getElement(), IInstallableUnit.class);
		Control control;
		if (iu == null) {
			Label label = new Label(parent, SWT.DEFAULT);
			label.setText(ProvUIMessages.IUPropertyPage_NoIUSelected);
			control = label;
		}
		control = createIUPage(parent, iu);
		Dialog.applyDialogFont(parent);
		return control;
	}

	protected int computeWidthLimit(Control control, int nchars) {
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		return Dialog.convertWidthInCharsToPixels(fontMetrics, nchars);
	}

	protected int computeHeightLimit(Control control, int nchars) {
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();
		return Dialog.convertHeightInCharsToPixels(fontMetrics, nchars);
	}

	protected abstract Control createIUPage(Composite parent, IInstallableUnit iu);

	protected void showURL(URL url) {
		IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
		try {
			IWebBrowser browser = support.getExternalBrowser();
			browser.openURL(url);
		} catch (PartInitException e) {
			ProvUI.handleException(e, ProvUIMessages.IUGeneralInfoPropertyPage_CouldNotOpenBrowser, StatusManager.LOG | StatusManager.BLOCK);
		}
	}
}
