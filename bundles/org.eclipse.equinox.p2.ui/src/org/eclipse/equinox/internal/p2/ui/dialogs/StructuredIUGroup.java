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

import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.QueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.IUColumnConfig;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * A StructuredIUGroup is a reusable UI component that displays a
 * structured view of IU's driven by some queries.
 * 
 * @since 3.4
 */
public abstract class StructuredIUGroup {

	private FontMetrics fm;
	protected StructuredViewer viewer;
	private Composite composite;
	private Policy policy;
	private IUColumnConfig[] columnConfig;

	/**
	 * Create a group that represents the available IU's.
	 * 
	 * @param policy The application policy to use in the group
	 * @param parent the parent composite for the group
	 * to retrieve elements in the viewer.
	 * @param font The font to use for calculating pixel sizes.  This font is
	 * not managed by the receiver.
	 * @param columnConfig the columns to be shown
	 */
	protected StructuredIUGroup(Policy policy, Composite parent, Font font, IUColumnConfig[] columnConfig) {
		this.policy = policy;
		if (columnConfig == null)
			this.columnConfig = ProvUI.getIUColumnConfig();
		else
			this.columnConfig = columnConfig;

		// Set up a fontmetrics for calculations
		GC gc = new GC(parent);
		gc.setFont(font);
		fm = gc.getFontMetrics();
		gc.dispose();
	}

	protected void createGroupComposite(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gd);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		viewer = createViewer(composite);

		viewer.getControl().setLayoutData(getViewerGridData());
	}

	protected GridData getViewerGridData() {
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		return data;
	}

	protected abstract StructuredViewer createViewer(Composite parent);

	protected Composite getComposite() {
		return composite;
	}

	protected Shell getShell() {
		return composite.getShell();
	}

	protected StructuredViewer getStructuredViewer() {
		return viewer;
	}

	protected IUColumnConfig[] getColumnConfig() {
		return columnConfig;
	}

	public IInstallableUnit[] getSelectedIUs() {
		return ElementUtils.elementsToIUs(getSelectedIUElements());
	}

	public Object[] getSelectedIUElements() {
		return ((IStructuredSelection) viewer.getSelection()).toArray();
	}

	protected int convertHorizontalDLUsToPixels(int dlus) {
		return Dialog.convertHorizontalDLUsToPixels(fm, dlus);
	}

	protected int convertWidthInCharsToPixels(int dlus) {
		return Dialog.convertWidthInCharsToPixels(fm, dlus);
	}

	protected int convertVerticalDLUsToPixels(int dlus) {
		return Dialog.convertVerticalDLUsToPixels(fm, dlus);
	}

	protected int convertHeightInCharsToPixels(int dlus) {
		return Dialog.convertHeightInCharsToPixels(fm, dlus);
	}

	protected QueryProvider getQueryProvider() {
		return policy.getQueryProvider();
	}

	protected Policy getPolicy() {
		return policy;
	}

	protected Control getDefaultFocusControl() {
		if (viewer != null)
			return viewer.getControl();
		return null;
	}
}
