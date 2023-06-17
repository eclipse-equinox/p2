/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rapicorp, Inc - prompt to install debian package
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.touchpoint.natives.actions.ActionConstants;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.Touchpoint;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.osgi.util.NLS;

public class NativeTouchpoint extends Touchpoint {
	public static final String PARM_BACKUP = "backup"; //$NON-NLS-1$
	public static final String PARM_ARTIFACT = "artifact"; //$NON-NLS-1$
	public static final String PARM_ARTIFACT_LOCATION = "artifact.location"; //$NON-NLS-1$

	private static final String FOLDER = "nativePackageScripts"; //$NON-NLS-1$
	private static final String INSTALL_COMMANDS = "installCommands.txt"; //$NON-NLS-1$
	private static final String INSTALL_PREFIX = "installPrefix"; //$NON-NLS-1$

	private static Map<IProfile, IBackupStore> backups = new WeakHashMap<>();

	private static class NativePackageToInstallInfo {
		NativePackageEntry entry;
		IInstallableUnit iu;

		public NativePackageToInstallInfo(NativePackageEntry entry, IInstallableUnit iu) {
			this.entry = entry;
			this.iu = iu;
		}
	}

	private List<NativePackageToInstallInfo> packagesToInstall = new ArrayList<>();
	private Properties installCommandsProperties = new Properties();

	private IProvisioningAgent agent;
	private String distro;

	@Override
	public IStatus initializeOperand(IProfile profile, Map<String, Object> parameters) {
		agent = (IProvisioningAgent) parameters.get(ActionConstants.PARM_AGENT);
		IArtifactKey artifactKey = (IArtifactKey) parameters.get(PARM_ARTIFACT);
		if (!parameters.containsKey(PARM_ARTIFACT_LOCATION) && artifactKey != null) {
			try {
				IFileArtifactRepository downloadCache = Util.getDownloadCacheRepo(agent);
				File fileLocation = downloadCache.getArtifactFile(artifactKey);
				if (fileLocation != null && fileLocation.exists())
					parameters.put(PARM_ARTIFACT_LOCATION, fileLocation.getAbsolutePath());
			} catch (ProvisionException e) {
				return e.getStatus();
			}
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId,
			Map<String, Object> touchpointParameters) {
		touchpointParameters.put(PARM_BACKUP, getBackupStore(profile));
		return null;
	}

	@Override
	public String qualifyAction(String actionId) {
		return Activator.ID + "." + actionId; //$NON-NLS-1$
	}

	@Override
	public IStatus prepare(IProfile profile) {
		// does not have to do anything - everything is already in the correct place
		// the commit means that the backup is discarded - if that fails it is not a
		// terrible problem.
		return super.prepare(profile);
	}

	@Override
	public IStatus commit(IProfile profile) {
		promptForNativePackage();
		IBackupStore store = getBackupStore(profile);
		store.discard();
		clearProfileState(profile);
		return Status.OK_STATUS;
	}

	private void promptForNativePackage() {
		if (packagesToInstall.size() == 0)
			return;
		loadInstallCommandsProperties(installCommandsProperties, distro);
		UIServices serviceUI = agent.getService(UIServices.class);
		String text = Messages.PromptForNative_IntroText;
		String downloadLinks = ""; //$NON-NLS-1$
		List<NativePackageEntry> entriesWithoutDownloadLink = new ArrayList<>(packagesToInstall.size());
		for (NativePackageToInstallInfo nativePackageEntry : packagesToInstall) {
			text += '\t' + nativePackageEntry.entry.name + ' ' + formatVersion(nativePackageEntry.entry);
			if (nativePackageEntry.iu != null) {
				String name = nativePackageEntry.iu.getProperty(IInstallableUnit.PROP_NAME, null);
				if (name != null && !name.isEmpty()) {
					text += ' ';
					text += NLS.bind(Messages.NativeTouchpoint_PromptForNative_RequiredBy, name);
				}
			}

			text += '\n';
			if (nativePackageEntry.entry.getDownloadLink() != null) {
				downloadLinks += "    <a>" + nativePackageEntry.entry.getDownloadLink() + "</a>\n"; //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				entriesWithoutDownloadLink.add(nativePackageEntry.entry);
			}
		}

		String installCommands = createCommand(entriesWithoutDownloadLink);
		if (installCommands != null) {
			text += Messages.PromptForNative_InstallText + installCommands;
		}

		String downloadText = null;
		if (downloadLinks.length() > 0) {
			downloadText = Messages.NativeTouchpoint_PromptForNative_YouCanDownloadFrom + downloadLinks;
		}

		serviceUI.showInformationMessage(Messages.PromptForNative_DialogTitle, text, downloadText);
	}

	private String formatVersion(NativePackageEntry entry) {
		if (entry.getVersion() == null)
			return null;
		return getUserFriendlyComparator(entry.comparator) + ' ' + entry.version + ' ';
	}

	private String getUserFriendlyComparator(String comparator) {
		if (comparator == null)
			return ""; //$NON-NLS-1$
		return installCommandsProperties.getProperty(comparator, ""); //$NON-NLS-1$
	}

	public static void loadInstallCommandsProperties(Properties properties, String distro) {
		File f = getFileFromBundle(distro, INSTALL_COMMANDS);
		if (f == null)
			return;

		try (InputStream is = new BufferedInputStream(new FileInputStream(f))) {
			properties.load(is);
		} catch (IOException e) {
			// fallthrough to return empty string
		}
	}

	private String getInstallCommad() {
		return installCommandsProperties.getProperty(INSTALL_PREFIX, ""); //$NON-NLS-1$
	}

	private String createCommand(List<NativePackageEntry> packageEntries) {
		if (packageEntries.isEmpty())
			return null;
		String text = getInstallCommad() + ' ';
		for (NativePackageEntry nativePackageEntry : packageEntries) {
			text += nativePackageEntry.name + " "; //$NON-NLS-1$
		}
		return text;
	}

	/**
	 * Add the given entry as a new native package that needs to be installed.
	 * 
	 * @param entry Package information about the native
	 * @param iu    optional IU that has this requirement
	 */
	public void addPackageToInstall(NativePackageEntry entry, IInstallableUnit iu) {
		packagesToInstall.add(new NativePackageToInstallInfo(entry, iu));
	}

	public List<NativePackageEntry> getPackagesToInstall() {
		return Collections.unmodifiableList(packagesToInstall.stream().map(e -> e.entry).collect(Collectors.toList()));
	}

	public void setDistro(String distro) {
		this.distro = distro;
	}

	/**
	 * Converts a profile id into a string that can be used as a file name in any
	 * file system.
	 */
	public static String escape(String toEscape) {
		StringBuffer buffer = new StringBuffer();
		int length = toEscape.length();
		for (int i = 0; i < length; ++i) {
			char ch = toEscape.charAt(i);
			switch (ch) {
			case '\\':
			case '/':
			case ':':
			case '*':
			case '?':
			case '"':
			case '<':
			case '>':
			case '|':
			case '%':
				buffer.append("%" + (int) ch + ";"); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			default:
				buffer.append(ch);
			}
		}
		return buffer.toString();
	}

	@Override
	public IStatus rollback(IProfile profile) {
		IStatus returnStatus = Status.OK_STATUS;
		IBackupStore store = getBackupStore(profile);
		try {
			store.restore();
		} catch (IOException e) {
			returnStatus = new Status(IStatus.ERROR, Activator.ID,
					NLS.bind(Messages.failed_backup_restore, store.getBackupName()), e);
		} catch (ClosedBackupStoreException e) {
			returnStatus = new Status(IStatus.ERROR, Activator.ID,
					NLS.bind(Messages.failed_backup_restore, store.getBackupName()), e);
		}
		clearProfileState(profile);
		return returnStatus;
	}

	public static File getFileFromBundle(String distro, String file) {
		URL[] installScripts = FileLocator.findEntries(Activator.getContext().getBundle(),
				IPath.fromOSString(NativeTouchpoint.FOLDER + '/' + distro + '/' + file));
		if (installScripts.length == 0)
			return null;

		try {
			return URIUtil.toFile(URIUtil.toURI(FileLocator.toFileURL(installScripts[0])));
		} catch (URISyntaxException e) {
			// Can't happen, the URI is returned by OSGi
		} catch (IOException e) {
			// continue to return null
		}
		return null;
	}

	/**
	 * Cleans up the transactional state associated with a profile.
	 */
	private static synchronized void clearProfileState(IProfile profile) {
		backups.remove(profile);
	}

	/**
	 * Gets the transactional state associated with a profile. A transactional state
	 * is created if it did not exist.
	 * 
	 * @param profile
	 * @return a lazily initialized backup store
	 */
	private static synchronized IBackupStore getBackupStore(IProfile profile) {
		IBackupStore store = backups.get(profile);
		if (store == null) {
			store = new LazyBackupStore(escape(profile.getProfileId()));
			backups.put(profile, store);
		}
		return store;
	}
}
