/*******************************************************************************
 * Copyright (c) 2023 Spirent Communications and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *		Vasili Gulevich (Spirent Communications) - initial implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import java.util.Objects;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * UpdatesPopup is an async popup dialog for notifying the user of updates.
 * 
 * @since 4.28
 */
class UpdatesPopup extends PopupDialog {
	private static final int POPUP_OFFSET = 20;

	protected Composite dialogArea;
	private final MouseListener clickListener;

	private final String message;

	public UpdatesPopup(Shell parentShell, String message) {
		super(parentShell, PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE | SWT.MODELESS, false, true, true, false, false,
				AutomaticUpdateMessages.AutomaticUpdatesPopup_UpdatesAvailableTitle, null);
		this.message = Objects.requireNonNull(message);
		clickListener = MouseListener
				.mouseDownAdapter(e -> AutomaticUpdatePlugin.getDefault().getAutomaticUpdater().launchUpdate());
	}

	@Override
	protected Composite createDialogArea(Composite parent) {
		dialogArea = new Composite(parent, SWT.NONE);
		dialogArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		dialogArea.setLayout(layout);
		dialogArea.addMouseListener(clickListener);

		// The "click to update" label
		Label infoLabel = new Label(dialogArea, SWT.NONE);
		infoLabel.setText(message);
		infoLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
		infoLabel.addMouseListener(clickListener);

		return dialogArea;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(AutomaticUpdateMessages.AutomaticUpdatesPopup_UpdatesAvailableTitle);
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Shell parent = getParentShell();
		Point parentSize, parentLocation;

		if (parent != null) {
			parentSize = parent.getSize();
			parentLocation = parent.getLocation();
		} else {
			Rectangle bounds = getShell().getDisplay().getBounds();
			parentSize = new Point(bounds.width, bounds.height);
			parentLocation = new Point(0, 0);
		}
		// We have to take parent location into account because SWT considers all
		// shell locations to be in display coordinates, even if the shell is parented.
		return new Point(parentSize.x - initialSize.x + parentLocation.x - POPUP_OFFSET,
				parentSize.y - initialSize.y + parentLocation.y - POPUP_OFFSET);
	}

	/*
	 * Overridden so that clicking in the title menu area closes the dialog. Also
	 * creates a close box menu in the title area.
	 */
	@Override
	protected Control createTitleMenuArea(Composite parent) {
		Composite titleComposite = (Composite) super.createTitleMenuArea(parent);
		titleComposite.addMouseListener(clickListener);

		ToolBar toolBar = new ToolBar(titleComposite, SWT.FLAT);
		ToolItem closeButton = new ToolItem(toolBar, SWT.PUSH, 0);

		GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(toolBar);
		closeButton.setImage(
				AutomaticUpdatePlugin.getDefault().getImageRegistry().get((AutomaticUpdatePlugin.IMG_TOOL_CLOSE)));
		closeButton.setHotImage(
				AutomaticUpdatePlugin.getDefault().getImageRegistry().get((AutomaticUpdatePlugin.IMG_TOOL_CLOSE_HOT)));
		closeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> close()));
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=177183
		toolBar.addMouseListener(MouseListener.mouseDownAdapter(e -> close()));
		return titleComposite;
	}

	/*
	 * Overridden to adjust the span of the title label. Reachy, reachy....
	 */
	@Override
	protected Control createTitleControl(Composite parent) {
		Control control = super.createTitleControl(parent);
		Object data = control.getLayoutData();
		if (data instanceof GridData) {
			((GridData) data).horizontalSpan = 1;
		}
		return control;
	}

}
