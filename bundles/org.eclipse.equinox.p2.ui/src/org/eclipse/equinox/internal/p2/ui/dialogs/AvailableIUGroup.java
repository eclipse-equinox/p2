/*******************************************************************************
 *  Copyright (c) 2008, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.net.URI;
import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.query.IUViewQueryContext;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * An AvailableIUGroup is a reusable UI component that displays the
 * IU's available for installation.  By default, content from all available
 * repositories is shown.
 * 
 * @since 3.4
 */
public class AvailableIUGroup extends StructuredIUGroup {

	/**
	 * Show contents from all repositories
	 */
	public static final int AVAILABLE_ALL = 1;

	/**
	 * Don't show any repository content
	 */
	public static final int AVAILABLE_NONE = 2;

	/**
	 * Show local repository content
	 */
	public static final int AVAILABLE_LOCAL = 3;

	/**
	 * Show content for a specific repository
	 */
	public static final int AVAILABLE_SPECIFIED = 4;

	IUViewQueryContext queryContext;
	int filterConstant = AVAILABLE_ALL;
	URI repositoryFilter;
	QueryableMetadataRepositoryManager queryableManager;
	// We restrict the type of the filter used because PatternFilter does
	// unnecessary accesses of children that cause problems with the deferred
	// tree.
	AvailableIUPatternFilter filter;
	private boolean useBold = false;
	private IUDetailsLabelProvider labelProvider;
	private int repoFlags;
	Display display;
	DelayedFilterCheckboxTree filteredTree;
	Job lastRequestedLoadJob;

	/**
	 * Create a group that represents the available IU's from all available
	 * repositories.  The default policy controls the visibility flags for
	 * repositories and IU's.
	 * 
	 * @param parent the parent composite for the group
	 */
	public AvailableIUGroup(ProvisioningUI ui, final Composite parent) {
		this(ui, parent, parent.getFont(), null, getDefaultColumnConfig(), AVAILABLE_ALL);
	}

	private static IUColumnConfig[] getDefaultColumnConfig() {
		// increase primary column width because we might be nesting names under categories and require more space than a flat list
		IUColumnConfig nameColumn = new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, ILayoutConstants.DEFAULT_PRIMARY_COLUMN_WIDTH + 15);
		IUColumnConfig versionColumn = new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, ILayoutConstants.DEFAULT_COLUMN_WIDTH);
		return new IUColumnConfig[] {nameColumn, versionColumn};
	}

	/**
	 * Create a group that represents the available IU's.
	 * 
	 * @param ui the policy to use for deciding what should be shown
	 * @param parent the parent composite for the group
	 * @param font The font to use for calculating pixel sizes.  This font is
	 * not managed by the receiver.
	 * @param queryContext the IUViewQueryContext that determines additional
	 * information about what is shown, such as the visible repositories
	 * @param columnConfig the description of the columns that should be shown.  If <code>null</code>, a default
	 * will be used.
	 * @param filterConstant a constant specifying which repositories are used when showing content
	 */
	public AvailableIUGroup(ProvisioningUI ui, final Composite parent, Font font, IUViewQueryContext queryContext, IUColumnConfig[] columnConfig, int filterConstant) {
		super(ui, parent, font, columnConfig);
		this.display = parent.getDisplay();
		if (queryContext == null)
			this.queryContext = ProvUI.getQueryContext(getPolicy());
		else
			this.queryContext = queryContext;
		repoFlags = ui.getRepositoryTracker().getMetadataRepositoryFlags();
		this.queryableManager = new QueryableMetadataRepositoryManager(ui, false);
		this.filterConstant = filterConstant;
		this.filter = new AvailableIUPatternFilter(getColumnConfig());
		this.filter.setIncludeLeadingWildcard(true);
		createGroupComposite(parent);
	}

	protected StructuredViewer createViewer(Composite parent) {
		// Table of available IU's
		filteredTree = new DelayedFilterCheckboxTree(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, filter, getPreFilterJobProvider());
		final TreeViewer availableIUViewer = filteredTree.getViewer();

		availableIUViewer.getTree().setFont(parent.getFont());
		filteredTree.getFilterControl().setFont(parent.getFont());

		// If the user expanded or collapsed anything while we were loading a repo
		// in the background, we would not want to disrupt their work by making
		// a newly loaded visible and expanding it.  Setting the load job to null 
		// will take care of this.
		availableIUViewer.getTree().addTreeListener(new TreeListener() {
			public void treeCollapsed(TreeEvent e) {
				lastRequestedLoadJob = null;
			}

			public void treeExpanded(TreeEvent e) {
				lastRequestedLoadJob = null;
			}
		});

		labelProvider = new IUDetailsLabelProvider(filteredTree, getColumnConfig(), getShell());
		labelProvider.setUseBoldFontForFilteredItems(useBold);
		labelProvider.setToolTipProperty(IInstallableUnit.PROP_DESCRIPTION);

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(getColumnConfig());
		availableIUViewer.setComparator(comparator);
		availableIUViewer.setComparer(new ProvElementComparer());

		// Now the content provider.
		DeferredQueryContentProvider contentProvider = new DeferredQueryContentProvider();
		availableIUViewer.setContentProvider(contentProvider);

		// Now the presentation, columns before label provider.
		setTreeColumns(availableIUViewer.getTree());
		availableIUViewer.setLabelProvider(labelProvider);

		// Notify the filtered tree so that it can hook listeners on the
		// content provider.  This is needed so that filtering is only allowed
		// after content has been retrieved.
		filteredTree.contentProviderSet(contentProvider);

		final StructuredViewerProvisioningListener listener = new StructuredViewerProvisioningListener(getClass().getName(), availableIUViewer, ProvUIProvisioningListener.PROV_EVENT_METADATA_REPOSITORY, getProvisioningUI().getOperationRunner()) {
			protected void repositoryAdded(final RepositoryEvent event) {
				makeRepositoryVisible(event.getRepositoryLocation());
			}

			protected void refreshViewer() {
				final TreeViewer treeViewer = filteredTree.getViewer();
				final Tree tree = treeViewer.getTree();
				IWorkbench workbench = PlatformUI.getWorkbench();
				if (workbench.isClosing())
					return;
				if (tree != null && !tree.isDisposed()) {
					updateAvailableViewState();
				}

			}
		};
		ProvUI.getProvisioningEventBus(getProvisioningUI().getSession()).addListener(listener);

		availableIUViewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				ProvUI.getProvisioningEventBus(getProvisioningUI().getSession()).removeListener(listener);
			}
		});
		updateAvailableViewState();
		return availableIUViewer;
	}

	private void setTreeColumns(Tree tree) {
		tree.setHeaderVisible(true);

		IUColumnConfig[] cols = getColumnConfig();
		for (int i = 0; i < cols.length; i++) {
			TreeColumn tc = new TreeColumn(tree, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(cols[i].getColumnTitle());
			tc.setWidth(cols[i].getWidthInPixels(tree));
		}
	}

	Object getNewInput() {
		if (repositoryFilter != null) {
			return new MetadataRepositoryElement(queryContext, getProvisioningUI(), repositoryFilter, true);
		} else if (filterConstant == AVAILABLE_NONE) {
			// Dummy object that explains empty site list
			return new ProvElement(null) {
				public Object[] getChildren(Object o) {
					String description;
					String name;
					int severity;
					if (!getPolicy().getRepositoriesVisible()) {
						// shouldn't get here ideally.  No sites and no way to add any.
						severity = IStatus.ERROR;
						name = ProvUIMessages.AvailableIUGroup_NoSitesConfiguredExplanation;
						description = ProvUIMessages.AvailableIUGroup_NoSitesConfiguredDescription;
					} else {
						severity = IStatus.INFO;
						name = ProvUIMessages.AvailableIUGroup_NoSitesExplanation;
						description = ProvUIMessages.ColocatedRepositoryManipulator_NoContentExplanation;
					}
					return new Object[] {new EmptyElementExplanation(null, severity, name, description)};
				}

				public String getLabel(Object o) {
					// Label not needed for input
					return null;
				}
			};
		} else {
			queryableManager.setRespositoryFlags(repoFlags);
			return new MetadataRepositories(queryContext, getProvisioningUI(), queryableManager);
		}
	}

	/**
	 * Set a boolean indicating whether a bold font should be used when
	 * showing filtered items.  This method does not refresh the tree or 
	 * labels, so that must be done explicitly by the caller.
	 * @param useBoldFont
	 */
	public void setUseBoldFontForFilteredItems(boolean useBoldFont) {
		if (labelProvider != null)
			labelProvider.setUseBoldFontForFilteredItems(useBoldFont);
	}

	/**
	 * Return the composite that contains the controls in this group.
	 * @return the composite
	 */
	public Composite getComposite() {
		return super.getComposite();
	}

	/**
	 * Get the viewer used to represent the available IU's
	 * @return the viewer
	 */
	public StructuredViewer getStructuredViewer() {
		return super.getStructuredViewer();
	}

	/**
	 * Get the selected IU's
	 * @return the array of selected IU's
	 */
	// overridden for visibility in the public package
	public java.util.List<IInstallableUnit> getSelectedIUs() {
		return super.getSelectedIUs();
	}

	// overridden to weed out non-IU elements, such as repositories or empty explanations
	public Object[] getSelectedIUElements() {
		Object[] elements = ((IStructuredSelection) viewer.getSelection()).toArray();
		ArrayList<Object> list = new ArrayList<Object>(elements.length);
		for (int i = 0; i < elements.length; i++)
			if (ElementUtils.getIU(elements[i]) != null)
				list.add(elements[i]);
		return list.toArray();
	}

	public CheckboxTreeViewer getCheckboxTreeViewer() {
		return filteredTree.getCheckboxTreeViewer();
	}

	/**
	 * Get the selected IU's
	 * @return the array of checked IU's
	 */
	public IInstallableUnit[] getCheckedLeafIUs() {
		Object[] selections = filteredTree.getCheckedElements(); // Get all the elements that have been selected, not just the visible ones
		if (selections.length == 0)
			return new IInstallableUnit[0];
		ArrayList<IInstallableUnit> leaves = new ArrayList<IInstallableUnit>(selections.length);
		for (int i = 0; i < selections.length; i++) {
			if (!getCheckboxTreeViewer().getGrayed(selections[i])) {
				IInstallableUnit iu = ProvUI.getAdapter(selections[i], IInstallableUnit.class);
				if (iu != null && !ProvUI.isCategory(iu) && !leaves.contains(iu))
					leaves.add(iu);
			}
		}
		return leaves.toArray(new IInstallableUnit[leaves.size()]);
	}

	public Tree getTree() {
		if (viewer == null)
			return null;
		return ((TreeViewer) viewer).getTree();
	}

	/*
	 * Make the repository with the specified location visible in the viewer.
	 */
	void makeRepositoryVisible(final URI location) {
		// If we are viewing by anything other than site, there is no specific way
		// to make a repo visible. 
		if (!(queryContext.getViewType() == IUViewQueryContext.AVAILABLE_VIEW_BY_REPO)) {
			if (Display.getCurrent() == null)
				display.asyncExec(new Runnable() {
					public void run() {
						updateAvailableViewState();
					}
				});
			else
				updateAvailableViewState();
			return;
		}
		// First reset the input so that the new repo shows up
		Runnable runnable = new Runnable() {
			public void run() {
				final TreeViewer treeViewer = filteredTree.getViewer();
				final Tree tree = treeViewer.getTree();
				IWorkbench workbench = PlatformUI.getWorkbench();
				if (workbench.isClosing())
					return;
				if (tree != null && !tree.isDisposed()) {
					updateAvailableViewState();
				}
			}
		};
		if (Display.getCurrent() == null)
			display.asyncExec(runnable);
		else
			runnable.run();
		// We don't know if loading will be a fast or slow operation.
		// We do it in a job to be safe, and when it's done, we update
		// the UI.
		Job job = new Job(NLS.bind(ProvUIMessages.AvailableIUGroup_LoadingRepository, URIUtil.toUnencodedString(location))) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					getProvisioningUI().loadMetadataRepository(location, true, monitor);
					return Status.OK_STATUS;
				} catch (ProvisionException e) {
					return e.getStatus();
				} catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
			}
		};
		job.setPriority(Job.LONG);
		job.setSystem(true);
		job.setUser(false);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(final IJobChangeEvent event) {
				if (event.getResult().isOK())
					display.asyncExec(new Runnable() {
						@SuppressWarnings("rawtypes")
						public void run() {
							final TreeViewer treeViewer = filteredTree.getViewer();
							IWorkbench workbench = PlatformUI.getWorkbench();
							if (workbench.isClosing())
								return;
							// Expand only if there have been no other jobs started for other repos.
							if (event.getJob() == lastRequestedLoadJob) {
								final Tree tree = treeViewer.getTree();
								if (tree != null && !tree.isDisposed()) {
									TreeItem[] items = tree.getItems();
									for (int i = 0; i < items.length; i++) {
										if (items[i].getData() instanceof IRepositoryElement) {
											URI url = ((IRepositoryElement) items[i].getData()).getLocation();
											if (url.equals(location)) {
												treeViewer.expandToLevel(items[i].getData(), AbstractTreeViewer.ALL_LEVELS);
												tree.select(items[i]);
												return;
											}
										}
									}
								}
							}
						}
					});
			}
		});
		lastRequestedLoadJob = job;
		job.schedule();
	}

	public void updateAvailableViewState() {
		if (getTree() == null || getTree().isDisposed())
			return;
		final Composite parent = getComposite().getParent();
		setUseBoldFontForFilteredItems(queryContext.getViewType() != IUViewQueryContext.AVAILABLE_VIEW_FLAT);

		BusyIndicator.showWhile(display, new Runnable() {
			public void run() {
				parent.setRedraw(false);
				getCheckboxTreeViewer().setInput(getNewInput());
				parent.layout(true);
				parent.setRedraw(true);
			}
		});
	}

	public Control getDefaultFocusControl() {
		if (filteredTree != null)
			return filteredTree.getFilterControl();
		return null;
	}

	protected String getFilterString() {
		return filteredTree.getFilterString();
	}

	protected GridData getViewerGridData() {
		GridData data = super.getViewerGridData();
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		return data;
	}

	/**
	 * Set the checked elements to the specified selections.  This method
	 * does not force visibility/expansion of the checked elements.  If they are not
	 * visible, they will not be checked.
	 * @param selections
	 */
	public void setChecked(Object[] selections) {
		filteredTree.getCheckboxTreeViewer().setCheckedElements(selections);
		// TODO HACK ALERT!
		// Since We don't have API for setAllChecked(boolean), clients have to use this method.
		// We need to signal DelayedFilterCheckboxTree when everything needs to be deselected since
		// we aren't firing an event for each item.
		Object element = selections.length == 0 ? DelayedFilterCheckboxTree.ALL_ITEMS_HACK : selections[0];
		filteredTree.getCheckboxTreeViewer().fireCheckStateChanged(element, selections.length > 0);
	}

	public void setRepositoryFilter(int filterFlag, URI repoLocation) {
		// If there has been no change, don't do anything.  We will be
		// clearing out selection caches in this method and should not do
		// so if there's really no change.
		if (filterConstant == filterFlag) {
			if (filterConstant != AVAILABLE_SPECIFIED)
				return;
			if (repoLocation != null && repoLocation.equals(repositoryFilter))
				return;
		}
		filterConstant = filterFlag;

		switch (filterFlag) {
			case AVAILABLE_ALL :
			case AVAILABLE_NONE :
				repositoryFilter = null;
				repoFlags &= ~IRepositoryManager.REPOSITORIES_LOCAL;
				break;
			case AVAILABLE_LOCAL :
				repositoryFilter = null;
				repoFlags |= IRepositoryManager.REPOSITORIES_LOCAL;
				break;
			default :
				repositoryFilter = repoLocation;
				break;
		}
		updateAvailableViewState();
		filteredTree.clearCheckStateCache();
	}

	private IPreFilterJobProvider getPreFilterJobProvider() {
		return new IPreFilterJobProvider() {

			public Job getPreFilterJob() {
				switch (filterConstant) {
					case AVAILABLE_ALL :
						Job preFilterJob = new LoadMetadataRepositoryJob(getProvisioningUI());
						preFilterJob.setProperty(LoadMetadataRepositoryJob.SUPPRESS_REPOSITORY_EVENTS, Boolean.toString(true));
						return preFilterJob;
					case AVAILABLE_NONE :
					case AVAILABLE_LOCAL :
						return null;
					default :
						if (repositoryFilter == null)
							return null;
						Job job = new Job("Repository Load Job") { //$NON-NLS-1$
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								try {
									getProvisioningUI().loadMetadataRepository(repositoryFilter, false, monitor);
									return Status.OK_STATUS;
								} catch (ProvisionException e) {
									return e.getStatus();
								}
							}

						};
						job.setPriority(Job.SHORT);
						return job;
				}
			}

		};
	}
}
