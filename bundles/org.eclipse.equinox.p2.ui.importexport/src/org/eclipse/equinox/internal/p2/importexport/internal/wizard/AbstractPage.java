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
 *     Ericsson AB (Pascal Rapicault) - Bug 387115 - Allow to export everything
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal.wizard;

import java.io.File;
import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.equinox.internal.p2.importexport.P2ImportExport;
import org.eclipse.equinox.internal.p2.importexport.internal.Constants;
import org.eclipse.equinox.internal.p2.importexport.internal.Messages;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.model.InstalledIUElement;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.eclipse.ui.progress.WorkbenchJob;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractPage extends WizardPage implements Listener {

	protected String currentMessage;
	protected Button destinationBrowseButton;
	protected Button includeAllButton;
	protected Combo destinationNameField;
	protected P2ImportExport importexportService = null;
	protected CheckboxTreeViewer viewer = null;
	protected Exception finishException;
	protected boolean entryChanged = false;
	protected static IProfileRegistry profileRegistry = null;
	static IProvisioningAgent agent = null;

	// dialog store id constants
	private static final String STORE_DESTINATION_NAMES_ID = "P2ImportExportPage.STORE_DESTINATION_NAMES_ID";//$NON-NLS-1$

	protected static final int COMBO_HISTORY_LENGTH = 5;

	/**
	 * {@link DelayedFilterCheckboxTree} has a timing bug to prevent restoring the check state,
	 * the methods {@link DeferredTreeContentManager}'s runClearPlaceholderJob and 
	 * DelayedFilterCheckboxTree.doCreateRefreshJob().new JobChangeAdapter() {...}.done(IJobChangeEvent) has timing issue,
	 * I can't find a way to guarantee the first job is executed firstly
	 *
	 */
	final class ImportExportFilteredTree extends FilteredTree {
		ArrayList<Object> checkState = new ArrayList<Object>();

		ImportExportFilteredTree(Composite parent, int treeStyle, PatternFilter filter, boolean useNewLook) {
			super(parent, treeStyle, filter, useNewLook);
		}

		@Override
		protected TreeViewer doCreateTreeViewer(Composite composite, int style) {
			return new CheckboxTreeViewer(composite, style);
		}

		@Override
		protected WorkbenchJob doCreateRefreshJob() {
			WorkbenchJob job = super.doCreateRefreshJob();
			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void aboutToRun(IJobChangeEvent event) {
					Display.getDefault().syncExec(new Runnable() {

						public void run() {
							Object[] checked = viewer.getCheckedElements();
							if (checkState == null)
								checkState = new ArrayList<Object>(checked.length);
							for (int i = 0; i < checked.length; i++)
								if (!viewer.getGrayed(checked[i]))
									if (!checkState.contains(checked[i]))
										checkState.add(checked[i]);
						}
					});
				}

				@Override
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {
						Display.getDefault().asyncExec(new Runnable() {

							public void run() {
								if (viewer == null || viewer.getTree().isDisposed())
									return;
								if (checkState == null)
									return;

								viewer.setCheckedElements(new Object[0]);
								viewer.setGrayedElements(new Object[0]);
								// Now we are only going to set the check state of the leaf nodes
								// and rely on our container checked code to update the parents properly.
								Iterator<Object> iter = checkState.iterator();
								while (iter.hasNext()) {
									viewer.setChecked(iter.next(), true);
								}

								updatePageCompletion();
							}
						});
					}
				}
			});
			return job;
		}
	}

	class TreeViewerComparator extends ViewerComparator {
		private int sortColumn = 0;
		private int lastSortColumn = 0;
		private boolean ascending = false;
		private boolean lastAscending = false;

		@Override
		public int compare(Viewer viewer1, Object e1, Object e2) {
			IInstallableUnit iu1 = ProvUI.getAdapter(e1, IInstallableUnit.class);
			IInstallableUnit iu2 = ProvUI.getAdapter(e2, IInstallableUnit.class);
			if (iu1 != null && iu2 != null) {
				if (viewer1 instanceof TreeViewer) {
					TreeViewer treeViewer = (TreeViewer) viewer1;
					IBaseLabelProvider baseLabel = treeViewer.getLabelProvider();
					if (baseLabel instanceof ITableLabelProvider) {
						ITableLabelProvider tableProvider = (ITableLabelProvider) baseLabel;
						String e1p = tableProvider.getColumnText(e1, getSortColumn());
						String e2p = tableProvider.getColumnText(e2, getSortColumn());
						// don't suppress this warning as it will cause build-time warning
						// see bug 423628. This should be possible to fix once
						// SWT/JFace adopt generics
						int result = getComparator().compare(e1p, e2p);
						// Secondary column sort
						if (result == 0) {
							e1p = tableProvider.getColumnText(e1, lastSortColumn);
							e2p = tableProvider.getColumnText(e2, lastSortColumn);
							// don't suppress this warning as it will cause build-time warning
							// see bug 423628. This should be possible to fix once
							// SWT/JFace adopt generics
							int result2 = getComparator().compare(e1p, e2p);
							return lastAscending ? result2 : (-1) * result2;
						}
						return isAscending() ? result : (-1) * result;
					}
				}
				// we couldn't determine a secondary sort, call it equal
				return 0;
			}
			return super.compare(viewer1, e1, e2);
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

	private void createColumns(TreeViewer treeViewer) {
		String[] titles = {Messages.Column_Name, Messages.Column_Version, Messages.Column_Id};
		for (int i = 0; i < titles.length; i++) {
			TreeViewerColumn column = new TreeViewerColumn(treeViewer, SWT.NONE);
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
		TreeViewerComparator comparator = (TreeViewerComparator) viewer.getComparator();
		// toggle direction if it's the same column
		if (columnIndex == comparator.getSortColumn()) {
			comparator.setAscending(!comparator.isAscending());
		}
		comparator.setSortColumn(columnIndex);
		viewer.getTree().setSortColumn(viewer.getTree().getColumn(columnIndex));
		viewer.getTree().setSortDirection(comparator.isAscending() ? SWT.UP : SWT.DOWN);
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

	protected void createDestinationGroup(Composite parent, boolean includeButton) {
		Composite composite = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		Label label = new Label(composite, SWT.NONE);
		label.setText(getDestinationLabel());

		destinationNameField = new Combo(composite, SWT.SINGLE | SWT.BORDER);
		restoreWidgetValues();
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		destinationNameField.setLayoutData(data);
		destinationNameField.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleDestinationChanged(getDestinationValue());
			}
		});
		destinationNameField.addKeyListener(new KeyListener() {

			/*
			 * @see KeyListener.keyPressed
			 */
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.CR) {
					entryChanged = false;
					handleDestinationChanged(getDestinationValue());
				}
			}

			public void keyReleased(KeyEvent e) {
				// do nothing
			}
		});
		destinationNameField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				entryChanged = true;
			}
		});
		destinationNameField.addFocusListener(new FocusListener() {
			/*
			 * @see FocusListener.focusGained(FocusEvent)
			 */
			public void focusGained(FocusEvent e) {
				//Do nothing when getting focus
			}

			/*
			 * @see FocusListener.focusLost(FocusEvent)
			 */
			public void focusLost(FocusEvent e) {
				//Clear the flag to prevent constant update
				if (entryChanged) {
					entryChanged = false;
					handleDestinationChanged(getDestinationValue());
				}

			}
		});

		destinationBrowseButton = new Button(composite, SWT.PUSH);
		destinationBrowseButton.setText(Messages.Page_BUTTON_BROWSER);
		destinationBrowseButton.addListener(SWT.Selection, this);
		destinationBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		if (includeButton) {
			includeAllButton = new Button(composite, SWT.CHECK);
			includeAllButton.setText(Messages.ExportPage_EntriesNotInRepo);
			includeAllButton.setSelection(allowExportWithoutRepositoryReference());
			GridData dataIncludeButton = new GridData();
			dataIncludeButton.horizontalSpan = 3;
			includeAllButton.setLayoutData(dataIncludeButton);
		}
	}

	private boolean allowExportWithoutRepositoryReference() {
		return Platform.getPreferencesService().getBoolean(Constants.Bundle_ID, Constants.PREF_IU_WITHOUT_REPO, false, new IScopeContext[] {DefaultScope.INSTANCE});
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
		PatternFilter filter = getPatternFilter();
		filter.setIncludeLeadingWildcard(true);
		final ImportExportFilteredTree filteredTree = new ImportExportFilteredTree(group, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, filter, true);
		viewer = (CheckboxTreeViewer) filteredTree.getViewer();
		final Tree tree = viewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(false);
		viewer.setComparator(new TreeViewerComparator());
		viewer.setComparer(new ProvElementComparer());
		createColumns(viewer);
		final ITreeContentProvider contentProvider = getContentProvider();
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(getLabelProvider());
		viewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				if (!event.getChecked() && filteredTree.checkState != null) {
					ArrayList<Object> toRemove = new ArrayList<Object>(1);
					// See bug 258117.  Ideally we would get check state changes 
					// for children when the parent state changed, but we aren't, so
					// we need to remove all children from the additive check state
					// cache.
					if (contentProvider.hasChildren(event.getElement())) {
						Set<Object> unchecked = new HashSet<Object>();
						Object[] children = contentProvider.getChildren(event.getElement());
						for (int i = 0; i < children.length; i++) {
							unchecked.add(children[i]);
						}
						Iterator<Object> iter = filteredTree.checkState.iterator();
						while (iter.hasNext()) {
							Object current = iter.next();
							if (current != null && unchecked.contains(current)) {
								toRemove.add(current);
							}
						}
					} else {
						for (Object element : filteredTree.checkState) {
							if (viewer.getComparer().equals(element, event.getElement())) {
								toRemove.add(element);
								// Do not break out of the loop.  We may have duplicate equal
								// elements in the cache.  Since the cache is additive, we want
								// to be sure we've gotten everything.
							}
						}
					}
					filteredTree.checkState.removeAll(toRemove);
				}
			}
		});
		parent.addControlListener(new ControlAdapter() {
			private final int[] columnRate = new int[] {6, 2, 2};

			@Override
			public void controlResized(ControlEvent e) {
				Rectangle area = parent.getClientArea();
				Point size = tree.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				ScrollBar vBar = tree.getVerticalBar();
				int width = area.width - tree.computeTrim(0, 0, 0, 0).width - vBar.getSize().x;
				if (size.y > area.height + tree.getHeaderHeight()) {
					// Subtract the scrollbar width from the total column width
					// if a vertical scrollbar will be required
					Point vBarSize = vBar.getSize();
					width -= vBarSize.x;
				}
				Point oldSize = tree.getSize();
				TreeColumn[] columns = tree.getColumns();
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
					tree.setSize(area.width, area.height);
				} else {
					// table is getting bigger so make the table 
					// bigger first and then make the columns wider
					// to match the client area width
					tree.setSize(area.width, area.height);
					for (; i < columns.length - 1; i++) {
						columns[i].setWidth(width * columnRate[i] / 10);
						hasUsed += columns[i].getWidth();
					}
					columns[columns.length - 1].setWidth(width - hasUsed);
				}
			}
		});
		ICheckStateProvider provider = getViewerDefaultState();
		if (provider != null)
			viewer.setCheckStateProvider(provider);
		else
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					updatePageCompletion();
				}
			});
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
				for (TreeItem item : viewer.getTree().getItems()) {
					if (!item.getChecked()) {
						item.setChecked(true);
						Event event = new Event();
						event.widget = item.getParent();
						event.detail = SWT.CHECK;
						event.item = item;
						event.type = SWT.Selection;
						viewer.getTree().notifyListeners(SWT.Selection, event);
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
				for (TreeItem item : viewer.getTree().getItems()) {
					viewer.setSubtreeChecked(item.getData(), false);
				}
				updatePageCompletion();
			}
		});
	}

	protected PatternFilter getPatternFilter() {
		return new AvailableIUPatternFilter(getColumnConfig());
	}

	protected ICheckStateProvider getViewerDefaultState() {
		return null;
	}

	protected ITableLabelProvider getLabelProvider() {
		return new IUDetailsLabelProvider(null, getColumnConfig(), null);
	}

	protected ITreeContentProvider getContentProvider() {
		ProvElementContentProvider provider = new ProvElementContentProvider() {
			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof InstalledIUElement)
					return false;
				return super.hasChildren(element);
			}

			@Override
			public Object[] getChildren(Object parent) {
				if (parent instanceof InstalledIUElement)
					return new Object[0];
				return super.getChildren(parent);
			}
		};
		return provider;
	}

	protected boolean determinePageCompletion() {
		currentMessage = null;
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
			handleDestinationChanged(selectedFileName);
		}
	}

	public void handleEvent(Event event) {
		Widget source = event.widget;

		if (source == destinationBrowseButton) {
			handleDestinationBrowseButtonPressed();
		}
		updatePageCompletion();
	}

	protected void handleDestinationChanged(String newDestination) {
		// do nothing
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
			if (this instanceof AbstractImportPage)
				saveWidgetValues();
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

	protected void saveWidgetValues() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
			if (directoryNames == null) {
				directoryNames = new String[0];
			}

			directoryNames = addToHistory(directoryNames, getDestinationValue());
			settings.put(STORE_DESTINATION_NAMES_ID, directoryNames);
		}
	}

	protected String[] addToHistory(String[] history, String newEntry) {
		List<String> l = new ArrayList<String>(Arrays.asList(history));
		addToHistory(l, newEntry);
		String[] r = new String[l.size()];
		l.toArray(r);
		return r;
	}

	protected void addToHistory(List<String> history, String newEntry) {
		history.remove(newEntry);
		history.add(0, newEntry);

		// since only one new item was added, we can be over the limit
		// by at most one item
		if (history.size() > COMBO_HISTORY_LENGTH) {
			history.remove(COMBO_HISTORY_LENGTH);
		}
	}

	/**
	 * Hook method for restoring widget values to the values that they held last
	 * time this wizard was used to completion.
	 */
	protected void restoreWidgetValues() {

		IDialogSettings settings = getDialogSettings();

		if (settings != null) {
			String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
			if (directoryNames != null) {
				// destination
				setDestinationValue(directoryNames[0]);
				for (int i = 0; i < directoryNames.length; i++) {
					addDestinationItem(directoryNames[i]);
				}

				setDestinationValue(""); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Add the passed value to self's destination widget's history
	 * 
	 * @param value
	 *            java.lang.String
	 */
	protected void addDestinationItem(String value) {
		destinationNameField.add(value);
	}
}