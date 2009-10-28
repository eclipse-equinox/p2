/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.internal.p2.ui.viewers.MetadataRepositoryElementComparator;
import org.eclipse.equinox.internal.p2.ui.viewers.RepositoryDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
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
import org.eclipse.ui.progress.IElementCollector;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Page that allows users to update, add, remove, import, and
 * export repositories.  This page can be hosted inside a preference
 * dialog or inside its own dialog.  When hosting this page inside
 * a non-preference dialog, some of the dialog methods will likely 
 * have to call page methods.  The following snippet shows how to host
 * this page inside a TitleAreaDialog.
 * <pre>
 *		TitleAreaDialog dialog = new TitleAreaDialog(shell) {
 *
 *			RepositoryManipulationPage page;
 *
 *			protected Control createDialogArea(Composite parent) {
 *				page = new RepositoryManipulationPage(policy);
 *				page.createControl(parent);
 *				this.setTitle("Software Sites");
 *				this.setMessage("The enabled sites will be searched for software.  Disabled sites are ignored.);
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
 * @since 3.5
 */
public class RepositoryManipulationPage extends PreferencePage implements IWorkbenchPreferencePage, ICopyable {
	final static String DEFAULT_FILTER_TEXT = ProvUIMessages.RepositoryManipulationPage_DefaultFilterString;
	private final static int FILTER_DELAY = 200;

	StructuredViewerProvisioningListener listener;
	TableViewer repositoryViewer;
	Table table;
	Policy policy;
	Display display;
	boolean changed = false;
	MetadataRepositoryElementComparator comparator;
	RepositoryDetailsLabelProvider labelProvider;
	RepositoryManipulator manipulator;
	RepositoryManipulator localCacheRepoManipulator;
	CachedMetadataRepositories input;
	Text pattern, details;
	PatternFilter filter;
	WorkbenchJob filterJob;
	Button addButton, removeButton, editButton, refreshButton, disableButton, exportButton;

	class CachedMetadataRepositories extends MetadataRepositories {
		Hashtable cachedElements;

		CachedMetadataRepositories() {
			super(policy);
			setIncludeDisabledRepositories(manipulator != null);
		}

		public int getQueryType() {
			return QueryProvider.METADATA_REPOS;
		}

		public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor) {
			if (cachedElements == null) {
				super.fetchDeferredChildren(o, collector, monitor);
				// now we know we have children
				Object[] children = getChildren(o);
				cachedElements = new Hashtable(children.length);
				for (int i = 0; i < children.length; i++) {
					if (children[i] instanceof MetadataRepositoryElement)
						cachedElements.put(URIUtil.toUnencodedString(((MetadataRepositoryElement) children[i]).getLocation()), children[i]);
				}
				return;
			}
			// Use the cache rather than fetching children
			collector.add(cachedElements.values().toArray(), monitor);
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
	 * This method must be called before the contents are created.
	 * @param policy
	 */
	public void setPolicy(Policy policy) {
		this.policy = policy;
		manipulator = policy.getRepositoryManipulator();
	}

	protected Control createContents(Composite parent) {
		display = parent.getDisplay();
		// The help refers to the full-blown dialog.  No help if it's read only.
		if (manipulator != null)
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parent.getShell(), IProvHelpContextIds.REPOSITORY_MANIPULATION_DIALOG);

		Composite composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gd);

		GridLayout layout = new GridLayout();
		layout.numColumns = manipulator == null ? 1 : 2;
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);

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
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		pattern.setLayoutData(gd);

		// spacer to fill other column
		if (manipulator != null)
			new Label(composite, SWT.NONE);

		// Table of available repositories
		repositoryViewer = new TableViewer(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		table = repositoryViewer.getTable();

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
					changed = true;
					repo.setNickname(value.toString());
					if (comparator.getSortKey() == RepositoryDetailsLabelProvider.COL_NAME)
						repositoryViewer.refresh(true);
					else
						repositoryViewer.update(repo, null);
				}
			}

		});
		repositoryViewer.setColumnProperties(new String[] {"nickname"}); //$NON-NLS-1$
		repositoryViewer.setCellEditors(new CellEditor[] {new TextCellEditor(repositoryViewer.getTable())});

		repositoryViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (manipulator != null)
					validateButtons();
				setDetails();
			}
		});

		// Input last
		repositoryViewer.setInput(getInput());

		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		table.setLayoutData(data);

		// Drop targets and vertical buttons only if repository manipulation is provided.
		if (manipulator != null) {
			DropTarget target = new DropTarget(table, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
			target.setTransfer(new Transfer[] {URLTransfer.getInstance(), FileTransfer.getInstance()});
			target.addDropListener(new RepositoryManipulatorDropTarget(getRepositoryManipulator(), table));

			// Vertical buttons
			Composite verticalButtonBar = createVerticalButtonBar(composite);
			data = new GridData(SWT.FILL, SWT.FILL, false, false);
			data.verticalAlignment = SWT.TOP;
			data.verticalIndent = 0;
			verticalButtonBar.setLayoutData(data);
			listener = getViewerProvisioningListener();

			ProvUI.addProvisioningListener(listener);
			composite.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent event) {
					ProvUI.removeProvisioningListener(listener);
				}
			});

			validateButtons();
		}

		// Details area
		details = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
		data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_SITEDETAILS_HEIGHT);

		details.setLayoutData(data);

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
		if (manipulator != null)
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

	public boolean performOk() {
		if (changed)
			ElementUtils.updateRepositoryUsingElements(getElements(), getShell());
		return super.performOk();
	}

	private StructuredViewerProvisioningListener getViewerProvisioningListener() {
		return new StructuredViewerProvisioningListener(repositoryViewer, ProvUIProvisioningListener.PROV_EVENT_METADATA_REPOSITORY) {
			protected void repositoryDiscovered(RepositoryEvent e) {
				RepositoryManipulationPage.this.asyncRefresh(null);
			}

			protected void repositoryChanged(RepositoryEvent e) {
				RepositoryManipulationPage.this.asyncRefresh(null);
			}
		};
	}

	MetadataRepositoryElement[] getElements() {
		return (MetadataRepositoryElement[]) getInput().cachedElements.values().toArray(new MetadataRepositoryElement[getInput().cachedElements.size()]);
	}

	MetadataRepositoryElement[] getSelectedElements() {
		Object[] items = ((IStructuredSelection) repositoryViewer.getSelection()).toArray();
		ArrayList list = new ArrayList(items.length);
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof MetadataRepositoryElement)
				list.add(items[i]);
		}
		return (MetadataRepositoryElement[]) list.toArray(new MetadataRepositoryElement[list.size()]);
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
		AddRepositoryDialog dialog = new AddRepositoryDialog(getShell(), policy) {
			protected RepositoryManipulator getRepositoryManipulator() {
				return RepositoryManipulationPage.this.getRepositoryManipulator();
			}
		};
		dialog.setTitle(manipulator.getAddOperationLabel());
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
					SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(ProvUIMessages.RepositoryManipulationPage_ContactingSiteMessage, location), 300);
					try {
						ProvUI.clearRepositoryNotFound(location);
						// If the manager doesn't know this repo, refreshing it will not work.
						// We temporarily add it, but we must remove it in case the user cancels out of this page.
						if (!includesRepo(manipulator.getKnownRepositories(), location)) {
							// Start a batch operation so we can swallow events
							ProvUI.startBatchOperation();
							AddRepositoryOperation op = manipulator.getAddOperation(location);
							op.setNotify(false);
							op.execute(mon.newChild(100));
							remove[0] = true;
						}
						ProvisioningUtil.refreshArtifactRepositories(new URI[] {location}, mon.newChild(100));
						ProvisioningUtil.refreshMetadataRepositories(new URI[] {location}, mon.newChild(100));
					} catch (ProvisionException e) {
						// Need to report after dialog is closed or the error dialog will disappear when progress
						// disappears
						fail[0] = e;
					} catch (OperationCanceledException e) {
						// Catch canceled login attempts
						fail[0] = new ProvisionException(new Status(IStatus.CANCEL, ProvUIActivator.PLUGIN_ID, ProvUIMessages.RepositoryManipulationPage_RefreshOperationCanceled, e));
					} finally {
						// Check if the monitor was canceled
						if (fail[0] == null && mon.isCanceled())
							fail[0] = new ProvisionException(new Status(IStatus.CANCEL, ProvUIActivator.PLUGIN_ID, ProvUIMessages.RepositoryManipulationPage_RefreshOperationCanceled));
						// If we temporarily added a repo so we could read it, remove it.
						if (remove[0]) {
							RemoveRepositoryOperation op = manipulator.getRemoveOperation(new URI[] {location});
							op.setNotify(false);
							try {
								op.execute(new NullProgressMonitor());
							} catch (ProvisionException e) {
								// Don't report
							}
							// stop swallowing events
							ProvUI.endBatchOperation(false);
						}
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
				ProvUI.notFoundStatusReported(location);
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
			for (int i = 0; i < selected.length; i++)
				selected[i].setEnabled(enableSites);
			if (comparator.getSortKey() == RepositoryDetailsLabelProvider.COL_ENABLEMENT)
				repositoryViewer.refresh(true);
			else
				for (int i = 0; i < selected.length; i++)
					repositoryViewer.update(selected[i], null);
			changed = true;
		}
		validateButtons();
	}

	void importRepositories() {
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				MetadataRepositoryElement[] imported = UpdateManagerCompatibility.importSites(getShell());
				if (imported.length > 0) {
					Hashtable repos = getInput().cachedElements;
					changed = true;
					for (int i = 0; i < imported.length; i++)
						repos.put(URIUtil.toUnencodedString(imported[i].getLocation()), imported[i]);
					asyncRefresh(null);
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
		RepositoryNameAndLocationDialog dialog = new RepositoryNameAndLocationDialog(getShell(), policy) {
			protected String getInitialLocationText() {
				return URIUtil.toUnencodedString(selected[0].getLocation());
			}

			protected String getInitialNameText() {
				return selected[0].getName();
			}

			protected RepositoryLocationValidator getRepositoryLocationValidator() {
				return new RepositoryLocationValidator() {
					public IStatus validateRepositoryLocation(URI uri, boolean contactRepositories, IProgressMonitor monitor) {
						if (URIUtil.sameURI(uri, selected[0].getLocation()))
							return Status.OK_STATUS;
						return RepositoryManipulationPage.this.getRepositoryManipulator().getRepositoryLocationValidator(getShell()).validateRepositoryLocation(uri, contactRepositories, monitor);
					}
				};
			}

		};
		int retCode = dialog.open();
		if (retCode == Window.OK) {
			selected[0].setNickname(dialog.getName());
			selected[0].setLocation(dialog.getLocation());
			changed = true;
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

	void asyncRefresh(final MetadataRepositoryElement elementToSelect) {
		display.asyncExec(new Runnable() {
			public void run() {
				repositoryViewer.refresh();
				if (elementToSelect != null)
					repositoryViewer.setSelection(new StructuredSelection(elementToSelect), true);
			}
		});
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
		if (policy == null)
			setPolicy(Policy.getDefault());
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
					getInput().cachedElements.remove(URIUtil.toUnencodedString(selections[i].getLocation()));
				}
				asyncRefresh(null);
			}
		}
	}

	// Return a repo manipulator that only operates on the local cache.
	// Labels and other presentation info are used from the original manipulator.
	RepositoryManipulator getRepositoryManipulator() {
		if (localCacheRepoManipulator == null)
			localCacheRepoManipulator = new RepositoryManipulator() {
				public AddRepositoryOperation getAddOperation(URI location) {
					return new AddRepositoryOperation("Cached add repo operation", new URI[] {location}) { //$NON-NLS-1$
						protected IStatus doExecute(IProgressMonitor monitor) {
							MetadataRepositoryElement element = null;
							for (int i = 0; i < locations.length; i++) {
								element = new MetadataRepositoryElement(getInput(), locations[i], true);
								if (nicknames != null)
									element.setNickname(nicknames[i]);
								getInput().cachedElements.put(URIUtil.toUnencodedString(locations[i]), element);
							}
							changed = true;
							asyncRefresh(element);
							return Status.OK_STATUS;
						}

						protected IStatus doBatchedExecute(IProgressMonitor monitor) {
							// Not called due to override of doExecute
							return null;
						}

						protected void setNickname(URI loc, String nickname) {
							// Not called due to override of doExecute
						}
					};
				}

				public String getAddOperationLabel() {
					return manipulator.getAddOperationLabel();
				}

				public URI[] getKnownRepositories() {
					return RepositoryManipulationPage.this.getKnownRepositories();
				}

				public String getManipulatorButtonLabel() {
					return manipulator.getManipulatorButtonLabel();
				}

				public String getManipulatorLinkLabel() {
					return manipulator.getManipulatorLinkLabel();
				}

				public RemoveRepositoryOperation getRemoveOperation(URI[] repoLocations) {
					return new RemoveRepositoryOperation("Cached remove repo operation", repoLocations) { //$NON-NLS-1$
						protected IStatus doBatchedExecute(IProgressMonitor monitor) {
							removeRepositories();
							return Status.OK_STATUS;
						}
					};
				}

				public String getRemoveOperationLabel() {
					return manipulator.getRemoveOperationLabel();
				}

				public RepositoryLocationValidator getRepositoryLocationValidator(Shell shell) {
					return new DefaultMetadataURLValidator() {
						protected URI[] getKnownLocations() {
							return getKnownRepositories();
						}
					};
				}

				public boolean manipulateRepositories(Shell shell) {
					// we are the manipulator
					return true;
				}

				public String getManipulatorInstructionString() {
					// we are the manipulator
					return null;
				}

				public String getRepositoryNotFoundInstructionString() {
					// we are in the manipulator, no further instructions
					return null;
				}

			};
		return localCacheRepoManipulator;
	}

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
