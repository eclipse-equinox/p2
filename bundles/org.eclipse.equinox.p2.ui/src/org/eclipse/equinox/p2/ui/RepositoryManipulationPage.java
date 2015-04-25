/*******************************************************************************
 *  Copyright (c) 2007, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB (Pascal Rapicault) - bug 398539
 *******************************************************************************/
package org.eclipse.equinox.p2.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Page that allows users to update, add, remove, import, and
 * export repositories.  This page can be hosted inside a preference
 * dialog or inside its own dialog.  
 * 
 * When hosting this page inside a non-preference dialog, some of the 
 * dialog methods will likely have to call page methods.  The following 
 * snippet shows how to host this page inside a TitleAreaDialog.
 * <pre>
 *		TitleAreaDialog dialog = new TitleAreaDialog(shell) {
 *
 *			RepositoryManipulationPage page;
 *
 *			protected Control createDialogArea(Composite parent) {
 *				page = new RepositoryManipulationPage();
 *              page.setProvisioningUI(ProvisioningUI.getDefaultUI());
 *				page.createControl(parent);
 *				this.setTitle("Software Sites");
 *				this.setMessage("The enabled sites will be searched for software.  Disabled sites are ignored.");
 *				return page.getControl();
 *			}
 *
 *			protected void okPressed() {
 *				if (page.performOk())
 *					super.okPressed();
 *			}
 *
 *			protected void cancelPressed() {
 *				if (page.performCancel())
 *					super.cancelPressed();
 *			}
 *		};
 *		dialog.open();
 * </pre>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * 
 * @since 2.0
 */
public class RepositoryManipulationPage extends PreferencePage implements IWorkbenchPreferencePage, ICopyable {
	final static String DEFAULT_FILTER_TEXT = ProvUIMessages.RepositoryManipulationPage_DefaultFilterString;
	private final static int FILTER_DELAY = 200;

	StructuredViewerProvisioningListener listener;
	CheckboxTableViewer repositoryViewer;
	Table table;
	ProvisioningUI ui;
	Policy policy;
	Display display;
	boolean changed = false;
	MetadataRepositoryElementComparator comparator;
	RepositoryDetailsLabelProvider labelProvider;
	RepositoryTracker tracker;
	RepositoryTracker localCacheRepoManipulator;
	/**
	 * The input field is initialized lazily and should only be accessed via the {@link #getInput()} method.
	 */
	CachedMetadataRepositories input;
	Text pattern, details;
	PatternFilter filter;
	WorkbenchJob filterJob;
	Button addButton, removeButton, editButton, refreshButton, disableButton, exportButton;

	private Map<MetadataRepositoryElement, URI> originalURICache = new HashMap<MetadataRepositoryElement, URI>(2);
	private Map<MetadataRepositoryElement, String> originalNameCache = new HashMap<MetadataRepositoryElement, String>(2);

	class CachedMetadataRepositories extends MetadataRepositories {
		private Hashtable<String, MetadataRepositoryElement> cachedElements;

		CachedMetadataRepositories() {
			super(ui);
			setIncludeDisabledRepositories(getPolicy().getRepositoriesVisible());
		}

		public int getQueryType() {
			return QueryProvider.METADATA_REPOS;
		}

		public Object[] fetchChildren(Object o, IProgressMonitor monitor) {
			if (cachedElements == null) {
				Object[] children = super.fetchChildren(o, monitor);
				cachedElements = new Hashtable<String, MetadataRepositoryElement>(children.length);
				for (int i = 0; i < children.length; i++) {
					if (children[i] instanceof MetadataRepositoryElement) {
						put((MetadataRepositoryElement) children[i]);
					}
				}
			}
			return cachedElements.values().toArray();
		}

		MetadataRepositoryElement[] getElements() {
			return cachedElements.values().toArray(new MetadataRepositoryElement[cachedElements.size()]);
		}

		void remove(MetadataRepositoryElement element) {
			cachedElements.remove(getKey(element.getLocation()));
		}

		void put(MetadataRepositoryElement element) {
			cachedElements.put(getKey(element.getLocation()), element);
		}

		MetadataRepositoryElement get(URI location) {
			return cachedElements.get(getKey(location));
		}

		String getKey(URI location) {
			String key = URIUtil.toUnencodedString(location);
			int length = key.length();
			if (length > 0 && key.charAt(length - 1) == '/') {
				key = key.substring(0, length - 1);
			}
			return key;
		}

	}

	class MetadataRepositoryPatternFilter extends PatternFilter {
		MetadataRepositoryPatternFilter() {
			setIncludeLeadingWildcard(true);
		}

		public boolean isElementVisible(Viewer viewer, Object element) {
			if (element instanceof MetadataRepositoryElement) {
				return wordMatches(labelProvider.getColumnText(element, RepositoryDetailsLabelProvider.COL_NAME) + " " + labelProvider.getColumnText(element, RepositoryDetailsLabelProvider.COL_LOCATION)); //$NON-NLS-1$
			}
			return false;
		}
	}

	/**
	 * Create a repository manipulation page that will display the repositories
	 * available to the user.
	 */
	public RepositoryManipulationPage() {
		this.setProvisioningUI(ProvisioningUI.getDefaultUI());
	}

	/**
	 * Set the provisioning UI that provides the session, policy, and other
	 * services for the UI.  This method must be called before the contents are 
	 * created or it will have no effect.
	 * 
	 * @param ui the provisioning UI to use for this page.
	 */
	public void setProvisioningUI(ProvisioningUI ui) {
		this.ui = ui;
		this.policy = ui.getPolicy();
		tracker = ui.getRepositoryTracker();
	}

	protected Control createContents(Composite parent) {
		display = parent.getDisplay();
		// The help refers to the full-blown dialog.  No help if it's read only.
		if (policy.getRepositoriesVisible())
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parent.getShell(), IProvHelpContextIds.REPOSITORY_MANIPULATION_DIALOG);

		Composite composite = new Composite(parent, SWT.NONE);

		// Filter box
		pattern = new Text(composite, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.CANCEL);
		pattern.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			public void getName(AccessibleEvent e) {
				e.result = DEFAULT_FILTER_TEXT;
			}
		});
		pattern.setText(DEFAULT_FILTER_TEXT);
		pattern.selectAll();
		pattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				applyFilter();
			}
		});

		pattern.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					if (table.getItemCount() > 0) {
						table.setFocus();
					} else if (e.character == SWT.CR) {
						return;
					}
				}
			}
		});

		pattern.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				display.asyncExec(new Runnable() {
					public void run() {
						if (!pattern.isDisposed()) {
							if (DEFAULT_FILTER_TEXT.equals(pattern.getText().trim())) {
								pattern.selectAll();
							}
						}
					}
				});
			}
		});

		// spacer to fill other column
		if (policy.getRepositoriesVisible())
			new Label(composite, SWT.NONE);

		// Table of available repositories
		table = new Table(composite, SWT.CHECK | SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		repositoryViewer = new CheckboxTableViewer(table);

		// Key listener for delete
		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL) {
					removeRepositories();
				}
			}
		});
		setTableColumns();
		CopyUtils.activateCopy(this, table);

		repositoryViewer.setComparer(new ProvElementComparer());
		comparator = new MetadataRepositoryElementComparator(RepositoryDetailsLabelProvider.COL_NAME);
		repositoryViewer.setComparator(comparator);
		filter = new MetadataRepositoryPatternFilter();
		repositoryViewer.setFilters(new ViewerFilter[] {filter});
		// We don't need a deferred content provider because we are caching local results before
		// actually querying
		repositoryViewer.setContentProvider(new ProvElementContentProvider());
		labelProvider = new RepositoryDetailsLabelProvider();
		repositoryViewer.setLabelProvider(labelProvider);

		// Edit the nickname
		repositoryViewer.setCellModifier(new ICellModifier() {
			public boolean canModify(Object element, String property) {
				return element instanceof MetadataRepositoryElement;
			}

			public Object getValue(Object element, String property) {
				return ((MetadataRepositoryElement) element).getName();
			}

			public void modify(Object element, String property, Object value) {
				if (value != null && value.toString().length() >= 0) {
					MetadataRepositoryElement repo;
					if (element instanceof Item) {
						repo = (MetadataRepositoryElement) ((Item) element).getData();
					} else if (element instanceof MetadataRepositoryElement) {
						repo = (MetadataRepositoryElement) element;
					} else {
						return;
					}
					if (!value.toString().equals(repo.getName())) {
						changed = true;
						repo.setNickname(value.toString());
						if (comparator.getSortKey() == RepositoryDetailsLabelProvider.COL_NAME)
							repositoryViewer.refresh(true);
						else
							repositoryViewer.update(repo, null);
					}
				}
			}

		});
		repositoryViewer.setColumnProperties(new String[] {"nickname"}); //$NON-NLS-1$
		repositoryViewer.setCellEditors(new CellEditor[] {new TextCellEditor(repositoryViewer.getTable())});

		repositoryViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (policy.getRepositoriesVisible())
					validateButtons();
				setDetails();
			}
		});

		repositoryViewer.setCheckStateProvider(new ICheckStateProvider() {
			public boolean isChecked(Object element) {
				return ((MetadataRepositoryElement) element).isEnabled();
			}

			public boolean isGrayed(Object element) {
				return false;
			}
		});

		repositoryViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				MetadataRepositoryElement element = (MetadataRepositoryElement) event.getElement();
				element.setEnabled(event.getChecked());
				// paranoid that an equal but not identical element was passed in as the selection.
				// update the cache map just in case.
				getInput().put(element);
				// update the viewer to show the change
				updateForEnablementChange(new MetadataRepositoryElement[] {element});
			}
		});

		// Input last
		repositoryViewer.setInput(getInput());

		// Drop targets and vertical buttons only if repository manipulation is provided.
		if (policy.getRepositoriesVisible()) {
			DropTarget target = new DropTarget(table, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
			target.setTransfer(new Transfer[] {URLTransfer.getInstance(), FileTransfer.getInstance()});
			target.addDropListener(new RepositoryManipulatorDropTarget(ui, table));

			// Vertical buttons
			Composite verticalButtonBar = createVerticalButtonBar(composite);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(verticalButtonBar);
			listener = getViewerProvisioningListener();

			ProvUI.getProvisioningEventBus(ui.getSession()).addListener(listener);
			composite.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent event) {
					ProvUI.getProvisioningEventBus(ui.getSession()).removeListener(listener);
				}
			});

			validateButtons();
		}

		// Details area
		details = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_SITEDETAILS_HEIGHT);
		data.widthHint = 1;

		details.setLayoutData(data);

		GridLayoutFactory.fillDefaults()
			.numColumns(policy.getRepositoriesVisible() ? 2 : 1)
			.margins(LayoutConstants.getMargins())
			.spacing(LayoutConstants.getSpacing())
			.generateLayout(composite);

		Dialog.applyDialogFont(composite);
		return composite;
	}

	private Button createVerticalButton(Composite parent, String label, boolean defaultButton) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);

		GridData data = setVerticalButtonLayoutData(button);
		data.horizontalAlignment = GridData.FILL;

		button.setToolTipText(label);
		if (defaultButton) {
			Shell shell = parent.getShell();
			if (shell != null) {
				shell.setDefaultButton(button);
			}
		}
		return button;
	}

	private GridData setVerticalButtonLayoutData(Button button) {
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		button.setLayoutData(data);
		return data;
	}

	private void setTableColumns() {
		table.setHeaderVisible(true);
		String[] columnHeaders;
		if (policy.getRepositoriesVisible())
			columnHeaders = new String[] {ProvUIMessages.RepositoryManipulationPage_NameColumnTitle, ProvUIMessages.RepositoryManipulationPage_LocationColumnTitle, ProvUIMessages.RepositoryManipulationPage_EnabledColumnTitle};
		else
			columnHeaders = new String[] {ProvUIMessages.RepositoryManipulationPage_NameColumnTitle, ProvUIMessages.RepositoryManipulationPage_LocationColumnTitle};
		for (int i = 0; i < columnHeaders.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columnHeaders[i]);
			if (i == RepositoryDetailsLabelProvider.COL_ENABLEMENT) {
				tc.setWidth(convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_SMALL_COLUMN_WIDTH));
				tc.setAlignment(SWT.CENTER);
			} else if (i == RepositoryDetailsLabelProvider.COL_NAME) {
				tc.setWidth(convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_COLUMN_WIDTH));
			} else {
				tc.setWidth(convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH));
			}
			tc.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					columnSelected((TableColumn) e.widget);
				}

				public void widgetSelected(SelectionEvent e) {
					columnSelected((TableColumn) e.widget);
				}

			});
			// First column only
			if (i == 0) {
				table.setSortColumn(tc);
				table.setSortDirection(SWT.UP);
			}
		}
	}

	private Composite createVerticalButtonBar(Composite parent) {
		// Create composite.
		Composite composite = new Composite(parent, SWT.NONE);
		initializeDialogUnits(composite);

		// create a layout with spacing and margins appropriate for the font
		// size.
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 5;
		layout.marginHeight = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);

		createVerticalButtons(composite);
		return composite;
	}

	private void createVerticalButtons(Composite parent) {
		addButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_Add, false);
		addButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				addRepository();
			}
		});

		editButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_Edit, false);
		editButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				changeRepositoryProperties();
			}
		});

		removeButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_Remove, false);
		removeButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				removeRepositories();
			}
		});

		refreshButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_RefreshConnection, false);
		refreshButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				refreshRepository();
			}
		});

		disableButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_DisableButton, false);
		disableButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				toggleRepositoryEnablement();
			}
		});

		Button button = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_Import, false);
		button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				importRepositories();
			}
		});

		exportButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationPage_Export, false);
		exportButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				exportRepositories();
			}
		});
	}

	CachedMetadataRepositories getInput() {
		if (input == null)
			input = new CachedMetadataRepositories();
		return input;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		if (changed)
			ElementUtils.updateRepositoryUsingElements(ui, getElements());
		originalNameCache.clear();
		originalURICache.clear();
		return super.performOk();
	}

	private StructuredViewerProvisioningListener getViewerProvisioningListener() {
		return new StructuredViewerProvisioningListener(RepositoryManipulationPage.this.getClass().getName(), repositoryViewer, ProvUIProvisioningListener.PROV_EVENT_METADATA_REPOSITORY, ui.getOperationRunner()) {
			protected void repositoryDiscovered(RepositoryEvent e) {
				RepositoryManipulationPage.this.safeRefresh(null);
			}

			protected void repositoryChanged(RepositoryEvent e) {
				RepositoryManipulationPage.this.safeRefresh(null);
			}
		};
	}

	MetadataRepositoryElement[] getElements() {
		return getInput().getElements();
	}

	MetadataRepositoryElement[] getSelectedElements() {
		Object[] items = ((IStructuredSelection) repositoryViewer.getSelection()).toArray();
		ArrayList<Object> list = new ArrayList<Object>(items.length);
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof MetadataRepositoryElement)
				list.add(items[i]);
		}
		return list.toArray(new MetadataRepositoryElement[list.size()]);
	}

	void validateButtons() {
		MetadataRepositoryElement[] elements = getSelectedElements();
		exportButton.setEnabled(elements.length > 0);
		removeButton.setEnabled(elements.length > 0);
		editButton.setEnabled(elements.length == 1);
		refreshButton.setEnabled(elements.length == 1);
		if (elements.length >= 1) {
			if (toggleMeansDisable(elements))
				disableButton.setText(ProvUIMessages.RepositoryManipulationPage_DisableButton);
			else
				disableButton.setText(ProvUIMessages.RepositoryManipulationPage_EnableButton);
			disableButton.setEnabled(true);
		} else {
			disableButton.setText(ProvUIMessages.RepositoryManipulationPage_EnableButton);
			disableButton.setEnabled(false);
		}
	}

	void addRepository() {
		AddRepositoryDialog dialog = new AddRepositoryDialog(getShell(), ui) {
			protected RepositoryTracker getRepositoryTracker() {
				return RepositoryManipulationPage.this.getLocalCacheRepoTracker();
			}
		};
		dialog.setTitle(ProvUIMessages.ColocatedRepositoryManipulator_AddSiteOperationLabel);
		dialog.open();
	}

	void refreshRepository() {
		final MetadataRepositoryElement[] selected = getSelectedElements();
		final ProvisionException[] fail = new ProvisionException[1];
		final boolean[] remove = new boolean[1];
		remove[0] = false;
		if (selected.length != 1)
			return;
		final URI location = selected[0].getLocation();
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					monitor.beginTask(NLS.bind(ProvUIMessages.RepositoryManipulationPage_ContactingSiteMessage, location), 100);
					try {
						// Batch the events for this operation so that any events on reload (discovery, etc.) will be ignored
						// in the UI as they happen.
						ui.signalRepositoryOperationStart();
						tracker.clearRepositoryNotFound(location);
						// If the managers don't know this repo, refreshing it will not work.
						// We temporarily add it, but we must remove it in case the user cancels out of this page.
						if (!includesRepo(tracker.getKnownRepositories(ui.getSession()), location)) {
							remove[0] = true;
							// We don't want to use the tracker here because it ensures that additions are
							// reported as user events to be responded to.  We don't want, for example, the
							// install wizard to change combo selections based on what is done here.
							ProvUI.getMetadataRepositoryManager(ui.getSession()).addRepository(location);
							ProvUI.getArtifactRepositoryManager(ui.getSession()).addRepository(location);
						}
						// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=312332
						// We assume repository colocation here.  Ideally we should not do this, but the
						// RepositoryTracker API is swallowing the refresh errors.
						SubMonitor sub = SubMonitor.convert(monitor, 200);
						try {
							ProvUI.getMetadataRepositoryManager(ui.getSession()).refreshRepository(location, sub.newChild(100));
						} catch (ProvisionException e) {
							fail[0] = e;
						}
						try {
							ProvUI.getArtifactRepositoryManager(ui.getSession()).refreshRepository(location, sub.newChild(100));
						} catch (ProvisionException e) {
							// Failure in the artifact repository.  We will not report this because the user has no separate visibility
							// of the artifact repository.  We should log the error.  If this repository fails during a download, the error
							// will be reported at that time to the user, when it matters.  This also prevents false error reporting when
							// a metadata repository didn't actually have a colocated artifact repository.
							LogHelper.log(e);
						}
					} catch (OperationCanceledException e) {
						// Catch canceled login attempts
						fail[0] = new ProvisionException(new Status(IStatus.CANCEL, ProvUIActivator.PLUGIN_ID, ProvUIMessages.RepositoryManipulationPage_RefreshOperationCanceled, e));
					} finally {
						// Check if the monitor was canceled
						if (fail[0] == null && monitor.isCanceled())
							fail[0] = new ProvisionException(new Status(IStatus.CANCEL, ProvUIActivator.PLUGIN_ID, ProvUIMessages.RepositoryManipulationPage_RefreshOperationCanceled));
						// If we temporarily added a repo so we could read it, remove it.
						if (remove[0]) {
							ProvUI.getMetadataRepositoryManager(ui.getSession()).removeRepository(location);
							ProvUI.getArtifactRepositoryManager(ui.getSession()).removeRepository(location);
						}
						ui.signalRepositoryOperationComplete(null, false);
					}
				}
			});
		} catch (InvocationTargetException e) {
			// nothing to report
		} catch (InterruptedException e) {
			// nothing to report
		}
		if (fail[0] != null) {
			// If the repo was not found, tell ProvUI that we will be reporting it.
			// We are going to report problems directly to the status manager because we
			// do not want the automatic repo location editing to kick in.
			if (fail[0].getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND) {
				tracker.addNotFound(location);
			}
			if (!fail[0].getStatus().matches(IStatus.CANCEL)) {
				// An error is only shown if the dialog was not canceled
				ProvUI.handleException(fail[0], null, StatusManager.SHOW);
			}
		} else {
			// Confirm that it was successful
			MessageDialog.openInformation(getShell(), ProvUIMessages.RepositoryManipulationPage_TestConnectionTitle, NLS.bind(ProvUIMessages.RepositoryManipulationPage_TestConnectionSuccess, URIUtil.toUnencodedString(location)));
		}
		repositoryViewer.update(selected[0], null);
		setDetails();
	}

	boolean includesRepo(URI[] repos, URI repo) {
		for (int i = 0; i < repos.length; i++)
			if (repos[i].equals(repo))
				return true;
		return false;
	}

	void toggleRepositoryEnablement() {
		MetadataRepositoryElement[] selected = getSelectedElements();
		if (selected.length >= 1) {
			boolean enableSites = !toggleMeansDisable(selected);
			for (int i = 0; i < selected.length; i++) {
				selected[i].setEnabled(enableSites);
				getInput().put(selected[i]);
			}
			updateForEnablementChange(selected);
		}
		validateButtons();
	}

	void updateForEnablementChange(MetadataRepositoryElement[] updated) {
		if (comparator.getSortKey() == RepositoryDetailsLabelProvider.COL_ENABLEMENT)
			repositoryViewer.refresh(true);
		else
			for (int i = 0; i < updated.length; i++)
				repositoryViewer.update(updated[i], null);
		changed = true;
	}

	void importRepositories() {
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				MetadataRepositoryElement[] imported = UpdateManagerCompatibility.importSites(getShell());
				if (imported.length > 0) {
					changed = true;
					for (int i = 0; i < imported.length; i++)
						getInput().put(imported[i]);
					safeRefresh(null);
				}
			}
		});
	}

	void exportRepositories() {
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				MetadataRepositoryElement[] elements = getSelectedElements();
				if (elements.length == 0)
					elements = getElements();
				UpdateManagerCompatibility.exportSites(getShell(), elements);
			}
		});
	}

	void changeRepositoryProperties() {
		final MetadataRepositoryElement[] selected = getSelectedElements();
		if (selected.length != 1)
			return;

		URI originalLocation = null;
		String originalName = null;
		if (originalURICache.containsKey(selected[0])) {
			originalLocation = originalURICache.get(selected[0]);
		} else
			originalLocation = selected[0].getLocation();
		if (originalNameCache.containsKey(selected[0])) {
			originalName = originalNameCache.get(selected[0]);
		} else
			originalName = selected[0].getName();
		final URI existingLocation = originalLocation;
		RepositoryNameAndLocationDialog dialog = new RepositoryNameAndLocationDialog(getShell(), ui) {
			protected String getInitialLocationText() {
				return URIUtil.toUnencodedString(selected[0].getLocation());
			}

			protected String getInitialNameText() {
				return selected[0].getName();
			}

			@Override
			protected URI getOriginalLocation() {
				return existingLocation;
			}
		};

		int retCode = dialog.open();
		if (retCode == Window.OK && dialog.getLocation() != null) {
			selected[0].setNickname(dialog.getName());
			selected[0].setLocation(dialog.getLocation());
			if (dialog.getLocation().equals(existingLocation)) {
				// the change is reverted
				originalURICache.remove(selected[0]);
			} else if (!originalURICache.containsKey(selected[0]))
				originalURICache.put(selected[0], existingLocation);
			if (dialog.getName().equals(originalName)) {
				// the change is reverted
				originalNameCache.remove(selected[0]);
			} else if (!originalNameCache.containsKey(selected[0]))
				originalNameCache.put(selected[0], originalName);
			if (originalURICache.size() > 0 || originalNameCache.size() > 0)
				changed = true;
			else
				changed = false;
			repositoryViewer.update(selected[0], null);
			setDetails();
		}
	}

	void columnSelected(TableColumn tc) {
		TableColumn[] cols = table.getColumns();
		for (int i = 0; i < cols.length; i++) {
			if (cols[i] == tc) {
				if (i != comparator.getSortKey()) {
					comparator.setSortKey(i);
					table.setSortColumn(tc);
					comparator.sortAscending();
					table.setSortDirection(SWT.UP);
				} else {
					if (comparator.isAscending()) {
						table.setSortDirection(SWT.DOWN);
						comparator.sortDescending();
					} else {
						table.setSortDirection(SWT.UP);
						comparator.sortAscending();
					}
				}
				repositoryViewer.refresh();
				break;
			}
		}
	}

	void safeRefresh(final MetadataRepositoryElement elementToSelect) {
		Runnable runnable = new Runnable() {
			public void run() {
				repositoryViewer.refresh();
				if (elementToSelect != null)
					repositoryViewer.setSelection(new StructuredSelection(elementToSelect), true);
			}
		};
		if (Display.getCurrent() == null)
			display.asyncExec(runnable);
		else
			runnable.run();
	}

	void applyFilter() {
		String text = pattern.getText();
		if (text == DEFAULT_FILTER_TEXT)
			text = ""; //$NON-NLS-1$
		if (text.length() == 0)
			filter.setPattern(null);
		else
			filter.setPattern(text);
		if (filterJob != null)
			filterJob.cancel();
		filterJob = new WorkbenchJob("filter job") { //$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				if (!repositoryViewer.getTable().isDisposed())
					repositoryViewer.refresh();
				return Status.OK_STATUS;
			}

		};
		filterJob.setSystem(true);
		filterJob.schedule(FILTER_DELAY);
	}

	void setDetails() {
		MetadataRepositoryElement[] selections = getSelectedElements();
		if (selections.length == 1) {
			details.setText(selections[0].getDescription());
		} else {
			details.setText(""); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		noDefaultAndApplyButton();
		if (ui == null) {
			setProvisioningUI(ProvisioningUI.getDefaultUI());
		}
	}

	void removeRepositories() {
		MetadataRepositoryElement[] selections = getSelectedElements();
		if (selections.length > 0) {
			String message = ProvUIMessages.RepositoryManipulationPage_RemoveConfirmMessage;
			if (selections.length == 1)
				message = NLS.bind(ProvUIMessages.RepositoryManipulationPage_RemoveConfirmSingleMessage, URIUtil.toUnencodedString(selections[0].getLocation()));
			if (MessageDialog.openQuestion(getShell(), ProvUIMessages.RepositoryManipulationPage_RemoveConfirmTitle, message)) {

				changed = true;
				for (int i = 0; i < selections.length; i++) {
					getInput().remove(selections[i]);
				}
				safeRefresh(null);
			}
		}
	}

	// Return a repo manipulator that only operates on the local cache.
	// Labels and other presentation info are used from the original manipulator.
	RepositoryTracker getLocalCacheRepoTracker() {
		if (localCacheRepoManipulator == null)
			localCacheRepoManipulator = new RepositoryTracker() {
				public void addRepository(URI location, String nickname, ProvisioningSession session) {
					MetadataRepositoryElement element = getInput().get(location);
					if (element == null) {
						element = new MetadataRepositoryElement(getInput(), null, ui, location, true);
						getInput().put(element);
					}
					if (nickname != null)
						element.setNickname(nickname);
					changed = true;
					safeRefresh(element);
				}

				public URI[] getKnownRepositories(ProvisioningSession session) {
					return RepositoryManipulationPage.this.getKnownRepositories();
				}

				public void removeRepositories(URI[] repoLocations, ProvisioningSession session) {
					RepositoryManipulationPage.this.removeRepositories();
				}

				public void refreshRepositories(URI[] locations, ProvisioningSession session, IProgressMonitor monitor) {
					// Nothing to refresh in the local cache
				}

				public IStatus validateRepositoryLocation(ProvisioningSession session, URI location, boolean contactRepositories, IProgressMonitor monitor) {
					IStatus status = super.validateRepositoryLocation(session, location, contactRepositories, monitor);
					if (status.isOK()) {
						String repoString = URIUtil.toUnencodedString(location);
						int length = repoString.length();
						if (length > 0 && repoString.charAt(length - 1) == '/') {
							try {
								location = URIUtil.fromString(repoString.substring(0, length - 1));
							} catch (URISyntaxException e) {
								return status;
							}
							status = super.validateRepositoryLocation(session, location, contactRepositories, monitor);
						}
					}
					return status;

				}
			};
		return localCacheRepoManipulator;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.ui.ICopyable#copyToClipboard(org.eclipse.swt.widgets.Control)
	 */
	public void copyToClipboard(Control activeControl) {
		MetadataRepositoryElement[] elements = getSelectedElements();
		if (elements.length == 0)
			elements = getElements();
		String text = ""; //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < elements.length; i++) {
			buffer.append(labelProvider.getClipboardText(elements[i], CopyUtils.DELIMITER));
			if (i > 0)
				buffer.append(CopyUtils.NEWLINE);
		}
		text = buffer.toString();

		if (text.length() == 0)
			return;
		Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
		clipboard.setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
		clipboard.dispose();
	}

	// If more than half of the selected repos are enabled, toggle means disable.
	// Otherwise it means enable.
	private boolean toggleMeansDisable(MetadataRepositoryElement[] elements) {
		double count = 0;
		for (int i = 0; i < elements.length; i++)
			if (elements[i].isEnabled())
				count++;
		return (count / elements.length) > 0.5;
	}

	URI[] getKnownRepositories() {
		MetadataRepositoryElement[] elements = getElements();
		URI[] locations = new URI[elements.length];
		for (int i = 0; i < elements.length; i++)
			locations[i] = elements[i].getLocation();
		return locations;
	}
}
