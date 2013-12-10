/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.discovery.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.compatibility.SiteVerifier;
import org.eclipse.equinox.internal.p2.discovery.model.*;
import org.eclipse.equinox.internal.p2.discovery.util.CatalogCategoryComparator;
import org.eclipse.equinox.internal.p2.discovery.util.CatalogItemComparator;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryUi;
import org.eclipse.equinox.internal.p2.ui.discovery.util.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * The main wizard page that allows users to select connectors that they wish to install.
 * 
 * @author David Green
 * @author Steffen Pingel
 */
public class CatalogViewer extends FilteredViewer {

	protected static class CatalogContentProvider implements ITreeContentProvider {

		private Catalog catalog;

		private boolean hasCategories;

		public boolean hasCategories() {
			return hasCategories;
		}

		public void setHasCategories(boolean hasCategories) {
			this.hasCategories = hasCategories;
		}

		public void dispose() {
			catalog = null;
		}

		public Catalog getCatalog() {
			return catalog;
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof CatalogCategory) {
				return ((CatalogCategory) parentElement).getItems().toArray();
			}
			return null;
		}

		public Object[] getElements(Object inputElement) {
			if (catalog != null) {
				List<Object> elements = new ArrayList<Object>();
				if (hasCategories()) {
					elements.addAll(catalog.getCategories());
				}
				elements.addAll(catalog.getItems());
				return elements.toArray(new Object[0]);
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (element instanceof CatalogCategory) {
				return catalog;
			}
			if (element instanceof CatalogItem) {
				return ((CatalogItem) element).getCategory();
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof CatalogCategory) {
				return ((CatalogCategory) element).getItems().size() > 0;
			}
			return false;
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.catalog = (Catalog) newInput;
		}

	}

	private class Filter extends ViewerFilter {

		public Filter() {
			// constructor
		}

		@Override
		public boolean select(Viewer filteredViewer, Object parentElement, Object element) {
			if (element instanceof CatalogItem) {
				return doFilter((CatalogItem) element);
			} else if (element instanceof CatalogCategory) {
				// only show categories if at least one child is visible
				CatalogCategory category = (CatalogCategory) element;
				for (CatalogItem item : category.getItems()) {
					if (doFilter(item)) {
						return true;
					}
				}
				return false;
			}
			return true;
		}

	}

	private class FindFilter extends PatternFilter {

		public FindFilter() {
			// constructor
		}

		private boolean filterMatches(String text) {
			return text != null && wordMatches(text);
		}

		@Override
		protected Object[] getChildren(Object element) {
			if (element instanceof CatalogCategory) {
				return ((CatalogCategory) element).getItems().toArray();
			}
			return super.getChildren(element);
		}

		@Override
		protected boolean isLeafMatch(Viewer filteredViewer, Object element) {
			if (element instanceof CatalogItem) {
				CatalogItem descriptor = (CatalogItem) element;
				if (!(filterMatches(descriptor.getName()) || filterMatches(descriptor.getDescription()) || filterMatches(descriptor.getProvider()) || filterMatches(descriptor.getLicense()))) {
					return false;
				}
				return true;
			}
			return false;
		}

	}

	//	private class ConnectorBorderPaintListener implements PaintListener {
	//		public void paintControl(PaintEvent e) {
	//			Composite composite = (Composite) e.widget;
	//			Rectangle bounds = composite.getBounds();
	//			GC gc = e.gc;
	//			gc.setLineStyle(SWT.LINE_DOT);
	//			gc.drawLine(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y);
	//		}
	//	}

	private static final int DEFAULT_HEIGHT = 250;

	final Catalog catalog;

	private final List<CatalogItem> checkedItems = new ArrayList<CatalogItem>();

	private boolean complete;

	private final CatalogConfiguration configuration;

	protected final IRunnableContext context;

	boolean ignoreUpdates;

	Set<String> installedFeatures;

	DiscoveryResources resources;

	private final SelectionProviderAdapter selectionProvider;

	protected final IShellProvider shellProvider;

	boolean showInstalled;

	Button showInstalledCheckbox;

	Set<Tag> visibleTags;

	private boolean showCategories;

	private CatalogContentProvider contentProvider;

	public CatalogViewer(Catalog catalog, IShellProvider shellProvider, IRunnableContext context, CatalogConfiguration configuration) {
		Assert.isNotNull(catalog);
		Assert.isNotNull(shellProvider);
		Assert.isNotNull(context);
		Assert.isNotNull(configuration);
		this.catalog = catalog;
		this.shellProvider = shellProvider;
		this.context = context;
		this.configuration = configuration;
		this.selectionProvider = new SelectionProviderAdapter();
		this.showInstalled = configuration.isShowInstalled();
		this.showCategories = configuration.isShowCategories();
		if (configuration.getSelectedTags() != null) {
			this.visibleTags = new HashSet<Tag>(configuration.getSelectedTags());
		} else {
			this.visibleTags = new HashSet<Tag>();
		}
		setMinimumHeight(DEFAULT_HEIGHT);
		setComplete(false);
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionProvider.addSelectionChangedListener(listener);
	}

	protected void catalogUpdated(boolean wasCancelled, boolean wasError) {
		if (catalog != null && !wasCancelled && !wasError) {
			doCheckCatalog();
		}
		viewer.setInput(catalog);
		selectionProvider.setSelection(StructuredSelection.EMPTY);
	}

	protected void doCheckCatalog() {
		int categoryWithConnectorCount = 0;
		for (CatalogCategory category : catalog.getCategories()) {
			categoryWithConnectorCount += category.getItems().size();
		}
		if (categoryWithConnectorCount == 0) {
			// nothing was discovered: notify the user
			MessageDialog.openWarning(getShell(), Messages.ConnectorDiscoveryWizardMainPage_noConnectorsFound, Messages.ConnectorDiscoveryWizardMainPage_noConnectorsFound_description);
		}
	}

	protected IStatus computeStatus(InvocationTargetException e, String message) {
		Throwable cause = e.getCause();
		if (cause.getMessage() != null) {
			message = NLS.bind(Messages.ConnectorDiscoveryWizardMainPage_message_with_cause, message, cause.getMessage());
		}
		return new Status(IStatus.ERROR, DiscoveryUi.ID_PLUGIN, message, e);
	}

	protected Pattern createPattern(String filterText) {
		if (filterText == null || filterText.length() == 0) {
			return null;
		}
		String regex = filterText;
		regex.replace("\\", "\\\\").replace("?", ".").replace("*", ".*?"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	}

	@Override
	protected PatternFilter doCreateFilter() {
		return new FindFilter();
	}

	@Override
	protected void doCreateHeaderControls(Composite parent) {
		if (configuration.isShowInstalledFilter()) {
			showInstalledCheckbox = new Button(parent, SWT.CHECK);
			showInstalledCheckbox.setSelection(showInstalled);
			showInstalledCheckbox.setText(Messages.DiscoveryViewer_Show_Installed);
			showInstalledCheckbox.addSelectionListener(new SelectionListener() {

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}

				public void widgetSelected(SelectionEvent e) {
					if (ignoreUpdates) {
						return;
					}

					ignoreUpdates = true;
					setShowInstalled(showInstalledCheckbox.getSelection());
					ignoreUpdates = false;
				}
			});
		}
		if (configuration.isShowTagFilter()) {
			for (final Tag tag : catalog.getTags()) {
				final Button checkbox = new Button(parent, SWT.CHECK);
				checkbox.setSelection(visibleTags.contains(tag));
				checkbox.setText(tag.getLabel());
				checkbox.addSelectionListener(new SelectionListener() {
					public void widgetDefaultSelected(SelectionEvent e) {
						widgetSelected(e);
					}

					public void widgetSelected(SelectionEvent e) {
						boolean selection = checkbox.getSelection();
						if (selection) {
							visibleTags.add(tag);
						} else {
							visibleTags.remove(tag);
						}
						refresh();
					}
				});
			}
		}
	}

	@Override
	protected StructuredViewer doCreateViewer(Composite container) {

		StructuredViewer _viewer = new ControlListViewer(container, SWT.BORDER) {
			@Override
			protected ControlListItem<?> doCreateItem(Composite parent, Object element) {
				return doCreateViewerItem(parent, element);
			}
		};
		contentProvider = doCreateContentProvider();
		contentProvider.setHasCategories(isShowCategories());
		_viewer.setContentProvider(contentProvider);
		_viewer.setSorter(new ViewerSorter() {
			CatalogCategoryComparator categoryComparator = new CatalogCategoryComparator();

			CatalogItemComparator itemComparator = new CatalogItemComparator();

			@Override
			public int compare(Viewer v, Object o1, Object o2) {
				CatalogCategory cat1 = getCategory(o1);
				CatalogCategory cat2 = getCategory(o2);

				// FIXME filter uncategorized items?
				if (cat1 == null) {
					return (cat2 != null) ? 1 : 0;
				} else if (cat2 == null) {
					return 1;
				}

				int i = categoryComparator.compare(cat1, cat2);
				if (i == 0) {
					if (o1 instanceof CatalogCategory) {
						return -1;
					}
					if (o2 instanceof CatalogCategory) {
						return 1;
					}
					if (cat1 == cat2 && o1 instanceof CatalogItem && o2 instanceof CatalogItem) {
						return itemComparator.compare((CatalogItem) o1, (CatalogItem) o2);
					}
					return super.compare(v, o1, o2);
				}
				return i;
			}

			//					private int compare(Comparator<Object> comparator, Object key1, Object key2) {
			//						if (key1 == null) {
			//							return (key2 != null) ? 1 : 0;
			//						} else if (key2 == null) {
			//							return -1;
			//						}
			//						return comparator.compare(key1, key2);
			//					}
			private CatalogCategory getCategory(Object o) {
				if (o instanceof CatalogCategory) {
					return (CatalogCategory) o;
				}
				if (o instanceof CatalogItem) {
					return ((CatalogItem) o).getCategory();
				}
				return null;
			}
		});

		resources = new DiscoveryResources(container.getDisplay());
		_viewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				resources.dispose();
				if (catalog != null)
					catalog.dispose();
			}
		});
		_viewer.addFilter(new Filter());
		return _viewer;
	}

	protected CatalogContentProvider doCreateContentProvider() {
		return new CatalogContentProvider();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected ControlListItem<?> doCreateViewerItem(Composite parent, Object element) {
		if (element instanceof CatalogItem) {
			return new DiscoveryItem(parent, SWT.NONE, resources, shellProvider, (CatalogItem) element, this);
		} else if (element instanceof CatalogCategory) {
			return new CategoryItem(parent, SWT.NONE, resources, (CatalogCategory) element);
		}
		return null;
	}

	protected boolean doFilter(CatalogItem item) {
		if (!showInstalled && item.isInstalled()) {
			return false;
		}

		if (!isTagVisible(item)) {
			return false;
		}

		for (CatalogFilter filter : configuration.getFilters()) {
			if (!filter.select(item)) {
				return false;
			}
		}

		return true;
	}

	public Catalog getCatalog() {
		return catalog;
	}

	public List<CatalogItem> getCheckedItems() {
		return new ArrayList<CatalogItem>(checkedItems);
	}

	public CatalogConfiguration getConfiguration() {
		return configuration;
	}

	protected Set<String> getInstalledFeatures(IProgressMonitor monitor) {
		Set<String> features = new HashSet<String>();
		IProfile profile = ProvUI.getProfileRegistry(ProvisioningUI.getDefaultUI().getSession()).getProfile(ProvisioningUI.getDefaultUI().getProfileId());
		if (profile != null) {
			IQueryResult<IInstallableUnit> result = profile.available(QueryUtil.createIUGroupQuery(), monitor);
			for (Iterator<IInstallableUnit> it = result.iterator(); it.hasNext();) {
				IInstallableUnit unit = it.next();
				features.add(unit.getId());
			}
		}
		return features;
	}

	protected DiscoveryResources getResources() {
		return resources;
	}

	public IStructuredSelection getSelection() {
		return (IStructuredSelection) selectionProvider.getSelection();
	}

	private Shell getShell() {
		return shellProvider.getShell();
	}

	public boolean isComplete() {
		return complete;
	}

	public boolean isShowCategories() {
		return showCategories;
	}

	public boolean isShowInstalled() {
		return showInstalled;
	}

	private boolean isTagVisible(CatalogItem item) {
		if (!configuration.isShowTagFilter()) {
			return true;
		}
		for (Tag selectedTag : visibleTags) {
			for (Tag tag : item.getTags()) {
				if (tag.equals(selectedTag)) {
					return true;
				}
			}
		}
		return false;
	}

	protected void modifySelection(final CatalogItem connector, boolean selected) {
		modifySelectionInternal(connector, selected);
		updateState();
	}

	private void modifySelectionInternal(final CatalogItem connector, boolean selected) {
		connector.setSelected(selected);
		if (selected) {
			checkedItems.add(connector);
		} else {
			checkedItems.remove(connector);
		}
	}

	protected void postDiscovery() {
		for (CatalogItem connector : catalog.getItems()) {
			connector.setInstalled(installedFeatures != null && installedFeatures.containsAll(connector.getInstallableUnits()));
		}
	}

	public void refresh() {
		if (viewer != null && !viewer.getControl().isDisposed()) {
			viewer.refresh();
		}
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionProvider.removeSelectionChangedListener(listener);
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public void setSelection(IStructuredSelection selection) {
		Set<CatalogItem> selected = new HashSet<CatalogItem>();
		for (Object descriptor : selection.toArray()) {
			if (descriptor instanceof CatalogItem) {
				selected.add((CatalogItem) descriptor);
			}
		}
		for (CatalogItem connector : catalog.getItems()) {
			modifySelectionInternal(connector, selected.contains(connector));
		}
		updateState();
	}

	public void setShowInstalled(boolean showInstalled) {
		this.showInstalled = showInstalled;
		showInstalledCheckbox.setSelection(showInstalled);
		refresh();
	}

	public void setShowCategories(boolean showCategories) {
		this.showCategories = showCategories;
		if (contentProvider != null) {
			contentProvider.setHasCategories(showCategories);
			refresh();
		}
	}

	public void updateCatalog() {
		boolean wasCancelled = false;
		boolean wasError = false;
		try {
			final IStatus[] result = new IStatus[1];
			context.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InterruptedException {
					if (installedFeatures == null) {
						installedFeatures = getInstalledFeatures(monitor);
					}

					result[0] = catalog.performDiscovery(monitor);
					if (monitor.isCanceled()) {
						throw new InterruptedException();
					}

					postDiscovery();
				}
			});

			if (result[0] != null && !result[0].isOK()) {
				StatusManager.getManager().handle(result[0], StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
				wasError = true;
			}
		} catch (InvocationTargetException e) {
			IStatus status = computeStatus(e, Messages.ConnectorDiscoveryWizardMainPage_unexpectedException);
			StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			wasError = true;
		} catch (InterruptedException e) {
			// cancelled by user so nothing to do here.
			wasCancelled = true;
		}
		if (catalog != null) {
			catalogUpdated(wasCancelled, wasError);
			verifyUpdateSiteAvailability();
		}
		// help UI tests
		viewer.setData("discoveryComplete", "true"); //$NON-NLS-1$//$NON-NLS-2$
	}

	protected void verifyUpdateSiteAvailability() {
		if (configuration.isVerifyUpdateSiteAvailability() && !catalog.getItems().isEmpty()) {
			try {
				context.run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						SiteVerifier verifier = new SiteVerifier(catalog);
						verifier.verifySiteAvailability(monitor);
					}
				});
			} catch (InvocationTargetException e) {
				IStatus status = computeStatus(e, Messages.ConnectorDiscoveryWizardMainPage_unexpectedException);
				StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.BLOCK | StatusManager.LOG);
			} catch (InterruptedException e) {
				// cancelled by user so nothing to do here.
			}
		}
	}

	private void updateState() {
		setComplete(!checkedItems.isEmpty());
		selectionProvider.setSelection(new StructuredSelection(getCheckedItems()));
	}

}
