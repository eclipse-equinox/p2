/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.sdk.externalFiles;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.BundleDescriptionFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.ServiceReference;

/**
 * Utility class that handles files or folders that are not recognized as valid
 * repositories.  Consults the user (and user preferences) to determine how to handle
 * the file.
 * 
 * @since 3.4
 *
 */
public class ExternalFileHandler {

	public static final int REPO_GENERATED = 3000;
	public static final int BUNDLE_INSTALLED = 3001;
	private static final String FILE = "file"; //$NON-NLS-1$
	File file;
	Shell shell;
	IProfile profile;

	public ExternalFileHandler(IProfile profile, File file, Shell shell) {
		this.file = file;
		this.shell = shell;
		this.profile = profile;
	}

	public IStatus processFile(IStatus originalStatus) {
		/*	if (file == null)
					return originalStatus;

				if (file.isDirectory()) {
					return generateRepoFromDirectory(originalStatus);
				}
				if (isBundle()) {
					return autoInstallBundle(originalStatus);
				}
				if (isArchive()) {
					return generateRepoFromArchive(originalStatus);
				}
		*/
		return originalStatus;
	}

	IStatus generateRepoFromDirectory(IStatus originalStatus) {
		String generateRepo = ProvSDKUIActivator.getDefault().getPreferenceStore().getString(PreferenceConstants.PREF_GENERATE_REPOFOLDER);
		if (MessageDialogWithToggle.NEVER.equals(generateRepo))
			return originalStatus;
		if (MessageDialogWithToggle.ALWAYS.equals(generateRepo)) {
			IMetadataRepository repository = generateRepository(file, file, false);
			if (repository != null)
				return generateOKStatus();
			return originalStatus;
		}
		final ConfirmRepoGenerationDialog dialog = new ConfirmRepoGenerationDialog(shell, file, file);
		dialog.open();

		// Any answer but yes means report an error
		if (dialog.getReturnCode() == IDialogConstants.YES_ID) {
			IMetadataRepository repository = generateRepository(file, dialog.getTargetLocation(), false);
			if (repository != null)
				return generateOKStatus();
		}
		return originalStatus;
	}

	IStatus generateRepoFromArchive(IStatus originalStatus) {
		String generateRepoFromArchive = ProvSDKUIActivator.getDefault().getPreferenceStore().getString(PreferenceConstants.PREF_GENERATE_ARCHIVEREPOFOLDER);
		if (MessageDialogWithToggle.NEVER.equals(generateRepoFromArchive))
			return originalStatus;
		File targetLocation = getDefaultUnzipFolder();
		if (targetLocation == null)
			return originalStatus;
		if (MessageDialogWithToggle.ALWAYS.equals(generateRepoFromArchive)) {
			IMetadataRepository repository = generateRepository(file, targetLocation, true);
			if (repository != null)
				return generateOKStatus();
			return originalStatus;
		}

		ConfirmRepoGenerationFromArchiveDialog dialog = new ConfirmRepoGenerationFromArchiveDialog(shell, file, targetLocation);
		dialog.open();

		// Any answer but yes means report an original status
		if (dialog.getReturnCode() == IDialogConstants.YES_ID) {
			IMetadataRepository repository = generateRepository(file, dialog.getTargetLocation(), true);
			if (repository != null)
				return generateOKStatus();
			return originalStatus;
		}
		return originalStatus;
	}

	IStatus autoInstallBundle(IStatus originalStatus) {
		try {
			String autoInstallBundle = ProvSDKUIActivator.getDefault().getPreferenceStore().getString(PreferenceConstants.PREF_AUTO_INSTALL_BUNDLES);
			if (MessageDialogWithToggle.NEVER.equals(autoInstallBundle))
				return originalStatus;
			if (MessageDialogWithToggle.ALWAYS.equals(autoInstallBundle)) {
				if (copyToDropins(file))
					return installOKStatus();
				return originalStatus;
			}
			MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(shell, ProvSDKMessages.ProvSDKUIActivator_Question, NLS.bind(ProvSDKMessages.ExternalFileHandler_PromptForInstallBundle, file.getAbsolutePath()), null, false, ProvSDKUIActivator.getDefault().getPreferenceStore(), PreferenceConstants.PREF_AUTO_INSTALL_BUNDLES);
			// Any answer but yes means report original status
			if (dialog.getReturnCode() == IDialogConstants.YES_ID) {
				if (copyToDropins(file))
					return installOKStatus();
			}
		} catch (FileNotFoundException e) {
			// Shouldn't happen, but maybe user deleted it just after selecting it?
			ProvUI.handleException(e, NLS.bind(ProvSDKMessages.ExternalFileHandler_ErrorCopyingFile, file.getAbsolutePath()), StatusManager.SHOW | StatusManager.LOG);
		} catch (IOException e) {
			ProvUI.handleException(e, NLS.bind(ProvSDKMessages.ExternalFileHandler_ErrorCopyingFile, file.getAbsolutePath()), StatusManager.SHOW | StatusManager.LOG);
		}
		return originalStatus;
	}

	boolean copyToDropins(File source) throws IOException {
		boolean success = false;
		/*		File dropinsFolder = ReconcilerHelper.getDropInsDirectory();
				if (!dropinsFolder.exists())
					dropinsFolder.mkdirs();
				File copiedBundle = new File(dropinsFolder, source.getName());
				if (!copiedBundle.exists())
					copiedBundle.createNewFile();
				success = FileUtils.copyStream(new FileInputStream(file), true, new FileOutputStream(copiedBundle), true) > 0;
				if (success)
					ProvUI.requestRestart(true, shell);
		*/
		return success;
	}

	IMetadataRepository generateRepository(final File source, final File targetLocation, final boolean unzipSource) {
		final IMetadataRepository[] repository = new IMetadataRepository[1];
		/*		runBackground(new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						try {
							File src = source;
							if (unzipSource) {
								FileUtils.unzipFile(source, targetLocation);
								src = targetLocation;
							}
							repository[0] = ReconcilerHelper.generateRepository(profile, src, targetLocation, makeRepositoryName(src), false);
						} catch (IOException e) {
							ProvUI.handleException(e, NLS.bind(ProvSDKMessages.ExternalFileHandler_ErrorExpandingArchive, source.getAbsolutePath()), StatusManager.SHOW | StatusManager.LOG);
						}
					}
				});
		*/
		return repository[0];
	}

	// TODO we may want to allow the user to name a generated repo.
	// For now we create a name.
	String makeRepositoryName(File targetLocation) {
		return NLS.bind(ProvSDKMessages.ExternalFileHandler_UserGeneratedRepoName, targetLocation.getAbsolutePath());

	}

	boolean isBundle() {
		if (file == null)
			return false;
		BundleDescriptionFactory factory = getBundleDescriptionFactory();
		if (factory == null)
			return false;
		return factory.getBundleDescription(file) != null;
	}

	private BundleDescriptionFactory getBundleDescriptionFactory() {

		ServiceReference reference = ProvSDKUIActivator.getContext().getServiceReference(PlatformAdmin.class.getName());
		if (reference == null)
			return null;
		PlatformAdmin platformAdmin = (PlatformAdmin) ProvSDKUIActivator.getContext().getService(reference);
		if (platformAdmin == null)
			return null;

		try {
			StateObjectFactory stateObjectFactory = platformAdmin.getFactory();
			return new BundleDescriptionFactory(stateObjectFactory, null);
		} finally {
			ProvSDKUIActivator.getContext().ungetService(reference);
		}
	}

	boolean isArchive() {
		if (file == null)
			return false;
		InputStream in = null;
		ZipInputStream zipStream = null;
		try {
			in = new FileInputStream(file);
			zipStream = new ZipInputStream(new BufferedInputStream(in));
			ZipEntry ze = zipStream.getNextEntry();
			if (ze == null) {
				in.close();
				zipStream.close();
				return false;
			}
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			safeClose(in);
			safeClose(zipStream);
		}
		return true;
	}

	void safeClose(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException e) {
			// ignore
		}
	}

	File getDefaultUnzipFolder() {
		File unzipFolder;
		// Make an auto-generated repo location a peer to the drop-ins directory
		/*
				String dropInsParent = ReconcilerHelper.getDropInsDirectory().getParent();
				if (dropInsParent != null) {
					unzipFolder = new File(dropInsParent, file.getName());
					unzipFolder = makeUnusedFolder(unzipFolder);
					if (unzipFolder != null)
						return unzipFolder;
				}
		*/
		// For some reason we had a failure with the drop-ins directory
		Location location = Platform.getConfigurationLocation();
		if (location != null) {
			URL configURL = location.getURL();
			if (configURL != null && configURL.getProtocol().startsWith(FILE)) {
				unzipFolder = new File(configURL.getFile(), file.getName());
				unzipFolder = makeUnusedFolder(unzipFolder);
				if (unzipFolder != null)
					return unzipFolder;
			}
		}
		return null;
	}

	File makeUnusedFolder(File suggestedFolder) {
		if (!suggestedFolder.exists()) {
			if (suggestedFolder.mkdir())
				return suggestedFolder;
		} else {
			// TODO hack, need to figure out what to do generate a unique name that's not too long, a la version qualifiers
			File generatedPath = new File(suggestedFolder.getParent(), suggestedFolder.getName() + new Long(System.currentTimeMillis()).toString());
			if (!generatedPath.exists()) {
				if (generatedPath.mkdir())
					return generatedPath;
			}
		}
		return null;
	}

	void runBackground(final IRunnableWithProgress runnable) {
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					runnable.run(monitor);
				}

			});
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), NLS.bind(ProvSDKMessages.ExternalFileHandler_UnexpectedError, file.getAbsoluteFile()), StatusManager.SHOW | StatusManager.LOG);

		} catch (InterruptedException e) {
			// Nothing to report
		}
	}

	IStatus generateOKStatus() {
		return new Status(IStatus.OK, ProvSDKUIActivator.PLUGIN_ID, REPO_GENERATED, NLS.bind(ProvSDKMessages.ExternalFileHandler_RepositoryGeneratedOK, file.getAbsolutePath()), null);
	}

	IStatus installOKStatus() {
		// temp
		return Status.OK_STATUS;
		//		return new Status(IStatus.OK, ProvSDKUIActivator.PLUGIN_ID, BUNDLE_INSTALLED, NLS.bind(ProvSDKMessages.ExternalFileHandler_BundleInstalledOK, ReconcilerHelper.getDropInsDirectory().getAbsolutePath()), null);
	}

}
