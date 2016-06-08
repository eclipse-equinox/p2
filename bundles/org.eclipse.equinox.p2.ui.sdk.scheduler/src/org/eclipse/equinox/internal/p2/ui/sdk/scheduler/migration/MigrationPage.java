/*******************************************************************************
 * Copyright (c) 2011, 2015 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     IBM Corporation - Ongoing development
 *     Ericsson AB (Pascal Rapicault) - Bug 387115 - Allow to export everything
 *     Ericsson AB (Hamdan Msheik) - Bug 398833, 402560, 427195
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.equinox.internal.p2.metadata.query.UpdateQuery;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdatePlugin;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class MigrationPage extends WizardPage implements ISelectableIUsPage, Listener {

	protected String currentMessage;
	//	protected Button destinationBrowseButton;
	protected CheckboxTreeViewer viewer = null;
	protected Exception finishException;
	protected boolean entryChanged = false;
	protected static IProfileRegistry profileRegistry = null;
	static IProvisioningAgent agent = null;
	protected Button updateToLatest;

	public static final String REMIND_ME_LATER = "remindMeToMigrateLater";

	IProfile profile = null;

	private ProvisioningOperationWizard wizard;
	private ProvisioningUI ui;

	protected IProvisioningAgent otherInstanceAgent = null;
	private Collection<IInstallableUnit> unitsToMigrate;
	private Set<IInstallableUnit> selectedUnitsToMigrate; // selected units to be migrated, initially contains all units not installed in current profile.
	private IProfile toImportFrom = null;
	//	private File instancePath = null;
	private URI[] metaURIs = null;
	private URI[] artiURIs = null;

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
						// SWT/JFace adopts generics
						int result = getComparator().compare(e1p, e2p);
						// Secondary column sort
						if (result == 0) {
							e1p = tableProvider.getColumnText(e1, lastSortColumn);
							e2p = tableProvider.getColumnText(e2, lastSortColumn);
							// don't suppress this warning as it will cause build-time warning
							// see bug 423628. This should be possible to fix once
							// SWT/JFace adopts generics
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
		BundleContext context = Platform.getBundle(ProvUIActivator.PLUGIN_ID).getBundleContext();
		ServiceTracker<IProvisioningAgent, IProvisioningAgent> tracker = new ServiceTracker<IProvisioningAgent, IProvisioningAgent>(context, IProvisioningAgent.class, null);
		tracker.open();
		agent = tracker.getService();
		tracker.close();
		if (agent != null)
			profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
	}

	public MigrationPage(String pageName) {
		super(pageName);
	}

	public MigrationPage(ProvisioningUI ui, ProvisioningOperationWizard wizard, IProfile toImportFrom, Collection<IInstallableUnit> unitsToMigrate, boolean firstTime) {
		super("MigrationPageInstance"); //$NON-NLS-1$
		this.wizard = wizard;
		this.ui = ui;
		profile = getSelfProfile();
		this.toImportFrom = toImportFrom;
		this.unitsToMigrate = unitsToMigrate;
		setTitle(firstTime ? ProvUIMessages.MigrationPage_DIALOG_TITLE_FIRSTRUN : ProvUIMessages.MigrationPage_DIALOG_TITLE);
		setDescription(NLS.bind(ProvUIMessages.MigrationPage_DIALOG_DESCRIPTION, Platform.getProduct().getName()));
	}

	public MigrationPage(ProvisioningUI ui, ProvisioningOperationWizard wizard, boolean firstTime) {
		super("importfrominstancepage"); //$NON-NLS-1$
		this.wizard = wizard;
		this.ui = ui;
		setTitle(firstTime ? ProvUIMessages.MigrationPage_DIALOG_TITLE_FIRSTRUN : ProvUIMessages.MigrationPage_DIALOG_TITLE);
		setDescription(NLS.bind(ProvUIMessages.MigrationPage_DIALOG_DESCRIPTION, Platform.getProduct().getName()));
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
		String[] titles = {ProvUIMessages.Column_Name, ProvUIMessages.Column_Version, ProvUIMessages.Column_Id};
		for (int i = 0; i < titles.length; i++) {
			TreeViewerColumn column = new TreeViewerColumn(treeViewer, SWT.NONE);
			column.getColumn().setText(titles[i]);
			column.getColumn().setResizable(true);
			column.getColumn().setMoveable(true);
			if (titles[i].equals(ProvUIMessages.Column_Name)) {
				column.getColumn().setWidth(300);
				updateTableSorting(i);
			} else {
				column.getColumn().setWidth(200);
			}

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

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		//		initializeService();
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout(1, true);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 5;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		createContents(composite);

		// can not finish initially, but don't want to start with an error
		// message either
		if (!(validateOptionsGroup())) {
			setPageComplete(false);
		}

		setControl(composite);
		//		giveFocusToDestination();
		Dialog.applyDialogFont(composite);
	}

	protected IUColumnConfig[] getColumnConfig() {
		return new IUColumnConfig[] {new IUColumnConfig(org.eclipse.equinox.internal.p2.ui.ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH), new IUColumnConfig(org.eclipse.equinox.internal.p2.ui.ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, ILayoutConstants.DEFAULT_SMALL_COLUMN_WIDTH), new IUColumnConfig(org.eclipse.equinox.internal.p2.ui.ProvUIMessages.ProvUI_IdColumnTitle, IUColumnConfig.COLUMN_ID, ILayoutConstants.DEFAULT_COLUMN_WIDTH)};
	}

	protected void createContents(Composite composite) {
		createInstallationTable(composite);
		createAdditionOptions(composite);
	}

	protected void createInstallationTable(final Composite parent) {

		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		FillLayout fill = new FillLayout();
		sashForm.setLayout(fill);
		GridData data = new GridData(GridData.FILL_BOTH);
		sashForm.setLayoutData(data);
		Composite sashComposite = new Composite(sashForm, SWT.NONE);
		GridLayout grid = new GridLayout();
		grid.marginWidth = 0;
		grid.marginHeight = 0;
		sashComposite.setLayout(grid);

		PatternFilter filter = getPatternFilter();
		filter.setIncludeLeadingWildcard(true);
		final ImportExportFilteredTree filteredTree = new ImportExportFilteredTree(sashComposite, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, filter, true);
		viewer = (CheckboxTreeViewer) filteredTree.getViewer();
		final Tree tree = viewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(false);
		GridData treeDataGrid = new GridData(GridData.FILL_BOTH);
		treeDataGrid.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		treeDataGrid.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH);
		tree.setLayoutData(treeDataGrid);

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
			private final int[] columnRate = new int[] {4, 2, 2};

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

		selectedUnitsToMigrate = identifyUnitsToBeMigrated();

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
		viewer.setInput(getInput());

		viewer.getTree().addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.item instanceof TreeItem && event.detail == SWT.CHECK) {
					TreeItem treeItem = (TreeItem) event.item;
					IInstallableUnit iu = ProvUI.getAdapter(event.item.getData(), IInstallableUnit.class);
					if (treeItem.getChecked()) {
						selectedUnitsToMigrate.add(iu);
					} else {
						selectedUnitsToMigrate.remove(iu);
					}
				}
				updatePageCompletion();
			}
		});

		Composite buttons = new Composite(sashComposite, SWT.NONE);
		buttons.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		buttons.setLayout(new RowLayout(SWT.HORIZONTAL));
		Button selectAll = new Button(buttons, SWT.PUSH);
		selectAll.setText(ProvUIMessages.AbstractPage_ButtonSelectAll);
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
		deselectAll.setText(ProvUIMessages.AbstractPage_ButtonDeselectAll);
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (TreeItem item : viewer.getTree().getItems()) {
					if (item.getChecked()) {
						item.setChecked(false);
						Event event = new Event();
						event.widget = item.getParent();
						event.detail = SWT.CHECK;
						event.item = item;
						event.type = SWT.Selection;
						viewer.getTree().notifyListeners(SWT.Selection, event);
					}
					viewer.setSubtreeChecked(item.getData(), false);
				}
				updatePageCompletion();
			}
		});

	}

	private Set<IInstallableUnit> identifyUnitsToBeMigrated() {

		Set<IInstallableUnit> ius = new HashSet<IInstallableUnit>();
		if (profile != null) {
			for (IInstallableUnit iu : unitsToMigrate) {
				IQueryResult<IInstallableUnit> collector = profile.query(QueryUtil.createIUQuery(iu.getId(), new VersionRange(iu.getVersion(), true, null, false)), new NullProgressMonitor());
				if (collector.isEmpty()) {
					ius.add(iu);
				}
			}
		}

		return ius;
	}

	protected void createAdditionOptions(Composite parent) {

		Composite composite = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		updateToLatest = new Button(composite, SWT.CHECK);
		updateToLatest.setText(ProvUIMessages.MigrationPage_UPDATE_TO_LATEST);
		updateToLatest.setSelection(loadCustomizedSetting());

	}

	protected PatternFilter getPatternFilter() {
		return new AvailableIUPatternFilter(getColumnConfig());
	}

	protected ICheckStateProvider getViewerDefaultState() {
		return new ICheckStateProvider() {

			public boolean isGrayed(Object element) {
				return false;
			}

			public boolean isChecked(Object element) {
				IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
				if (selectedUnitsToMigrate.contains(iu)) {
					return true;
				}
				return false;
			}
		};
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
		boolean complete = validateOptionsGroup();

		// Avoid draw flicker by not clearing the error
		// message unless all is valid.
		if (complete) {
			setErrorMessage(null);
		} else {
			setErrorMessage(currentMessage);
		}

		return complete;
	}

	protected int getBrowseDialogStyle() {
		return SWT.OPEN;
	}

	//TODO remove the implementation of Listener
	public void handleEvent(Event event) {
		//		Widget source = event.widget;
		//
		//		if (source == destinationBrowseButton) {
		//			handleDestinationBrowseButtonPressed();
		//		}
		//		updatePageCompletion();
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

	protected boolean validateOptionsGroup() {
		if (viewer == null || viewer.getCheckedElements().length > 0)
			return true;

		currentMessage = getNoOptionsMessage();
		return false;
	}

	protected ProvisioningOperationWizard getProvisioningWizard() {
		return wizard;
	}

	protected ProvisioningUI getProvisioningUI() {
		return ui;
	}

	public boolean hasInstalled(IInstallableUnit iu) {
		IQueryResult<IInstallableUnit> results = profile.query(QueryUtil.createIUQuery(iu.getId(), new VersionRange(iu.getVersion(), true, null, false)), null);
		return !results.isEmpty();
	}

	public String getIUNameWithDetail(IInstallableUnit iu) {
		IQueryResult<IInstallableUnit> results = profile.query(QueryUtil.createIUQuery(iu.getId(), new VersionRange(iu.getVersion(), true, null, false)), null);
		String text = iu.getProperty(IProfile.PROP_NAME, null);
		text = (text != null) ? text : iu.getId();
		if (!results.isEmpty()) {
			boolean hasHigherVersion = false;
			boolean hasEqualVersion = false;
			for (IInstallableUnit installedIU : results.toSet()) {
				int compareValue = installedIU.getVersion().compareTo(iu.getVersion());
				if (compareValue > 0) {
					hasHigherVersion = true;
					break;
				} else if (compareValue == 0)
					hasEqualVersion = true;
			}
			if (hasHigherVersion)
				return NLS.bind(ProvUIMessages.AbstractImportPage_HigherVersionInstalled, text);
			else if (hasEqualVersion)
				return NLS.bind(ProvUIMessages.AbstractImportPage_SameVersionInstalled, text);
		}
		return text;
	}

	protected void doFinish() throws Exception {
		// do nothing
	}

	@Override
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	protected String getDialogTitle() {
		return ProvUIMessages.MigrationPage_DIALOG_TITLE;
	}

	protected Object getInput() {

		IUElementListRoot root = new IUElementListRoot(ui);
		List<AvailableIUElement> elements = new ArrayList<AvailableIUElement>(unitsToMigrate.size());
		for (IInstallableUnit unit : unitsToMigrate) {
			elements.add(new AvailableIUElement(root, unit, toImportFrom.getProfileId(), false));
		}
		root.setChildren(elements.toArray());
		return root;
	}

	protected String getInvalidDestinationMessage() {
		return "";//ProvUIMessages.ImportFromInstallationPage_INVALID_DESTINATION; //$NON-NLS-1$
	}

	protected String getNoOptionsMessage() {
		return ProvUIMessages.MigrationPage_SELECT_COMPONENT;
	}

	class ImportFromInstallationLabelProvider extends IUDetailsLabelProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			String text = super.getColumnText(element, columnIndex);
			// it's the order of label provider
			if (columnIndex == 0) {
				IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
				return getIUNameWithDetail(iu);
			}
			return text;
		}

		@Override
		public Color getForeground(Object element) {
			IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
			if (hasInstalled(iu))
				return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
			return super.getForeground(element);
		}
	}

	protected ITableLabelProvider getLabelProvider() {
		return new ImportFromInstallationLabelProvider();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (otherInstanceAgent != null) {
			otherInstanceAgent.stop();
			otherInstanceAgent = null;
			toImportFrom = null;
		}
		cleanLocalRepository();
	}

	public void cleanLocalRepository() {
		if (metaURIs != null && metaURIs.length > 0) {
			IProvisioningAgent runningAgent = getProvisioningUI().getSession().getProvisioningAgent();
			IMetadataRepositoryManager manager = (IMetadataRepositoryManager) runningAgent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			for (URI uri : metaURIs)
				manager.removeRepository(uri);
			IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) runningAgent.getService(IArtifactRepositoryManager.SERVICE_NAME);
			for (URI uri : artiURIs)
				artifactManager.removeRepository(uri);
		}
	}

	// Both checkedElements and checkedElementsUpdates and the logic inside the getCheckedIUElements method
	// are used to prevent unnecessary call to getUpdates method due to computational cost.
	@SuppressWarnings("rawtypes") private Set checkedElements;
	@SuppressWarnings("rawtypes") private Set checkedElementsUpdates;
	private boolean getUpdatesCanceled;

	public Object[] getCheckedIUElements() {

		if (isUpdateToLatest()) {

			Object[] latestUpdates = getLatestVersionOfCheckedElements();

			// If the getUpdades operation is cancelled, then set checkedElements and checkedElementsUpdates to null to force the lookup for updates again. Thereafter throw OperationCanceledException.
			if (getUpdatesCanceled) {

				this.checkedElements = null;
				this.checkedElementsUpdates = null;
				throw new OperationCanceledException();
			}
			return latestUpdates;
		}
		return viewer.getCheckedElements();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private Object[] getLatestVersionOfCheckedElements() {

		Object[] checkedArray = viewer.getCheckedElements();
		if (this.checkedElements == null) {
			// initialize checkedElements and checkedElementsUpdates for the first time
			this.checkedElements = new HashSet(Arrays.asList(checkedArray));
			this.checkedElementsUpdates = new HashSet(Arrays.asList(getUpdates(checkedArray)));

		} else {
			Set checkedElementsNow = new HashSet(Arrays.asList(checkedArray));
			if (checkedElementsNow.size() != this.checkedElements.size() || (!checkedElementsNow.containsAll(checkedElements))) {
				// only if the set of checkedElements has changed get the update for them
				this.checkedElements = checkedElementsNow; //
				this.checkedElementsUpdates = new HashSet(Arrays.asList(getUpdates(checkedArray)));
			}
		}

		return this.checkedElementsUpdates.toArray();
	}

	public Object[] getSelectedIUElements() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setCheckedElements(Object[] elements) {
		new UnsupportedOperationException();
	}

	// Look for update of the current selected installation units and replace the old ons with the updated version
	private Object[] getUpdates(final Object[] _checkedElements) {

		final Collection<IInstallableUnit> toInstall = new ArrayList<IInstallableUnit>();

		try {
			getContainer().run(false, true, new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) {
					SubMonitor sub = SubMonitor.convert(monitor, _checkedElements.length);
					ProvisioningContext context = new ProvisioningContext(getProvisioningUI().getSession().getProvisioningAgent());

					for (Object iu : _checkedElements) {

						if (sub.isCanceled()) {
							MigrationPage.this.getUpdatesCanceled = true;
							toInstall.clear();
							sub.done();
							return;
						}

						if (iu instanceof AvailableIUElement) {
							IInstallableUnit unit = ((AvailableIUElement) iu).getIU();
							IuUpdateAndPatches updateAndPatches = filterToInstall(unit, updatesFor(unit, context, sub.newChild(1)));
							if (updateAndPatches.update != null) {
								toInstall.add(updateAndPatches.update);
							} else {
								toInstall.add(updateAndPatches.iu); // because it is not yet installed
								toInstall.addAll(updateAndPatches.patches);
							}

						}

						sub.worked(1);
					}
				}

			});
		} catch (InterruptedException e) {
			// Nothing to report if thread was interrupted
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
		}

		return toInstall.toArray();

	}

	public boolean isUpdateToLatest() {
		return updateToLatest.getSelection();
	}

	public ProvisioningContext getProvisioningContext() {
		ProvisioningContext context = new ProvisioningContext(getProvisioningUI().getSession().getProvisioningAgent());
		context.setArtifactRepositories(artiURIs);
		context.setMetadataRepositories(metaURIs);
		return context;
	}

	private static boolean hasHigherFidelity(IInstallableUnit iu, IInstallableUnit currentIU) {
		if (Boolean.parseBoolean(currentIU.getProperty(IInstallableUnit.PROP_PARTIAL_IU)) && !Boolean.parseBoolean(iu.getProperty(IInstallableUnit.PROP_PARTIAL_IU)))
			return true;
		return false;
	}

	public Collection<IInstallableUnit> updatesFor(IInstallableUnit toUpdate, ProvisioningContext context, IProgressMonitor monitor) {
		//		IPlanner planner = (IPlanner) getProvisioningUI().getSession().getProvisioningAgent().getService(IPlanner.SERVICE_NAME);
		//		return planner.updatesFor(toUpdate, context, monitor).toSet();

		Map<String, IInstallableUnit> resultsMap = new HashMap<String, IInstallableUnit>();

		SubMonitor sub = SubMonitor.convert(monitor, 1000);
		IQueryable<IInstallableUnit> queryable = context.getMetadata(sub.newChild(500));
		IQueryResult<IInstallableUnit> matches = queryable.query(new UpdateQuery(toUpdate), sub.newChild(500));
		for (Iterator<IInstallableUnit> it = matches.iterator(); it.hasNext();) {
			IInstallableUnit iu = it.next();
			String key = iu.getId() + "_" + iu.getVersion().toString(); //$NON-NLS-1$
			IInstallableUnit currentIU = resultsMap.get(key);
			if (currentIU == null || hasHigherFidelity(iu, currentIU))
				resultsMap.put(key, iu);
		}
		sub.done();
		return resultsMap.values();
	}

	class IuUpdateAndPatches {
		public IInstallableUnit iu;
		public IInstallableUnit update;
		public Collection<IInstallableUnit> patches;

		IuUpdateAndPatches(IInstallableUnit iu) {
			this.iu = iu;
			this.patches = new ArrayList<IInstallableUnit>();
		}

	}

	/**
	 *
	 * @param iu original unit.
	 * @param updates list of updates: patches or true updates.
	 * @return a structure holding the original unit, its most recent update and any available patches.
	 */
	private IuUpdateAndPatches filterToInstall(IInstallableUnit iu, Collection<IInstallableUnit> updates) {

		IuUpdateAndPatches updateAndPatches = new IuUpdateAndPatches(iu);

		for (IInstallableUnit update : updates) {

			if (QueryUtil.isPatch(update)) {
				updateAndPatches.patches.add(update);
			} else {
				if (updateAndPatches.update == null || updateAndPatches.update.getVersion().compareTo(update.getVersion()) < 0) {
					updateAndPatches.update = update;
				}
			}
		}

		return updateAndPatches;
	}

	public static boolean loadCustomizedSetting() {
		IScopeContext[] contexts = new IScopeContext[] {InstanceScope.INSTANCE, DefaultScope.INSTANCE, BundleDefaultsScope.INSTANCE, ConfigurationScope.INSTANCE};
		boolean updateToLatest = Platform.getPreferencesService().getBoolean(AutomaticUpdatePlugin.PLUGIN_ID, "updateToLatest", false, contexts);
		return updateToLatest;
	}

}
