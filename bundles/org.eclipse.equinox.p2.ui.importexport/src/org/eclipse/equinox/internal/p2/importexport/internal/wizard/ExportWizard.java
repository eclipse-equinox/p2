/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal.wizard;

import java.io.File;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.importexport.internal.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

public class ExportWizard extends AbstractWizard implements IExportWizard {

	public ExportWizard() {
		IDialogSettings workbenchSettings = ImportExportActivator.getDefault().getDialogSettings();
		String sectionName = "ExportWizard"; //$NON-NLS-1$
		IDialogSettings section = workbenchSettings.getSection(sectionName);
		if (section == null) {
			section = workbenchSettings.addNewSection(sectionName);
		}
		setDialogSettings(section);
	}

	@Override
	public void addPages() {
		super.addPages();
		mainPage = new ExportPage("mainPage"); //$NON-NLS-1$
		addPage(mainPage);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.ExportWizard_WizardTitle);
		setDefaultPageImageDescriptor(ImageDescriptor.createFromURL(Platform.getBundle(Constants.Bundle_ID).getEntry("icons/install_wiz.gif"))); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		File file = new File(((ExportPage) mainPage).getDestinationValue());
		if (file.exists()) {
			if (!MessageDialog.openConfirm(this.getShell(), Messages.ExportWizard_ConfirmDialogTitle, NLS.bind(Messages.ExportWizard_OverwriteConfirm, file.getAbsolutePath())))
				return false;
		}
		return super.performFinish();
	}
}
