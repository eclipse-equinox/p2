/*******************************************************************************
 *  Copyright (c) 2015 Red Hat Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Mickael Istria (Red Hat Inc.) - 483644 Improve "No updates found" dialog
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

public class MessageDialogWithLink extends MessageDialog {

	protected String linkMessage;
	protected Link link;
	protected List<SelectionListener> linkListeners = new ArrayList<SelectionListener>();

	public MessageDialogWithLink(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
		this.message = null;
		this.linkMessage = dialogMessage;
	}

	public MessageDialogWithLink(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, int defaultIndex, String... dialogButtonLabels) {
		this(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
	}

	@Override
	protected Control createMessageArea(Composite composite) {
		super.createMessageArea(composite);
		// create message
		if (linkMessage != null) {
			this.link = new Link(composite, getMessageLabelStyle());
			this.link.setText(this.linkMessage);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).hint(convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH), SWT.DEFAULT).applyTo(this.link);
			for (SelectionListener linkListener : this.linkListeners) {
				this.link.addSelectionListener(linkListener);
			}
		}
		return composite;
	}

	public void addSelectionListener(SelectionListener listener) {
		if (link != null && !link.isDisposed()) {
			link.addSelectionListener(listener);
		}
		this.linkListeners.add(listener);
	}

	public void removeSelectionListener(SelectionListener listener) {
		if (link != null && !link.isDisposed()) {
			link.removeSelectionListener(listener);
		}
		this.linkListeners.add(listener);
	}

}
