/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.updates;

import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.dialogs.UpdateDialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * AutomaticUpdatesPopup is an async popup dialog for notifying
 * the user of updates.
 * 
 * @since 3.4
 */
public class AutomaticUpdatesPopup extends PopupDialog {
	private static final int CHAR_HEIGHT = 5;
	private static final int CHAR_WIDTH = 40;
	private static final String DIALOG_SETTINGS_SECTION = "AutomaticUpdatesPopup"; //$NON-NLS-1$
	IInstallableUnit[] toUpdate;
	Profile profile;
	boolean downloaded;

	public AutomaticUpdatesPopup(IInstallableUnit[] toUpdate, Profile profile, boolean alreadyDownloaded) {
		super((Shell) null, PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE | SWT.MODELESS, true, true, false, false, ProvSDKMessages.AutomaticUpdatesDialog_UpdatesAvailableTitle, null);
		downloaded = alreadyDownloaded;
		this.profile = profile;
		this.toUpdate = toUpdate;
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		Link infoLink = new Link(parent, SWT.MULTI | SWT.WRAP);
		if (downloaded)
			infoLink.setText(ProvSDKMessages.AutomaticUpdatesDialog_ClickToReviewDownloaded);
		else
			infoLink.setText(ProvSDKMessages.AutomaticUpdatesDialog_ClickToReviewNotDownloaded);
		infoLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				new UpdateDialog(null, toUpdate, profile).open();
			}
		});
		infoLink.setLayoutData(new GridData(GridData.FILL_BOTH));
		return composite;

	}

	protected Point getInitialSize() {
		return charToPixels(new Point(CHAR_WIDTH, CHAR_HEIGHT));
	}

	private Point charToPixels(Point charBounds) {
		int x = charBounds.x;
		int y = charBounds.y;
		GC gc = new GC(getContents());
		gc.setFont(JFaceResources.getDialogFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		x = x * fontMetrics.getAverageCharWidth();
		y = y * fontMetrics.getHeight();
		gc.dispose();
		return new Point(x, y);
	}

	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = ProvSDKUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section == null) {
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
		}
		return section;
	}
}
