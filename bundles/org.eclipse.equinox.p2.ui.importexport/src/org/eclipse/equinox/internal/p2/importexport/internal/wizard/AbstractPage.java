/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     IBM Corporation - Ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal.wizard;

import java.io.File;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.importexport.P2ImportExport;
import org.eclipse.equinox.internal.p2.importexport.internal.Constants;
import org.eclipse.equinox.internal.p2.importexport.internal.Messages;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.ILayoutConstants;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractPage extends WizardPage implements Listener {

	protected String currentMessage;
	protected Button destinationBrowseButton;
	protected Combo destinationNameField;
	protected P2ImportExport importexportService = null;
	protected CheckboxTableViewer viewer = null;
	protected Exception finishException;
	protected static IProfileRegistry profileRegistry = null;
	protected static IProvisioningAgent agent = null;

	class TableViewerComparator extends ViewerComparator {
		private int sortColumn = 0;
		private int lastSortColumn = 0;
		private boolean ascending = false;
		private boolean lastAscending = false;

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			IInstallableUnit iu1 = ProvUI.getAdapter(e1, IInstallableUnit.class);
			IInstallableUnit iu2 = ProvUI.getAdapter(e2, IInstallableUnit.class);
			if (iu1 != null && iu2 != null) {
				if (viewer instanceof TableViewer) {
					TableViewer tableViewer = (TableViewer) viewer;
					IBaseLabelProvider baseLabel = tableViewer.getLabelProvider();
					if (baseLabel instanceof ITableLabelProvider) {
						ITableLabelProvider tableProvider = (ITableLabelProvider) baseLabel;
						String e1p = tableProvider.getColumnText(e1, getSortColumn());
						String e2p = tableProvider.getColumnText(e2, getSortColumn());
						@SuppressWarnings("unchecked")
						int result = getComparator().compare(e1p, e2p);
						// Secondary column sort
						if (result == 0) {
							e1p = tableProvider.getColumnText(e1, lastSortColumn);
							e2p = tableProvider.getColumnText(e2, lastSortColumn);
							@SuppressWarnings("unchecked")
							int result2 = getComparator().compare(e1p, e2p);
							return lastAscending ? result2 : (-1) * result2;
						}
						return isAscending() ? result : (-1) * result;
					}
				}
				// we couldn't determine a secondary sort, call it equal
				return 0;
			}
			return super.compare(viewer, e1, e2);
		}

		/**
		 * @return Returns the sortColumn.
		 */
		public int getSortColumn() {
			return sortColumn;
		}

		/**
		 * @param sortColumn
		 *            The sortColumn to set.
		 */
		public void setSortColumn(int sortColumn) {
			if (this.sortColumn != sortColumn) {
				lastSortColumn = this.sortColumn;
				lastAscending = this.ascending;
				this.sortColumn = sortColumn;
			}
		}

		/**
		 * @return Returns the ascending.
		 */
		public boolean isAscending() {
			return ascending;
		}

		/**
		 * @param ascending
		 *            The ascending to set.
		 */
		public void setAscending(boolean ascending) {
			this.ascending = ascending;
		}
	}

	static {
		BundleContext context = Platform.getBundle(Constants.Bundle_ID).getBundleContext();
		ServiceTracker<IProvisioningAgent, IProvisioningAgent> tracker = new ServiceTracker<IProvisioningAgent, IProvisioningAgent>(context, IProvisioningAgent.class, null);
		tracker.open();
		agent = tracker.getService();
		tracker.close();
		if (agent != null)
			profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
	}

	public AbstractPage(String pageName) {
		super(pageName);
	}

	public AbstractPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	protected IProfile getSelfProfile() {
		if (profileRegistry != null) {
			String selfID = System.getProperty("eclipse.p2.profile"); //$NON-NLS-1$
			if (selfID == null)
				selfID = IProfileRegistry.SELF;
			return profileRegistry.getProfile(selfID);
		}
		return null;
	}

	private void createColumns(TableViewer viewer) {
		String[] titles = {Messages.Column_Name, Messages.Column_Version, Messages.Column_Id};
		for (int i = 0; i < titles.length; i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText(titles[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			if (Messages.Column_Name.equals(titles[i]))
				updateTableSorting(i);
			final int columnIndex = i;
			column.getColumn().addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateTableSorting(columnIndex);
				}
			});
		}
	}

	protected void updateTableSorting(int columnIndex) {
		TableViewerComparator comparator = (TableViewerComparator) viewer.getComparator();
		// toggle direction if it's the same column
		if (columnIndex == comparator.getSortColumn()) {
			comparator.setAscending(!comparator.isAscending());
		}
		comparator.setSortColumn(columnIndex);
		viewer.getTable().setSortColumn(viewer.getTable().getColumn(columnIndex));
		viewer.getTable().setSortDirection(comparator.isAscending() ? SWT.UP : SWT.DOWN);
		viewer.refresh(false);
	}

	protected abstract void createContents(Composite composite);

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		initializeService();
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout(1, true);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 5;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		createContents(composite);

		// can not finish initially, but don't want to start with an error
		// message either
		if (!(validDestination() && validateOptionsGroup())) {
			setPageComplete(false);
		}

		setControl(composite);
		giveFocusToDestination();
		Dialog.applyDialogFont(composite);
	}

	protected void createDestinationGroup(Composite parent) {
		Composite composite = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		Label label = new Label(composite, SWT.NONE);
		label.setText(getDestinationLabel());

		destinationNameField = new Combo(composite, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		destinationNameField.setLayoutData(data);
		destinationNameField.addListener(SWT.Modify | SWT.Selection, this);
		destinationBrowseButton = new Button(composite, SWT.PUSH);
		destinationBrowseButton.setText(Messages.Page_BUTTON_BROWSER);
		destinationBrowseButton.addListener(SWT.Selection, this);
		destinationBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
	}

	protected IUColumnConfig[] getColumnConfig() {
		return new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, ILayoutConstants.DEFAULT_SMALL_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_IdColumnTitle, IUColumnConfig.COLUMN_ID, ILayoutConstants.DEFAULT_COLUMN_WIDTH)};
	}

	protected void createInstallationTable(final Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		GridData griddata = new GridData(GridData.FILL, GridData.FILL, true, true);
		griddata.verticalSpan = griddata.horizontalSpan = 0;
		group.setLayoutData(griddata);
		group.setLayout(new GridLayout(1, false));
		viewer = CheckboxTableViewer.newCheckList(group, SWT.MULTI | SWT.BORDER);
		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(false);
		viewer.setComparator(new TableViewerComparator());
		createColumns(viewer);
		viewer.setContentProvider(getContentProvider());
		viewer.setLabelProvider(getLabelProvider());
		parent.addControlListener(new ControlAdapter() {
			private final int[] columnRate = new int[] {6, 2, 2};

			@Override
			public void controlResized(ControlEvent e) {
				Rectangle area = parent.getClientArea();
				Point size = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				ScrollBar vBar = table.getVerticalBar();
				int width = area.width - table.computeTrim(0, 0, 0, 0).width - vBar.getSize().x;
				if (size.y > area.height + table.getHeaderHeight()) {
					// Subtract the scrollbar width from the total column width
					// if a vertical scrollbar will be required
					Point vBarSize = vBar.getSize();
					width -= vBarSize.x;
				}
				Point oldSize = table.getSize();
				TableColumn[] columns = table.getColumns();
				int hasUsed = 0, i = 0;
				if (oldSize.x > area.width) {
					// table is getting smaller so make the columns 
					// smaller first and then resize the table to
					// match the client area width
					for (; i < columns.length - 1; i++) {
						columns[i].setWidth(width * columnRate[i] / 10);
						hasUsed += columns[i].getWidth();
					}
					columns[columns.length - 1].setWidth(width - hasUsed);
					table.setSize(area.width, area.height);
				} else {
					// table is getting bigger so make the table 
					// bigger first and then make the columns wider
					// to match the client area width
					table.setSize(area.width, area.height);
					for (; i < columns.length - 1; i++) {
						columns[i].setWidth(width * columnRate[i] / 10);
						hasUsed += columns[i].getWidth();
					}
					columns[columns.length - 1].setWidth(width - hasUsed);
				}
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updatePageCompletion();
			}
		});
		ICheckStateProvider provider = getViewerDefaultState();
		if (provider != null)
			viewer.setCheckStateProvider(provider);
		viewer.getControl().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		viewer.getControl().setSize(300, 200);
		viewer.setInput(getInput());
		Composite buttons = new Composite(group, SWT.NONE);
		buttons.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		buttons.setLayout(new RowLayout(SWT.HORIZONTAL));
		Button selectAll = new Button(buttons, SWT.PUSH);
		selectAll.setText(Messages.AbstractPage_ButtonSelectAll);
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (TableItem item : viewer.getTable().getItems()) {
					if (!item.getChecked()) {
						item.setChecked(true);
						Event event = new Event();
						event.widget = item.getParent();
						event.detail = SWT.CHECK;
						event.item = item;
						event.type = SWT.Selection;
						viewer.getTable().notifyListeners(SWT.Selection, event);
					}
				}
				updatePageCompletion();
			}
		});
		Button deselectAll = new Button(buttons, SWT.PUSH);
		deselectAll.setText(Messages.AbstractPage_ButtonDeselectAll);
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setAllChecked(false);
				updatePageCompletion();
			}
		});
	}

	protected ICheckStateProvider getViewerDefaultState() {
		return null;
	}

	protected ITableLabelProvider getLabelProvider() {
		return new IUDetailsLabelProvider(null, getColumnConfig(), null);
	}

	protected IContentProvider getContentProvider() {
		return new DeferredQueryContentProvider();
	}

	protected boolean determinePageCompletion() {
		// validate groups in order of priority so error message is the most important one
		boolean complete = validateDestinationGroup() && validateOptionsGroup();

		// Avoid draw flicker by not clearing the error
		// message unless all is valid.
		if (complete) {
			setErrorMessage(null);
		} else {
			setErrorMessage(currentMessage);
		}

		return complete;
	}

	protected abstract void doFinish() throws Exception;

	protected int getBrowseDialogStyle() {
		return SWT.OPEN;
	}

	/**
	 * returns the destination label
	 * @return non null string
	 */
	protected abstract String getDestinationLabel();

	/**
	 * Answer the contents of self's destination specification widget
	 * 
	 * @return java.lang.String
	 */
	protected String getDestinationValue() {
		return destinationNameField.getText().trim();
	}

	/**
	 * return the title of dialog
	 * @return non null string
	 */
	protected abstract String getDialogTitle();

	protected abstract Object getInput();

	protected abstract String getInvalidDestinationMessage();

	protected String getNoOptionsMessage() {
		return Messages.PAGE_NOINSTALLTION_ERROR;
	}

	protected abstract void giveFocusToDestination();

	/**
	 * Open an appropriate destination browser so that the user can specify a
	 * source to import from
	 */
	protected void handleDestinationBrowseButtonPressed() {
		FileDialog dialog = new FileDialog(getContainer().getShell(), getBrowseDialogStyle() | SWT.SHEET);
		dialog.setText(getDialogTitle());
		dialog.setFilterPath(getDestinationValue());
		dialog.setFilterExtensions(new String[] {Messages.EXTENSION_p2F, Messages.EXTENSION_ALL});
		dialog.setFilterNames(new String[] {Messages.EXTENSION_p2F_NAME, Messages.EXTENSION_ALL_NAME});
		String selectedFileName = dialog.open();

		if (selectedFileName != null) {
			if (!selectedFileName.endsWith(Messages.EXTENSION_p2F.substring(1)))
				selectedFileName += Messages.EXTENSION_p2F.substring(1);
			setDestinationValue(selectedFileName);
		}
	}

	public void handleEvent(Event event) {
		Widget source = event.widget;

		if (source == destinationBrowseButton) {
			handleDestinationBrowseButtonPressed();
		}
	}

	protected void initializeService() {
		ServiceTracker<P2ImportExport, P2ImportExport> tracker = new ServiceTracker<P2ImportExport, P2ImportExport>(Platform.getBundle(Constants.Bundle_ID).getBundleContext(), P2ImportExport.class.getName(), null);
		tracker.open();
		importexportService = tracker.getService();
		tracker.close();
	}

	protected void setDestinationValue(String selectedFileName) {
		destinationNameField.setText(selectedFileName);
	}

	/**
	 * Determine if the page is complete and update the page appropriately.
	 */
	protected void updatePageCompletion() {
		boolean pageComplete = determinePageCompletion();
		setPageComplete(pageComplete);
		if (pageComplete) {
			setMessage(null);
		}
	}

	/**
	 * Validate the destination group.
	 * @return <code>true</code> if the group is valid. If
	 * not set the error message and return <code>false</code>.
	 */
	protected boolean validateDestinationGroup() {
		if (!validDestination()) {
			currentMessage = getInvalidDestinationMessage();
			return false;
		}

		return true;
	}

	protected boolean validateOptionsGroup() {
		if (viewer == null || viewer.getCheckedElements().length > 0)
			return true;

		currentMessage = getNoOptionsMessage();
		return false;
	}

	protected boolean validDestination() {
		if (this.destinationNameField == null)
			return true;
		File file = new File(getDestinationValue());
		return !(file.getPath().length() <= 0 || file.isDirectory());
	}
}