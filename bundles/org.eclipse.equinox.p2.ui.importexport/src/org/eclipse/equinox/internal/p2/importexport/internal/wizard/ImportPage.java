/*******************************************************************************
 * Copyright (c) 2011, 2023 WindRiver Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *     IBM Corporation - Ongoing development
 *     Ericsson AB (Pascal Rapicault) - Bug 387115 - Allow to export everything
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal.wizard;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.importexport.IUDetail;
import org.eclipse.equinox.internal.p2.importexport.VersionIncompatibleException;
import org.eclipse.equinox.internal.p2.importexport.internal.Messages;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard;
import org.eclipse.equinox.internal.p2.ui.viewers.IUColumnConfig;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PatternFilter;

public class ImportPage extends AbstractImportPage implements ISelectableIUsPage {

	class InstallationContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
			//
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return (Object[]) inputElement;
		}

		@Override
		public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
			//
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}

	}

	class InstallationLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			IInstallableUnit iu = ((IUDetail) element).getIU();
			switch (columnIndex) {
			case 0:
				return getIUNameWithDetail(iu);
			case 1:
				return iu.getVersion().toString();
			case 2:
				return iu.getId();
			default:
				throw new RuntimeException("Should not happen"); //$NON-NLS-1$
			}

		}

		@Override
		public Color getForeground(Object element) {
			if (hasInstalled(ProvUI.getAdapter(element, IInstallableUnit.class)))
				return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}
	}

	class P2ImportIUPatternFilter extends PatternFilter {

		boolean checkName, checkVersion, checkId = false;
		String patternString;

		/**
		 * Create a new instance of a AvailableIUPatternFilter
		 */
		public P2ImportIUPatternFilter(IUColumnConfig[] columnConfig) {
			super();
			for (IUColumnConfig columnConfig1 : columnConfig) {
				int field = columnConfig1.getColumnType();
				if (field == IUColumnConfig.COLUMN_ID)
					checkId = true;
				else if (field == IUColumnConfig.COLUMN_NAME)
					checkName = true;
				else if (field == IUColumnConfig.COLUMN_VERSION)
					checkVersion = true;
			}

		}

		@Override
		public boolean isElementSelectable(Object element) {
			return element instanceof IUDetail;
		}

		/*
		 * Overridden to remember the pattern string for an optimization in
		 * isParentMatch
		 */
		@Override
		public void setPattern(String patternString) {
			super.setPattern(patternString);
			this.patternString = patternString;
		}

		/*
		 * Overridden to avoid getting children unless there is actually a filter.
		 */
		@Override
		protected boolean isParentMatch(Viewer viewer1, Object element) {
			if (patternString == null || patternString.length() == 0)
				return true;
			return super.isParentMatch(viewer1, element);
		}

		@Override
		protected boolean isLeafMatch(Viewer viewer1, Object element) {
			String text = null;
			if (element instanceof IUDetail) {
				IInstallableUnit iu = ((IUDetail) element).getIU();
				if (checkName) {
					// Get the iu name in the default locale
					text = iu.getProperty(IInstallableUnit.PROP_NAME, null);
					if (text != null && wordMatches(text))
						return true;
				}
				if (checkId || (checkName && text == null)) {
					text = iu.getId();
					if (wordMatches(text)) {
						return true;
					}
				}
				if (checkVersion) {
					text = iu.getVersion().toString();
					if (wordMatches(text))
						return true;
				}
			}
			return false;
		}

	}

	private List<IUDetail> features;
	private final List<URI> loadRepos = new ArrayList<>();
	private final Map<IUDetail, List<IUDetail>> newProposedFeature = new HashMap<>();
	private Button contactAll;
	private Button installLatest;
	private String oldDestination;

	public ImportPage(ProvisioningUI ui, ProvisioningOperationWizard wizard) {
		super("importpage", ui, wizard); //$NON-NLS-1$
		setTitle(Messages.ImportPage_TITLE);
		setDescription(Messages.ImportPage_DESCRIPTION);
	}

	@Override
	protected void createContents(Composite composite) {
		createDestinationGroup(composite, false);
		createInstallationTable(composite);
		createAdditionOptions(composite);
	}

	private void createAdditionOptions(Composite parent) {
		Composite composite = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		contactAll = new Button(composite, SWT.CHECK);
		contactAll.setText(ProvUIMessages.AvailableIUsPage_ResolveAllCheckbox);

		installLatest = new Button(composite, SWT.CHECK);
		installLatest.setText(Messages.ImportPage_InstallLatestVersion);
		installLatest.setSelection(true);
	}

	@Override
	protected ITreeContentProvider getContentProvider() {
		return new InstallationContentProvider();
	}

	@Override
	protected ITableLabelProvider getLabelProvider() {
		return new InstallationLabelProvider();
	}

	@Override
	protected int getBrowseDialogStyle() {
		return SWT.OPEN;
	}

	@Override
	protected String getDestinationLabel() {
		return Messages.ImportPage_DESTINATION_LABEL;
	}

	@Override
	protected String getDialogTitle() {
		return Messages.ImportPage_FILEDIALOG_TITLE;
	}

	@Override
	protected IUDetail[] getInput() {
		return new IUDetail[0];
	}

	@Override
	protected String getInvalidDestinationMessage() {
		return Messages.ImportPage_DEST_ERROR;
	}

	@Override
	protected void giveFocusToDestination() {
		destinationNameField.setFocus();
	}

	@Override
	protected void updatePageCompletion() {
		super.updatePageCompletion();
		if (isPageComplete())
			getProvisioningWizard().operationSelectionsChanged(this);
	}

	@Override
	protected void handleDestinationChanged(String newDestination) {
		if (validateDestinationGroup()) {
			// p2f file is changed, update the cached data
			if (!newDestination.equals(oldDestination)) {
				loadRepos.clear();
				newProposedFeature.clear();
			}
			InputStream input = null;
			try {
				input = new BufferedInputStream(new FileInputStream(getDestinationValue()));
				features = importexportService.importP2F(input);
				contactAll.setSelection(hasEntriesWithoutRepo());
				viewer.setInput(features.toArray(new IUDetail[features.size()]));
				input.close();
			} catch (VersionIncompatibleException e) {
				MessageDialog.openWarning(getShell(), Messages.ImportPage_TITLE, e.getMessage());
			} catch (FileNotFoundException e) {
				MessageDialog.openError(getShell(), Messages.ImportPage_TITLE, Messages.ImportPage_FILENOTFOUND);
			} catch (IOException e) {
				MessageDialog.openError(getShell(), Messages.ImportPage_TITLE, e.getLocalizedMessage());
			}
		} else
			viewer.setInput(null);
		updatePageCompletion();
	}

	private boolean hasEntriesWithoutRepo() {
		for (IUDetail entry : features) {
			if (entry.getReferencedRepositories().size() == 0)
				return true;
		}
		return false;
	}

	@Override
	protected void setDestinationValue(String selectedFileName) {
		oldDestination = getDestinationValue();
		super.setDestinationValue(selectedFileName);
	}

	@Override
	protected boolean validDestination() {
		File target = new File(getDestinationValue());
		return super.validDestination() && target.exists() && target.canRead();
	}

	@Override
	public Object[] getCheckedIUElements() {
		Object[] checked = viewer.getCheckedElements();
		List<IUDetail> checkedFeatures = new ArrayList<>(checked.length);
		boolean useLatest = installLatest.getSelection();
		for (Object checked1 : checked) {
			IUDetail feature = (IUDetail) checked1;
			List<IUDetail> existingFeatures = newProposedFeature.get(feature);
			if (existingFeatures == null) {
				checkedFeatures.add(feature);
			} else {
				IUDetail matchPolicy = null;
				for (IUDetail f : existingFeatures) {
					if (matchPolicy == null) {
						matchPolicy = f;
					} else {
						if (matchPolicy.getIU().getVersion().compareTo(f.getIU().getVersion()) < 0) {
							if (useLatest) {
								matchPolicy = f;
							}
						} else {
							if (!useLatest) {
								matchPolicy = f;
							}
						}
					}
				}
				if (matchPolicy != null) {
					checkedFeatures.add(matchPolicy);
				}
			}
		}
		return checkedFeatures.toArray(new IUDetail[checkedFeatures.size()]);
	}

	@Override
	public Object[] getSelectedIUElements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCheckedElements(Object[] elements) {
		throw new UnsupportedOperationException();
	}

	public ProvisioningContext getProvisioningContext() {
		if (agent != null) {
			Object[] checked = viewer.getCheckedElements();
			List<URI> referredRepos = new ArrayList<>(checked.length);
			for (Object checkItem : checked) {
				IUDetail feature = (IUDetail) checkItem;
				for (URI uri : feature.getReferencedRepositories()) {
					referredRepos.add(uri);
				}
			}
			ProvisioningContext context = new ProvisioningContext(agent);
			if (!contactAll.getSelection()) {
				context.setArtifactRepositories(referredRepos.toArray(new URI[referredRepos.size()]));
				context.setMetadataRepositories(referredRepos.toArray(new URI[referredRepos.size()]));
			}
			return context;
		}
		return null;
	}

	public boolean hasUnloadedRepo() {
		for (Object checked : viewer.getCheckedElements()) {
			IUDetail feature = (IUDetail) checked;
			for (URI uri : feature.getReferencedRepositories())
				if (!loadRepos.contains(uri))
					return true;
		}
		return false;
	}

	class GetCheckedElement implements Runnable {
		Object[] checkedElements = null;

		@Override
		public void run() {
			checkedElements = viewer.getCheckedElements();
		}
	}

	public Object[] getChecked() {
		if (Display.findDisplay(Thread.currentThread()) != null)
			return viewer.getCheckedElements();
		GetCheckedElement get = new GetCheckedElement();
		Display.getDefault().syncExec(get);
		return get.checkedElements;
	}

	public void recompute(IProgressMonitor monitor) throws InterruptedException {
		SubMonitor sub = SubMonitor.convert(monitor, Messages.ImportPage_QueryFeaturesJob, 1000);
		if (agent != null) {
			IMetadataRepositoryManager metaManager = agent.getService(IMetadataRepositoryManager.class);
			IArtifactRepositoryManager artifactManager = agent.getService(IArtifactRepositoryManager.class);
			Object[] checked = getChecked();
			sub.setWorkRemaining(100 * checked.length);
			for (Object item : checked) {
				IUDetail feature = (IUDetail) item;
				if (!newProposedFeature.containsKey(feature)) {
					if (sub.isCanceled())
						throw new InterruptedException();
					SubMonitor sub2 = sub.newChild(100, SubMonitor.SUPPRESS_ALL_LABELS);
					sub2.setWorkRemaining(feature.getReferencedRepositories().size() * 500 + 100);
					List<IRepository<IInstallableUnit>> repos = new ArrayList<>();
					for (URI uri : feature.getReferencedRepositories()) {
						if (!metaManager.contains(uri)) {
							metaManager.addRepository(uri);
						}
						metaManager.setEnabled(uri, true);
						try {
							repos.add(metaManager.loadRepository(uri, sub2.newChild(500)));
						} catch (ProvisionException e) {
							e.printStackTrace();
						} catch (OperationCanceledException e) {
							throw new InterruptedException(e.getLocalizedMessage());
						}
						if (!artifactManager.contains(uri)) {
							artifactManager.addRepository(uri);
						}
						artifactManager.setEnabled(uri, true);
					}
					if (sub2.isCanceled()) {
						throw new InterruptedException();
					}
					VersionRange allNeverVersions = new VersionRange(feature.getIU().getVersion(), true, null, false);
					IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(feature.getIU().getId(), allNeverVersions);
					List<IUDetail> existingFeatures = new CompoundQueryable<>(repos).query(query, sub2.newChild(100))
							.stream().map(iu -> new IUDetail(iu, feature.getReferencedRepositories())).toList();
					newProposedFeature.put(feature, existingFeatures);
				} else {
					if (sub.isCanceled())
						throw new InterruptedException();
					sub.worked(100);
				}
			}
		}
	}

	@Override
	protected PatternFilter getPatternFilter() {
		return new P2ImportIUPatternFilter(getColumnConfig());
	}
}