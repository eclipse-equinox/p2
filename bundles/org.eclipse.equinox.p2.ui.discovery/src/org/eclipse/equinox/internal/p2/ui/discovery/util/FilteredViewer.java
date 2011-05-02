/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala - bug 187762
 *     Mohamed Tarief - tarief@eg.ibm.com - IBM - Bug 174481
 *     Tasktop Technologies - generalized filter code for structured viewers
 *     Benjamin Muskalla, Tasktop Technologies - bug 342164 
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.discovery.util;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.discovery.wizards.Messages;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Based on {@link org.eclipse.ui.dialogs.FilteredTree}.
 * 
 * @author Steffen Pingel
 */
public abstract class FilteredViewer {

	private boolean automaticFind;

	private Label clearFilterTextControl;

	private Composite container;

	TextSearchControl filterText;

	private int minimumHeight;

	String previousFilterText = ""; //$NON-NLS-1$

	WorkbenchJob refreshJob;

	private long refreshJobDelay = 200L;

	private PatternFilter searchFilter;

	protected StructuredViewer viewer;

	private Composite header;

	public FilteredViewer() {
		setAutomaticFind(true);
	}

	void clearFilterText() {
		filterText.getTextControl().setText(""); //$NON-NLS-1$
		filterTextChanged();
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(container);
		container.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (refreshJob != null) {
					refreshJob.cancel();
				}
			}
		});

		doCreateHeader();

		viewer = doCreateViewer(container);
		searchFilter = doCreateFilter();
		viewer.addFilter(searchFilter);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, minimumHeight).applyTo(viewer.getControl());
	}

	protected PatternFilter doCreateFilter() {
		return new PatternFilter() {
			@Override
			protected boolean isParentMatch(Viewer viewer, Object element) {
				return false;
			}
		};
	}

	private void doCreateFindControl(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.ConnectorDiscoveryWizardMainPage_filterLabel);
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

		filterText = new TextSearchControl(parent, automaticFind);
		if (automaticFind) {
			filterText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					filterTextChanged();
				}
			});
		} else {
			filterText.getTextControl().addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent e) {
					if (e.detail == SWT.TRAVERSE_RETURN) {
						e.doit = false;
						filterTextChanged();
					}
				}
			});
		}
		filterText.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if (e.detail == SWT.ICON_CANCEL) {
					clearFilterText();
				} else {
					// search icon and enter
					filterTextChanged();
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(filterText);
	}

	private void doCreateHeader() {
		header = new Composite(container, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(header);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(header);

		doCreateFindControl(header);
		doCreateHeaderControls(header);

		// arrange all header controls horizontally
		GridLayoutFactory.fillDefaults().numColumns(header.getChildren().length).applyTo(header);
	}

	protected void doCreateHeaderControls(Composite parent) {
		// ignore
	}

	public void setHeaderVisible(boolean visible) {
		if (header != null && visible != header.getVisible()) {
			header.setVisible(visible);
			GridData headerLayout = (GridData) header.getLayoutData();
			headerLayout.exclude = !visible;
			container.layout(true, true);
		}
	}

	public boolean isHeaderVisible() {
		return header != null && header.getVisible();
	}

	protected WorkbenchJob doCreateRefreshJob() {
		return new WorkbenchJob("filter") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (filterText.isDisposed()) {
					return Status.CANCEL_STATUS;
				}
				String text = filterText.getTextControl().getText();
				text = text.trim();

				if (!previousFilterText.equals(text)) {
					previousFilterText = text;
					doFind(text);
				}
				return Status.OK_STATUS;
			}
		};
	}

	protected abstract StructuredViewer doCreateViewer(Composite parent);

	protected void doFind(String text) {
		searchFilter.setPattern(text);
		if (clearFilterTextControl != null) {
			clearFilterTextControl.setVisible(text != null && text.length() != 0);
		}
		viewer.refresh(true);
	}

	/**
	 * Invoked whenever the filter text is changed or the user otherwise causes the filter text to change.
	 */
	protected void filterTextChanged() {
		if (refreshJob == null) {
			refreshJob = doCreateRefreshJob();
		} else {
			refreshJob.cancel();
		}
		refreshJob.schedule(refreshJobDelay);
	}

	/**
	 * Provides the text string of the search widget.
	 */
	protected String getFilterText() {
		return filterText == null ? null : filterText.getTextControl().getText();
	}

	protected void setFilterText(String newFilter) {
		if (filterText == null) {
			throw new IllegalStateException("Needs be invoked after controls are created"); //$NON-NLS-1$
		}
		filterText.getTextControl().setText(newFilter);
	}

	public Control getControl() {
		return container;
	}

	public int getMinimumHeight() {
		return minimumHeight;
	}

	protected long getRefreshJobDelay() {
		return refreshJobDelay;
	}

	public StructuredViewer getViewer() {
		return viewer;
	}

	public void setMinimumHeight(int minimumHeight) {
		this.minimumHeight = minimumHeight;
		if (viewer != null) {
			GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, minimumHeight).applyTo(viewer.getControl());
		}
	}

	protected void setRefreshJobDelay(long refreshJobDelay) {
		this.refreshJobDelay = refreshJobDelay;
	}

	public final void setAutomaticFind(boolean automaticFind) {
		if (filterText != null) {
			throw new IllegalStateException("setAutomaticFind() needs be invoked before controls are created"); //$NON-NLS-1$
		}
		this.automaticFind = automaticFind;
	}

	public final boolean isAutomaticFind() {
		return automaticFind;
	}

}
