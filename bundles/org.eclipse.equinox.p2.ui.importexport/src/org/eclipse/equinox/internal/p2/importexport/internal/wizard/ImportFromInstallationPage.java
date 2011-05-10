/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal.wizard;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.concurrent.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.extensionlocation.ExtensionLocationArtifactRepositoryFactory;
import org.eclipse.equinox.internal.p2.extensionlocation.ExtensionLocationMetadataRepositoryFactory;
import org.eclipse.equinox.internal.p2.importexport.internal.Constants;
import org.eclipse.equinox.internal.p2.importexport.internal.Messages;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.ProvisioningOperationWizard;
import org.eclipse.equinox.internal.p2.ui.model.ProfileElement;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.*;
import org.osgi.util.tracker.ServiceTracker;

public class ImportFromInstallationPage extends AbstractImportPage implements ISelectableIUsPage {

	protected IProvisioningAgent otherInstanceAgent = null;
	private File instancePath = null;
	private URI[] metaURIs = null;
	private URI[] artiURIs = null;
	private IProvisioningAgentProvider agentProvider;

	public ImportFromInstallationPage(ProvisioningUI ui, ProvisioningOperationWizard wizard) {
		super("importfrominstancepage", ui, wizard); //$NON-NLS-1$
		setTitle(Messages.ImportFromInstallationPage_DIALOG_TITLE);
		setDescription(Messages.ImportFromInstallationPage_DIALOG_DESCRIPTION);
	}

	@Override
	protected void createContents(Composite composite) {
		createDestinationGroup(composite);
		createInstallationTable(composite);
	}

	@Override
	protected String getDestinationLabel() {
		return Messages.ImportFromInstallationPage_DESTINATION_LABEL;
	}

	@Override
	protected String getDialogTitle() {
		return Messages.ImportFromInstallationPage_DIALOG_TITLE;
	}

	@Override
	protected Object getInput() {
		return new IInstallableUnit[0];
	}

	@Override
	protected String getInvalidDestinationMessage() {
		return Messages.ImportFromInstallationPage_INVALID_DESTINATION;
	}

	@Override
	protected String getNoOptionsMessage() {
		return Messages.ImportFromInstallationPage_SELECT_COMPONENT;
	}

	@Override
	protected boolean validateDestinationGroup() {
		return validateDestinationGroup(new NullProgressMonitor());
	}

	private IProvisioningAgentProvider getAgentProvider() {
		if (agentProvider == null) {
			ServiceTracker<IProvisioningAgentProvider, IProvisioningAgentProvider> tracker = new ServiceTracker<IProvisioningAgentProvider, IProvisioningAgentProvider>(Platform.getBundle(Constants.Bundle_ID).getBundleContext(), IProvisioningAgentProvider.class, null);
			tracker.open();
			agentProvider = tracker.getService();
			tracker.close();
		}
		return agentProvider;
	}

	boolean validateDestinationGroup(IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, 100);

		boolean rt;
		if (Display.findDisplay(Thread.currentThread()) == null) {
			Callable<Boolean> getSuperValidateDest = new Callable<Boolean>() {
				Boolean validated;

				public Boolean call() throws Exception {
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							validated = ImportFromInstallationPage.super.validateDestinationGroup();
						}
					});
					return validated;
				}
			};
			ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
			Future<Boolean> getSuperDestTask = executor.submit(getSuperValidateDest);

			try {
				rt = getSuperDestTask.get().booleanValue();
			} catch (Exception e) {
				return false;
			} finally {
				executor.shutdown();
			}
		} else
			rt = super.validateDestinationGroup();

		if (rt) {
			try {
				String destinate;
				if (Display.findDisplay(Thread.currentThread()) == null) {
					Callable<String> getDestinationValue = new Callable<String>() {
						String destination;

						public String call() throws Exception {
							if (Display.findDisplay(Thread.currentThread()) == null) {
								Display.getDefault().syncExec(new Runnable() {
									public void run() {
										destination = getDestinationValue();
									}
								});
							} else
								destination = getDestinationValue();
							return destination;
						}
					};
					ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
					Future<String> getDestTask = executor.submit(getDestinationValue);
					try {
						destinate = getDestTask.get();
					} finally {
						executor.shutdown();
					}
				} else
					destinate = getDestinationValue();
				try {
					File p2 = new File(destinate, "p2"); //$NON-NLS-1$
					if (p2.exists()) {
						boolean createAgent = true;
						if (otherInstanceAgent != null) {
							// don't create agent again if the selection is not changed
							if (!p2.equals(instancePath)) {
								otherInstanceAgent.stop();
								otherInstanceAgent = null;
								// update cached specified path by users
								instancePath = p2;
								cleanLocalRepository();
							} else
								createAgent = false;
						}
						if (createAgent)
							otherInstanceAgent = getAgentProvider().createAgent(p2.toURI());
						ArtifactRepositoryFactory factory = new ExtensionLocationArtifactRepositoryFactory();
						factory.setAgent(agent);
						IArtifactRepository artiRepo = factory.load(new File(destinate).toURI(), 0, progress.newChild(50));
						artiURIs = new URI[] {artiRepo.getLocation()};
						MetadataRepositoryFactory metaFatory = new ExtensionLocationMetadataRepositoryFactory();
						metaFatory.setAgent(agent);
						IMetadataRepository metaRepo = metaFatory.load(new File(destinate).toURI(), 0, progress.newChild(50));
						metaURIs = new URI[] {metaRepo.getLocation()};

					} else
						throw new FileNotFoundException();
				} catch (ProvisionException e) {
					if (otherInstanceAgent != null) {
						IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
						IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
						IProfile profile = ((IProfileRegistry) otherInstanceAgent.getService(IProfileRegistry.SERVICE_NAME)).getProfiles()[0];
						IAgentLocation location = (IAgentLocation) otherInstanceAgent.getService(IAgentLocation.SERVICE_NAME);
						URI engineDataArea = location.getDataArea("org.eclipse.equinox.p2.engine"); //$NON-NLS-1$
						progress.setWorkRemaining(50);
						IMetadataRepository metaRepo = manager.loadRepository(engineDataArea.resolve("profileRegistry/" + profile.getProfileId() + ".profile"), progress.newChild(25)); //$NON-NLS-1$//$NON-NLS-2$
						metaURIs = new URI[] {metaRepo.getLocation()};
						IArtifactRepository artiRepo = artifactManager.loadRepository(new File(destinate).toURI(), progress.newChild(25));
						artiURIs = new URI[] {artiRepo.getLocation()};
					} else
						throw new Exception();
				}
			} catch (Exception e) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						setErrorMessage(getInvalidDestinationMessage());
					}
				});
				rt = false;
				if (otherInstanceAgent != null)
					otherInstanceAgent.stop();
				otherInstanceAgent = null;
				cleanLocalRepository();
			} finally {
				monitor.done();
			}
		}
		return rt;
	}

	@Override
	protected void giveFocusToDestination() {
		destinationBrowseButton.setFocus();
	}

	@Override
	protected void handleDestinationBrowseButtonPressed() {
		DirectoryDialog dialog = new DirectoryDialog(getContainer().getShell());
		dialog.setText(getDialogTitle());
		dialog.setFilterPath(getDestinationValue());
		final String selectedFileName = dialog.open();

		if (selectedFileName != null) {
			setDestinationValue(selectedFileName);
		}
	}

	@Override
	protected void handleDestinationChanged(String newDestination) {
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) {
					Object input = null;
					if (validateDestinationGroup(monitor)) {
						IProfileRegistry registry = (IProfileRegistry) otherInstanceAgent.getService(IProfileRegistry.SERVICE_NAME);
						final IProfile currentProfile = registry.getProfiles()[0];
						final ProfileElement element = new ProfileElement(null, currentProfile.getProfileId()) {
							@Override
							public org.eclipse.equinox.p2.query.IQueryable<?> getQueryable() {
								return currentProfile;
							}
						};
						element.setQueryable(currentProfile);
						input = element;

					}
					final Object viewerInput = input;
					Display.getDefault().asyncExec(new Runnable() {

						public void run() {
							viewer.setInput(viewerInput);
							updatePageCompletion();
						}
					});
				}
			});
		} catch (InvocationTargetException e) {
			setErrorMessage(e.getLocalizedMessage());
			setPageComplete(false);
		} catch (InterruptedException e) {
			// won't happen
		}
	}

	@Override
	protected boolean validDestination() {
		if (this.destinationNameField == null)
			return true;
		File file = new File(getDestinationValue());
		return file.exists() && file.isDirectory();
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

	@Override
	protected ITableLabelProvider getLabelProvider() {
		return new ImportFromInstallationLabelProvider();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (otherInstanceAgent != null) {
			otherInstanceAgent.stop();
			otherInstanceAgent = null;
		}
		if (getWizard().performCancel())
			cleanLocalRepository();
	}

	public void cleanLocalRepository() {
		if (metaURIs != null && metaURIs.length > 0) {
			IProvisioningAgent agent = getProvisioningUI().getSession().getProvisioningAgent();
			IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			for (URI uri : metaURIs)
				manager.removeRepository(uri);
			IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
			for (URI uri : artiURIs)
				artifactManager.removeRepository(uri);
		}
	}

	public Object[] getCheckedIUElements() {
		return viewer.getCheckedElements();
	}

	public Object[] getSelectedIUElements() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setCheckedElements(Object[] elements) {
		new UnsupportedOperationException();
	}

	public ProvisioningContext getProvisioningContext() {
		ProvisioningContext context = new ProvisioningContext(getProvisioningUI().getSession().getProvisioningAgent());
		context.setArtifactRepositories(artiURIs);
		context.setMetadataRepositories(metaURIs);
		return context;
	}
}