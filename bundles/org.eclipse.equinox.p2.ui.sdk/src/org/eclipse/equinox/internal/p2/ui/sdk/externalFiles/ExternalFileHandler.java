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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.generator.BundleDescriptionFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.URLValidator;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
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

	private static final String FILE = "file"; //$NON-NLS-1$
	File file;
	Shell shell;

	// TODO
	// copied from reconciler activator until there is API
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=222456
	private static final String DROPINS_DIRECTORY = "org.eclipse.equinox.p2.reconciler.dropins.directory"; //$NON-NLS-1$
	private static final String OSGI_CONFIGURATION_AREA = "osgi.configuration.area"; //$NON-NLS-1$
	private static final String DROPINS = "dropins"; //$NON-NLS-1$

	public ExternalFileHandler(File file, Shell shell) {
		this.file = file;
		this.shell = shell;
	}

	public IStatus processFile(IStatus originalStatus) {
		if (file == null)
			return originalStatus;
		if (isBundle()) {
			return autoInstallBundle(originalStatus);
		}
		if (isArchive()) {
			return generateRepoFromArchive(originalStatus);
		}
		return originalStatus;
	}

	IStatus generateRepoFromArchive(IStatus originalStatus) {
		String generateRepoFromArchive = ProvSDKUIActivator.getDefault().getPreferenceStore().getString(PreferenceConstants.PREF_GENERATE_ARCHIVEREPOFOLDER);
		if (MessageDialogWithToggle.NEVER.equals(generateRepoFromArchive))
			return originalStatus;
		final File targetLocation = getDefaultUnzipFolder();
		if (targetLocation == null)
			return originalStatus;
		if (MessageDialogWithToggle.ALWAYS.equals(generateRepoFromArchive)) {
			final IMetadataRepository[] repository = new IMetadataRepository[1];
			BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
				public void run() {
					repository[0] = unzipAndGenerateRepository(file, targetLocation, null);
				}
			});
			if (repository[0] != null)
				return generateOKStatus();
			return originalStatus;
		}
		MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoCancelQuestion(shell, ProvSDKMessages.ProvSDKUIActivator_Question, NLS.bind(ProvSDKMessages.ExternalFileHandler_PromptForUnzip, file.getAbsolutePath()), null, false, ProvSDKUIActivator.getDefault().getPreferenceStore(), PreferenceConstants.PREF_GENERATE_ARCHIVEREPOFOLDER);

		if (dialog.getReturnCode() == IDialogConstants.YES_ID) {
			final IMetadataRepository[] repository = new IMetadataRepository[1];
			BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
				public void run() {
					repository[0] = unzipAndGenerateRepository(file, targetLocation, null);
				}
			});
			if (repository[0] != null)
				return generateOKStatus();
			return originalStatus;
		} else if (dialog.getReturnCode() == IDialogConstants.CANCEL_ID) {
			return Status.CANCEL_STATUS;
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
			MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoCancelQuestion(shell, ProvSDKMessages.ProvSDKUIActivator_Question, NLS.bind(ProvSDKMessages.ExternalFileHandler_PromptForInstallBundle, file.getAbsolutePath()), null, false, ProvSDKUIActivator.getDefault().getPreferenceStore(), PreferenceConstants.PREF_AUTO_INSTALL_BUNDLES);
			if (dialog.getReturnCode() == IDialogConstants.YES_ID) {
				if (copyToDropins(file))
					return installOKStatus();
			} else if (dialog.getReturnCode() == IDialogConstants.CANCEL_ID) {
				return Status.CANCEL_STATUS;
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
		File dropinsFolder = getDropInsDirectory();
		if (!dropinsFolder.exists())
			dropinsFolder.mkdirs();
		File copiedBundle = new File(dropinsFolder, source.getName());
		if (!copiedBundle.exists())
			copiedBundle.createNewFile();
		success = FileUtils.copyStream(new FileInputStream(file), true, new FileOutputStream(copiedBundle), true) > 0;
		if (success)
			ProvUI.requestRestart(true, shell);

		return success;
	}

	IMetadataRepository unzipAndGenerateRepository(final File source, final File targetLocation, IProgressMonitor monitor) {
		IMetadataRepository repository = null;
		try {
			FileUtils.unzipFile(source, targetLocation);
			URL repoLocation = new URL(URLValidator.makeFileURLString(targetLocation.getAbsolutePath()));
			repository = ProvisioningUtil.loadMetadataRepository(repoLocation, monitor);
		} catch (IOException e) {
			ProvUI.handleException(e, NLS.bind(ProvSDKMessages.ExternalFileHandler_ErrorExpandingArchive, source.getAbsolutePath()), StatusManager.SHOW | StatusManager.LOG);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, NLS.bind(ProvSDKMessages.ExternalFileHandler_ErrorLoadingFromZipDirectory, targetLocation.getAbsolutePath()), StatusManager.SHOW | StatusManager.LOG);
		}
		return repository;
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
		// Unzip content in a peer to the drop-ins directory
		String dropInsParent = getDropInsDirectory().getParent();
		if (dropInsParent != null) {
			unzipFolder = new File(dropInsParent, file.getName());
			unzipFolder = makeUnusedFolder(unzipFolder);
			if (unzipFolder != null)
				return unzipFolder;
		}
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

	IStatus generateOKStatus() {
		return new Status(IStatus.OK, ProvSDKUIActivator.PLUGIN_ID, URLValidator.REPO_AUTO_GENERATED, NLS.bind(ProvSDKMessages.ExternalFileHandler_RepositoryGeneratedOK, file.getAbsolutePath()), null);
	}

	IStatus installOKStatus() {
		return new Status(IStatus.OK, ProvSDKUIActivator.PLUGIN_ID, URLValidator.ALTERNATE_ACTION_TAKEN, NLS.bind(ProvSDKMessages.ExternalFileHandler_BundleInstalledOK, getDropInsDirectory().getAbsolutePath()), null);
	}

	// TODO
	// copied from reconciler activator
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=222456
	File getDropInsDirectory() {

		String watchedDirectoryProperty = ProvSDKUIActivator.getContext().getProperty(DROPINS_DIRECTORY);
		if (watchedDirectoryProperty != null) {
			File folder = new File(watchedDirectoryProperty);
			return folder;
		}
		try {
			//TODO: a proper install area would be better. osgi.install.area is relative to the framework jar
			URL baseURL = new URL(ProvSDKUIActivator.getContext().getProperty(OSGI_CONFIGURATION_AREA));
			URL folderURL = new URL(baseURL, "../" + DROPINS); //$NON-NLS-1$
			File folder = new File(folderURL.getPath());
			return folder;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
