/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     Ericsson AB (Pascal Rapicault) - Bug 387115 - Allow to export everything
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal.wizard;

import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.importexport.internal.Messages;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.model.ProfileElement;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class ExportPage extends AbstractPage {
	public ExportPage(String pageName) {
		super(pageName);
		setTitle(Messages.ExportPage_Title);
		setDescription(Messages.ExportPage_Description);
	}

	@Override
	protected void createContents(Composite composite) {
		Label label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		if (getSelfProfile() == null) {
			label.setText(Messages.ExportPage_ERROR_CONFIG);
			setPageComplete(false);
		} else {
			label.setText(Messages.ExportPage_Label);

			createInstallationTable(composite);
			createDestinationGroup(composite, true);
		}
	}

	@Override
	public void doFinish() throws Exception {
		finishException = null;
		if (viewer == null)
			return;
		// about to invoke the operation so save our state
		saveWidgetValues();
		final Object[] checked = viewer.getCheckedElements();
		final boolean includeAllEntries = includeAllButton.getSelection();
		OutputStream stream = null;
		try {
			File target = new File(ExportPage.this.destinationNameField.getText());
			if (!target.exists())
				target.createNewFile();
			stream = new BufferedOutputStream(new FileOutputStream(target));
			final OutputStream out = stream;
			getContainer().run(true, true, new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) throws InterruptedException {
					try {
						IInstallableUnit[] units = new IInstallableUnit[checked.length];
						for (int i = 0; i < units.length; i++)
							units[i] = ProvUI.getAdapter(checked[i], IInstallableUnit.class);
						IStatus status = importexportService.exportP2F(out, units, includeAllEntries, monitor);
						if (status.isMultiStatus()) {
							final StringBuilder sb = new StringBuilder();
							for (IStatus child : status.getChildren()) {
								if (child.isMultiStatus()) {
									for (IStatus grandchild : child.getChildren())
										sb.append("<li>").append(grandchild.getMessage()).append("</li>"); //$NON-NLS-1$ //$NON-NLS-2$
								} else if (child.isOK())
									sb.insert(0, Messages.ExportPage_SuccessWithProblems);
								else {
									sb.insert(0, Messages.ExportPage_Fail);
									sb.append(status.getMessage());
								}
							}
							sb.append(Messages.ExportPage_FixSuggestion);
							sb.append(Messages.ExportPage_TryAgainQuestion);
							Runnable runnable = new Runnable() {

								public void run() {
									String title = Messages.ExportPage_Title;
									tryAgain = StyledErrorDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), title, sb.toString());
								}
							};
							Display.getDefault().syncExec(runnable);
							if (tryAgain)
								throw new OperationCanceledException("User chosed try again."); //$NON-NLS-1$
						}
					} catch (OperationCanceledException e) {
						throw new InterruptedException(e.getMessage());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				boolean tryAgain = false;
			});
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// do nothing
				}
			}
			if (finishException != null)
				throw finishException;
		}
	}

	@Override
	protected String getDialogTitle() {
		return Messages.ExportPage_FILEDIALOG_TITLE;
	}

	@Override
	protected Object getInput() {
		ProfileElement element = new ProfileElement(null, getSelfProfile().getProfileId());
		return element;
	}

	@Override
	protected String getInvalidDestinationMessage() {
		return Messages.ExportPage_DEST_ERRORMESSAGE;
	}

	@Override
	protected void giveFocusToDestination() {
		if (viewer != null)
			viewer.getControl().setFocus();
	}

	@Override
	protected String getDestinationLabel() {
		return Messages.ExportPage_LABEL_EXPORTFILE;
	}

	@Override
	protected int getBrowseDialogStyle() {
		return SWT.SAVE;
	}

	@Override
	protected void handleDestinationChanged(String newDestination) {
		updatePageCompletion();
	}
}
