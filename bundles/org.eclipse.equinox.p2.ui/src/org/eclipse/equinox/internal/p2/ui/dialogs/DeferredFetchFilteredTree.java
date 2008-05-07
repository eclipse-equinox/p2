package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.IViewMenuProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement;
import org.eclipse.equinox.internal.provisional.p2.ui.query.QueryableMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.DeferredQueryContentListener;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.DeferredQueryContentProvider;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * FilteredTree extension that creates a check box tree,
 * provides a hook for menu creation, and forces synchronous 
 * fetching of the tree when the first 
 * filtering is performed.
 * 
 * @since 3.4
 *
 */
public class DeferredFetchFilteredTree extends FilteredTree {
	private static final long FILTER_DELAY_TIME = 200;
	private static final String WAIT_STRING = ProvUIMessages.DeferredFetchFilteredTree_RetrievingList;

	ToolBar toolBar;
	MenuManager menuManager;
	ToolItem viewMenuButton;
	Display display;
	PatternFilter patternFilter;
	IViewMenuProvider viewMenuProvider;
	DeferredQueryContentProvider contentProvider;
	boolean useCheckBoxTree = false;
	InputSchedulingRule filterRule;
	String savedFilterText;
	Job loadJob;
	WorkbenchJob filterJob;

	class InputSchedulingRule implements ISchedulingRule {
		Object input;

		InputSchedulingRule(Object input) {
			this.input = input;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse.core.runtime.jobs.ISchedulingRule)
		 */
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse.core.runtime.jobs.ISchedulingRule)
		 */
		public boolean isConflicting(ISchedulingRule rule) {
			if (rule instanceof InputSchedulingRule) {
				InputSchedulingRule other = (InputSchedulingRule) rule;
				if (input == null)
					return other.getInput() == null;
				return input.equals(other.getInput());
			}
			return false;
		}

		Object getInput() {
			return input;
		}
	}

	public DeferredFetchFilteredTree(Composite parent, int treeStyle, PatternFilter filter, final IViewMenuProvider viewMenuProvider, Display display, boolean useCheckBoxViewer) {
		super(parent);
		this.display = display;
		this.viewMenuProvider = viewMenuProvider;
		this.patternFilter = filter;
		this.useCheckBoxTree = useCheckBoxViewer;
		init(treeStyle, filter);
	}

	/*
	 * Overridden to see if filter controls were created.
	 * If they were not created, we need to create the view menu
	 * independently.  
	 * (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredTree#createControl(org.eclipse.swt.widgets.Composite, int)
	 */
	protected void createControl(Composite composite, int treeStyle) {
		super.createControl(composite, treeStyle);
		if (!showFilterControls && viewMenuProvider != null) {
			createViewMenu(composite);
		}
	}

	protected TreeViewer doCreateTreeViewer(Composite composite, int style) {
		if (useCheckBoxTree)
			return new ContainerCheckedTreeViewer(composite, style);
		return super.doCreateTreeViewer(composite, style);
	}

	protected Composite createFilterControls(Composite filterParent) {
		super.createFilterControls(filterParent);
		Object layout = filterParent.getLayout();
		if (layout instanceof GridLayout) {
			((GridLayout) layout).numColumns++;
		}
		if (viewMenuProvider != null)
			createViewMenu(filterParent);
		filterParent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				cancelLoadJob();
			}
		});
		return filterParent;
	}

	private void createViewMenu(Composite filterParent) {
		toolBar = new ToolBar(filterParent, SWT.FLAT);
		viewMenuButton = new ToolItem(toolBar, SWT.PUSH, 0);

		viewMenuButton.setImage(JFaceResources.getImage(PopupDialog.POPUP_IMG_MENU));
		viewMenuButton.setToolTipText(ProvUIMessages.AvailableIUGroup_ViewByToolTipText);
		viewMenuButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showViewMenu();
			}
		});

		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=177183
		toolBar.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				showViewMenu();
			}
		});

	}

	void showViewMenu() {
		if (menuManager == null) {
			menuManager = new MenuManager();
			viewMenuProvider.fillViewMenu(menuManager);
		}
		Menu menu = menuManager.createContextMenu(getShell());
		Rectangle bounds = toolBar.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = toolBar.getParent().toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}

	public void contentProviderSet(final DeferredQueryContentProvider deferredProvider) {
		this.contentProvider = deferredProvider;
		deferredProvider.addListener(new DeferredQueryContentListener() {
			public void inputChanged(Viewer v, Object oldInput, Object newInput) {
				if (newInput == null)
					return;
				// Cancel the load and filter jobs and null out the scheduling rule
				// so that a new one will be created on the new input when needed.
				cancelLoadJob();
				cancelAndResetFilterJob();
				filterRule = null;
				contentProvider.setSynchronous(false);

				if (showFilterControls && filterText != null && !filterText.isDisposed()) {
					filterText.setText(getInitialText());
					filterText.selectAll();
				}
			}

		});
	}

	/*
	 * Overridden to hook a listener on the job and set the deferred content provider
	 * to synchronous mode before a filter is done.
	 * @see org.eclipse.ui.dialogs.FilteredTree#doCreateRefreshJob()
	 */
	protected WorkbenchJob doCreateRefreshJob() {
		filterJob = super.doCreateRefreshJob();
		filterJob.addJobChangeListener(new JobChangeAdapter() {
			public void aboutToRun(final IJobChangeEvent event) {
				display.syncExec(new Runnable() {
					public void run() {
						String text = getFilterString();
						// If we are about to filter and there is
						// actually filtering to do, force a load
						// of the input and set the content
						// provider to synchronous mode.  We want the
						// load job to complete before continuing with filtering.
						if (text == null || (initialText != null && initialText.equals(text)))
							return;
						if (!contentProvider.getSynchronous()) {
							if (filterText != null && !filterText.isDisposed()) {
								filterText.setEnabled(false);
								filterText.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));
								savedFilterText = filterText.getText();
								filterText.setText(WAIT_STRING);
							}
							event.getJob().sleep();
							scheduleLoadJob();
							event.getJob().wakeUp(FILTER_DELAY_TIME);
							contentProvider.setSynchronous(true);
						}
					}
				});
			}

			public void done(IJobChangeEvent event) {
				display.asyncExec(new Runnable() {
					public void run() {
						restoreFilterText();
					}
				});
			}

			public void awake(IJobChangeEvent event) {
				display.asyncExec(new Runnable() {
					public void run() {
						restoreFilterText();
					}
				});
			}
		});
		filterJob.setRule(getFilterJobSchedulingRule());
		return filterJob;
	}

	void restoreFilterText() {
		if (filterText != null && !filterText.isDisposed() && !filterText.isEnabled()) {
			filterText.setText(savedFilterText);
			filterText.setEnabled(true);
			filterText.setCursor(null);
			filterText.setFocus();
			filterText.setSelection(savedFilterText.length(), savedFilterText.length());
		}
	}

	InputSchedulingRule getFilterJobSchedulingRule() {
		if (filterRule == null) {
			Object input = null;
			if (getViewer() != null)
				input = getViewer().getInput();
			filterRule = new InputSchedulingRule(input);
		}
		return filterRule;
	}

	void scheduleLoadJob() {
		cancelLoadJob();
		loadJob = new Job(WAIT_STRING) {
			protected IStatus run(IProgressMonitor monitor) {
				if (this.getRule() instanceof InputSchedulingRule) {
					Object input = ((InputSchedulingRule) this.getRule()).getInput();
					if (input instanceof QueriedElement)
						if (((QueriedElement) input).getQueryable() instanceof QueryableMetadataRepositoryManager) {
							QueryableMetadataRepositoryManager q = (QueryableMetadataRepositoryManager) ((QueriedElement) input).getQueryable();
							q.loadAll(monitor);
							if (monitor.isCanceled())
								return Status.CANCEL_STATUS;
						}
				}
				return Status.OK_STATUS;
			}
		};
		loadJob.setSystem(true);
		loadJob.setUser(false);
		loadJob.setRule(getFilterJobSchedulingRule());
		// Telling the operation runner about it ensures that listeners know we are running
		// a provisioning-related job.
		ProvisioningOperationRunner.manageJob(loadJob);
		loadJob.schedule();
	}

	void cancelLoadJob() {
		if (loadJob != null) {
			loadJob.cancel();
			loadJob = null;
		}
	}

	void cancelAndResetFilterJob() {
		if (filterJob != null) {
			filterJob.cancel();
			filterJob.setRule(getFilterJobSchedulingRule());
		}
	}

	protected void textChanged() {
		// Don't refilter if we are merely resetting the filter back
		// to what it was before loading repositories
		if (filterText.getText().trim().equals(WAIT_STRING))
			return;
		super.textChanged();
	}
}