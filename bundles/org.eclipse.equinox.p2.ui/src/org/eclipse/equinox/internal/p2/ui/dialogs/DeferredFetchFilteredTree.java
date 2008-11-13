package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.provisional.p2.ui.QueryableMetadataRepositoryManager;

import org.eclipse.equinox.internal.p2.ui.model.QueriedElement;

import org.eclipse.equinox.internal.p2.ui.viewers.IDeferredQueryContentListener;
import org.eclipse.equinox.internal.p2.ui.viewers.DeferredQueryContentProvider;

import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
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
	private static final String WAIT_STRING = ProvUIMessages.DeferredFetchFilteredTree_RetrievingList;

	ToolBar toolBar;
	MenuManager menuManager;
	ToolItem viewMenuButton;
	Display display;
	PatternFilter patternFilter;
	IViewMenuProvider viewMenuProvider;
	DeferredQueryContentProvider contentProvider;
	InputSchedulingRule filterRule;
	String savedFilterText;
	Job loadJob;
	WorkbenchJob filterJob;
	ControlEnableState enableState;
	Object viewerInput;
	ArrayList checkState = new ArrayList();
	ContainerCheckedTreeViewer checkboxViewer;

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

	public DeferredFetchFilteredTree(Composite parent, int treeStyle, PatternFilter filter, final IViewMenuProvider viewMenuProvider, Display display) {
		super(parent);
		this.display = display;
		this.viewMenuProvider = viewMenuProvider;
		this.patternFilter = filter;
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
		checkboxViewer = new ContainerCheckedTreeViewer(composite, style);
		checkboxViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				// We use an additive check state cache so we need to remove
				// previously checked items if the user unchecked them.
				if (!event.getChecked() && checkState != null) {
					Iterator iter = checkState.iterator();
					ArrayList toRemove = new ArrayList(1);
					while (iter.hasNext()) {
						Object element = iter.next();
						if (checkboxViewer.getComparer().equals(element, event.getElement())) {
							toRemove.add(element);
							// Do not break out of the loop.  We may have duplicate equal
							// elements in the cache.  Since the cache is additive, we want
							// to be sure we've gotten everything.
						}
					}
					checkState.removeAll(toRemove);
				}

			}
		});
		return checkboxViewer;
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
		deferredProvider.addListener(new IDeferredQueryContentListener() {
			public void inputChanged(Viewer v, Object oldInput, Object newInput) {
				if (newInput == null)
					return;
				// Store the input because it's not reset in the viewer until
				// after this listener is run.
				viewerInput = newInput;

				// Reset the state for remembering check marks
				checkState = new ArrayList();
				// Cancel the load and filter jobs and null out the scheduling rule
				// so that a new one will be created on the new input when needed.
				filterRule = null;
				cancelLoadJob();
				cancelAndResetFilterJob();
				contentProvider.setSynchronous(false);

				if (showFilterControls && filterText != null && !filterText.isDisposed()) {
					// We cancelled the load and if it was in progress the filter
					// would have been disabled.  
					restoreAfterLoading(getInitialText());
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
		// See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=229735
		// Ideally we would not have to copy the filtering job, but this
		// gives us the most precise control over how and when to preserve
		// the check mark state.  We have modified the superclass job so
		// that everything is expanded rather than recursively expanding
		// the tree and checking for the stop time.  This simplifies the
		// restoration of the correct checkmarks.
		filterJob = new WorkbenchJob("Refresh Filter") {//$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (treeViewer.getControl().isDisposed()) {
					return Status.CANCEL_STATUS;
				}

				String text = getFilterString();
				if (text == null) {
					return Status.OK_STATUS;
				}

				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				boolean initial = initialText != null && initialText.equals(text);
				if (initial) {
					patternFilter.setPattern(null);
				} else if (text != null) {
					patternFilter.setPattern(text);
				}

				Control redrawFalseControl = treeComposite != null ? treeComposite : treeViewer.getControl();
				try {
					// don't want the user to see updates that will be made to
					// the tree
					// we are setting redraw(false) on the composite to avoid
					// dancing scrollbar
					redrawFalseControl.setRedraw(false);
					treeViewer.getTree().setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));
					rememberLeafCheckState();
					treeViewer.refresh(true);
					// The superclass did a recursive expand so it could be more responsive to subsequent
					// typing.  We are expanding all so that we know everything is realized when we go to
					// restore the check state afterward.
					treeViewer.expandAll();
					if (text.length() > 0 && !initial) {
						// enabled toolbar - there is text to clear
						// and the list is currently being filtered
						updateToolbar(true);
					} else {
						// disabled toolbar - there is no text to clear
						// and the list is currently not filtered
						updateToolbar(false);
					}
				} finally {
					// done updating the tree - set redraw back to true
					TreeItem[] items = getViewer().getTree().getItems();
					if (items.length > 0 && getViewer().getTree().getSelectionCount() == 0) {
						treeViewer.getTree().setTopItem(items[0]);
					}
					restoreLeafCheckState();
					redrawFalseControl.setRedraw(true);
					treeViewer.getTree().setCursor(null);
				}
				return Status.OK_STATUS;
			}
		};

		filterJob.addJobChangeListener(new JobChangeAdapter() {
			public void aboutToRun(final IJobChangeEvent event) {
				final boolean[] shouldLoad = new boolean[1];
				shouldLoad[0] = false;
				display.syncExec(new Runnable() {
					public void run() {
						if (filterText != null && !filterText.isDisposed()) {
							String text = getFilterString();
							// If we are about to filter and there is
							// actually filtering to do, force a load
							// of the input and set the content
							// provider to synchronous mode.  We want the
							// load job to complete before continuing with filtering.
							if (text == null || (initialText != null && initialText.equals(text)))
								return;
							if (!contentProvider.getSynchronous() && loadJob == null) {
								if (filterText != null && !filterText.isDisposed()) {
									disableWhileLoading();
									shouldLoad[0] = true;
								}
							}
						}
					}
				});
				if (shouldLoad[0]) {
					event.getJob().sleep();
					scheduleLoadJob();
				}

			}

			public void done(IJobChangeEvent event) {
				// To be safe, we always reset the scheduling
				// rule because the input may have changed since the last run.
				event.getJob().setRule(getFilterJobSchedulingRule());
			}
		});
		filterJob.setRule(getFilterJobSchedulingRule());
		return filterJob;
	}

	void disableWhileLoading() {
		// We already disabled.
		if (enableState != null)
			return;
		// TODO Knowledge of our client's parent structure is cheating
		// but for now our only usage is in one particular widget tree and
		// we want to disable at the right place.
		if (parent != null && !parent.isDisposed()) {
			enableState = ControlEnableState.disable(parent.getParent());
		}
		if (filterText != null && !filterText.isDisposed()) {
			filterText.setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));
			getViewer().getTree().setCursor(display.getSystemCursor(SWT.CURSOR_WAIT));
			savedFilterText = filterText.getText();
			filterText.setText(WAIT_STRING);
		}

	}

	void restoreAfterLoading(String filterTextToRestore) {
		// If the filter text was previously disabled, reset the text to
		// the previous filter text.
		if (filterText != null && !filterText.isDisposed() && !filterText.isEnabled()) {
			filterText.setText(filterTextToRestore);
			filterText.setCursor(null);
			getViewer().getTree().setCursor(null);
			filterText.setSelection(filterTextToRestore.length(), filterTextToRestore.length());
		}
		// Now enable all of the controls
		if (enableState != null && parent != null && !parent.isDisposed()) {
			enableState.restore();
			enableState = null;
		}
		// Now set the focus back to the filter text
		if (filterText != null && !filterText.isDisposed())
			filterText.setFocus();
	}

	InputSchedulingRule getFilterJobSchedulingRule() {
		if (filterRule == null) {
			filterRule = new InputSchedulingRule(viewerInput);
		}
		return filterRule;
	}

	void scheduleLoadJob() {
		if (loadJob != null)
			return;
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
		loadJob.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					contentProvider.setSynchronous(true);
					display.asyncExec(new Runnable() {
						public void run() {
							// We have just loaded all content.  Trigger a viewer expand.
							// This really only need be done before filtering, but it can
							// be slow the very first time, so we may as well do it while
							// the user is already waiting rather than after they expect
							// things to be responsive.
							if (getViewer() != null && !getViewer().getTree().isDisposed()) {
								getViewer().expandAll();
							}
							restoreAfterLoading(savedFilterText);
						}
					});
					if (filterJob != null)
						filterJob.wakeUp();
				}
				loadJob = null;
			}
		});
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
			// callers have likely reset the filtering rule.
			// We can't reset it here because we don't know that
			// the job actually stopped, so we do it in the
			// done() handler.
		}
	}

	protected void textChanged() {
		// Don't refilter if we are merely resetting the filter back
		// to what it was before loading repositories
		if (filterText.getText().trim().equals(WAIT_STRING))
			return;
		super.textChanged();
	}

	void rememberLeafCheckState() {
		ContainerCheckedTreeViewer v = (ContainerCheckedTreeViewer) getViewer();
		Object[] checked = v.getCheckedElements();
		if (checkState == null)
			checkState = new ArrayList(checked.length);
		for (int i = 0; i < checked.length; i++)
			if (!v.getGrayed(checked[i]))
				checkState.add(checked[i]);
	}

	void restoreLeafCheckState() {
		if (checkboxViewer == null || checkboxViewer.getTree().isDisposed())
			return;
		if (checkState == null)
			return;

		checkboxViewer.setCheckedElements(new Object[0]);
		checkboxViewer.setGrayedElements(new Object[0]);
		// Now we are only going to set the check state of the leaf nodes
		// and rely on our container checked code to update the parents properly.
		Iterator iter = checkState.iterator();
		Object element = null;
		while (iter.hasNext()) {
			element = iter.next();
			if (!checkboxViewer.isExpandable(element)) {
				// setChecked does an internal expand
				checkboxViewer.setChecked(element, true);
			}
		}
		// We are only firing one event, knowing that this is enough for our listeners.
		if (element != null)
			checkboxViewer.fireCheckStateChanged(element, true);
	}

	public CheckboxTreeViewer getCheckboxTreeViewer() {
		return checkboxViewer;
	}
}