package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.IViewMenuProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.DeferredQueryContentListener;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.DeferredQueryContentProvider;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.JFaceResources;
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
 * FilteredTree extension that provides a hook for menu creation,
 * and forces synchronous fetching of the tree when the first 
 * filtering is performed.
 * 
 * @since 3.4
 *
 */
public class DeferredFetchFilteredTree extends FilteredTree {

	ToolBar toolBar;
	MenuManager menuManager;
	ToolItem viewMenuButton;
	FilteredTree tree;
	Display display;
	PatternFilter patternFilter;
	IViewMenuProvider viewMenuProvider;
	DeferredQueryContentProvider contentProvider;

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

	protected Composite createFilterControls(Composite filterParent) {
		super.createFilterControls(filterParent);
		Object layout = filterParent.getLayout();
		if (layout instanceof GridLayout) {
			((GridLayout) layout).numColumns++;
		}
		if (viewMenuProvider != null)
			createViewMenu(filterParent);
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
		WorkbenchJob job = super.doCreateRefreshJob();
		job.addJobChangeListener(new JobChangeAdapter() {
			public void aboutToRun(IJobChangeEvent event) {
				display.syncExec(new Runnable() {
					public void run() {
						String text = getFilterString();
						// If we are about to filter and there is
						// actually filtering to do, set the content
						// provider to synchronous mode.
						if (text == null || (initialText != null && initialText.equals(text)))
							return;
						contentProvider.setSynchronous(true);
					}
				});
			}
		});
		return job;
	}
}