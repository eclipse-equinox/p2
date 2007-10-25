/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.frameworkadmin.Manipulator;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.osgi.framework.Version;

public class EclipseTouchpoint extends Touchpoint {
	private static final TouchpointType TOUCHPOINT_TYPE = new TouchpointType("eclipse", new Version("1.0"));
	private static final String ACTION_ADD_JVM_ARG = "addJvmArg"; //$NON-NLS-1$
	private static final String ACTION_ADD_PROGRAM_ARG = "addProgramArg"; //$NON-NLS-1$
	private static final String ACTION_COLLECT = "collect"; //$NON-NLS-1$
	private static final String ACTION_INSTALL_BUNDLE = "installBundle"; //$NON-NLS-1$
	private static final String ACTION_MARK_STARTED = "markStarted"; //$NON-NLS-1$
	private static final String ACTION_REMOVE_JVM_ARG = "removeJvmArg"; //$NON-NLS-1$
	private static final String ACTION_REMOVE_PROGRAM_ARG = "removeProgramArg"; //$NON-NLS-1$
	private static final String ACTION_SET_FW_DEPENDENT_PROP = "setFwDependentProp"; //$NON-NLS-1$
	private static final String ACTION_SET_FW_INDEPENDENT_PROP = "setFwIndependentProp"; //$NON-NLS-1$
	private static final String ACTION_UNINSTALL_BUNDLE = "uninstallBundle"; //$NON-NLS-1$
	final static String ID = "org.eclipse.equinox.p2.touchpoint.eclipse"; //$NON-NLS-1$

	private static final String PARM_ARTIFACT = "@artifact"; //$NON-NLS-1$
	private static final String PARM_ARTIFACT_REQUESTS = "artifactRequests"; //$NON-NLS-1$
	private static final String PARM_BUNDLE = "bundle"; //$NON-NLS-1$
	private static final String PARM_INSTALL_FOLDER = "installFolder"; //$NON-NLS-1$
	private static final String PARM_IU = "iu"; //$NON-NLS-1$
	private static final String PARM_JVM_ARG = "jvmArg"; //$NON-NLS-1$
	private static final String PARM_MANIPULATOR = "manipulator"; //$NON-NLS-1$
	private static final String PARM_OPERAND = "operand"; //$NON-NLS-1$
	private static final String PARM_PREVIOUS_START_LEVEL = "previousStartLevel"; //$NON-NLS-1$
	private static final String PARM_PREVIOUS_STARTED = "previousStarted"; //$NON-NLS-1$
	private static final String PARM_PREVIOUS_VALUE = "previousValue"; //$NON-NLS-1$
	private static final String PARM_PROFILE = "profile"; //$NON-NLS-1$
	private static final String PARM_PROGRAM_ARG = "programArg"; //$NON-NLS-1$
	private static final String PARM_PROP_NAME = "propName"; //$NON-NLS-1$
	private static final String PARM_PROP_VALUE = "propValue"; //$NON-NLS-1$
	private static final String PARM_SET_START_LEVEL = "setStartLevel"; //$NON-NLS-1$
	private static final String PARM_START_LEVEL = "startLevel"; //$NON-NLS-1$
	private static final String PARM_STARTED = "started"; //$NON-NLS-1$

	// TODO: Here we may want to consult multiple caches
	IArtifactRequest[] collect(IInstallableUnit installableUnit, Profile profile) {
		IArtifactRepository targetRepo = null;
		IArtifactKey[] toDownload = installableUnit.getArtifacts();
		if (toDownload == null)
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;
		IArtifactRequest[] requests = new IArtifactRequest[toDownload.length];

		IFileArtifactRepository bundlePool = Util.getBundlePoolRepo(profile);
		if (isCompletelyInRepo(bundlePool, toDownload))
			return IArtifactRepositoryManager.NO_ARTIFACT_REQUEST;

		//If the installable unit has installation information, then the artifact is put in the download cache
		//otherwise it is a jar'ed bundle and we directly store it in the plugin cache
		if (isZipped(installableUnit.getTouchpointData())) {
			targetRepo = Util.getDownloadCacheRepo();
		} else {
			targetRepo = bundlePool;
		}
		int count = 0;
		for (int i = 0; i < toDownload.length; i++) {
			IArtifactKey key = toDownload[i];
			if (!targetRepo.contains(key)) {
				requests[count++] = Util.getArtifactRepositoryManager().createMirrorRequest(key, targetRepo);
			}
		}

		if (requests.length == count)
			return requests;
		IArtifactRequest[] result = new IArtifactRequest[count];
		System.arraycopy(requests, 0, result, 0, count);
		return result;
	}

	public IStatus completePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		Manipulator manipulator = (Manipulator) touchpointParameters.get(PARM_MANIPULATOR);
		try {
			manipulator.save(false);
		} catch (RuntimeException e) {
			return new Status(IStatus.ERROR, Activator.ID, 1, "Error saving manipulator", e);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, 1, "Error saving manipulator", e);
		}
		return null;
	}

	public ProvisioningAction getAction(String actionId) {
		if (actionId.equals(ACTION_INSTALL_BUNDLE)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return installBundle(parameters);
				}

				public IStatus undo(Map parameters) {
					return uninstallBundle(parameters);
				}
			};
		}

		if (actionId.equals(ACTION_UNINSTALL_BUNDLE)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					return uninstallBundle(parameters);
				}

				public IStatus undo(Map parameters) {
					return installBundle(parameters);
				}
			};
		}

		if (actionId.equals(ACTION_COLLECT)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Profile profile = (Profile) parameters.get(PARM_PROFILE);
					Operand operand = (Operand) parameters.get(PARM_OPERAND);
					IArtifactRequest[] requests = collect(operand.second(), profile);

					Collection artifactRequests = (Collection) parameters.get(PARM_ARTIFACT_REQUESTS);
					artifactRequests.add(requests);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					// nothing to do for now
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_ADD_PROGRAM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
						IArtifactKey artifactKey = iu.getArtifacts()[0];

						File fileLocation = null;
						try {
							fileLocation = Util.getBundleFile(artifactKey, isZipped(iu.getTouchpointData()), profile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (!fileLocation.exists())
							return new Status(IStatus.ERROR, ID, "The file is not available" + fileLocation.getAbsolutePath());
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().addProgramArg(programArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
						IArtifactKey artifactKey = iu.getArtifacts()[0];

						File fileLocation = null;
						try {
							fileLocation = Util.getBundleFile(artifactKey, isZipped(iu.getTouchpointData()), profile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (!fileLocation.exists())
							return new Status(IStatus.ERROR, ID, "The file is not available" + fileLocation.getAbsolutePath());
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().removeProgramArg(programArg);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_REMOVE_PROGRAM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
						IArtifactKey artifactKey = iu.getArtifacts()[0];

						File fileLocation = null;
						try {
							fileLocation = Util.getBundleFile(artifactKey, isZipped(iu.getTouchpointData()), profile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (!fileLocation.exists())
							return new Status(IStatus.ERROR, ID, "The file is not available" + fileLocation.getAbsolutePath());
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().removeProgramArg(programArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String programArg = (String) parameters.get(PARM_PROGRAM_ARG);

					if (programArg.equals(PARM_ARTIFACT)) {
						Profile profile = (Profile) parameters.get(PARM_PROFILE);
						IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
						IArtifactKey artifactKey = iu.getArtifacts()[0];

						File fileLocation = null;
						try {
							fileLocation = Util.getBundleFile(artifactKey, isZipped(iu.getTouchpointData()), profile);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (!fileLocation.exists())
							return new Status(IStatus.ERROR, ID, "The file is not available" + fileLocation.getAbsolutePath());
						programArg = fileLocation.getAbsolutePath();
					}

					manipulator.getLauncherData().addProgramArg(programArg);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(PARM_SET_START_LEVEL)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
					String startLevel = (String) parameters.get(PARM_START_LEVEL);

					BundleInfo bundleInfo = new BundleInfo();
					Util.initFromManifest(Util.getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							getMemento().put(PARM_PREVIOUS_START_LEVEL, new Integer(bundles[i].getStartLevel()));
							bundles[i].setStartLevel(Integer.parseInt(startLevel));
							break;
						}
					}
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);

					BundleInfo bundleInfo = new BundleInfo();
					Util.initFromManifest(Util.getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							Integer previousStartLevel = (Integer) getMemento().get(PARM_PREVIOUS_START_LEVEL);
							if (previousStartLevel != null)
								bundles[i].setStartLevel(previousStartLevel.intValue());
							break;
						}
					}
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_MARK_STARTED)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
					String started = (String) parameters.get(PARM_STARTED);

					BundleInfo bundleInfo = new BundleInfo();
					Util.initFromManifest(Util.getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							getMemento().put(PARM_PREVIOUS_STARTED, new Boolean(bundles[i].isMarkedAsStarted()));
							bundles[i].setMarkedAsStarted(Boolean.valueOf(started).booleanValue());
							break;
						}
					}
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);

					BundleInfo bundleInfo = new BundleInfo();
					Util.initFromManifest(Util.getManifest(iu.getTouchpointData()), bundleInfo);
					BundleInfo[] bundles = manipulator.getConfigData().getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].equals(bundleInfo)) {
							Boolean previousStarted = (Boolean) getMemento().get(PARM_PREVIOUS_STARTED);
							if (previousStarted != null)
								bundles[i].setMarkedAsStarted(previousStarted.booleanValue());
							break;
						}
					}
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_SET_FW_DEPENDENT_PROP)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					String propValue = (String) parameters.get(PARM_PROP_VALUE);
					getMemento().put(PARM_PREVIOUS_VALUE, manipulator.getConfigData().getFwDependentProp(propName));
					manipulator.getConfigData().setFwDependentProp(propName, propValue);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					String previousValue = (String) getMemento().get(PARM_PREVIOUS_VALUE);
					manipulator.getConfigData().setFwDependentProp(propName, previousValue);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_SET_FW_INDEPENDENT_PROP)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					String propValue = (String) parameters.get(PARM_PROP_VALUE);
					getMemento().put(PARM_PREVIOUS_VALUE, manipulator.getConfigData().getFwDependentProp(propName));
					manipulator.getConfigData().setFwIndependentProp(propName, propValue);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String propName = (String) parameters.get(PARM_PROP_NAME);
					String previousValue = (String) getMemento().get(PARM_PREVIOUS_VALUE);
					manipulator.getConfigData().setFwIndependentProp(propName, previousValue);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_ADD_JVM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					manipulator.getLauncherData().addJvmArg(jvmArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					manipulator.getLauncherData().removeJvmArg(jvmArg);
					return Status.OK_STATUS;
				}
			};
		}

		if (actionId.equals(ACTION_REMOVE_JVM_ARG)) {
			return new ProvisioningAction() {
				public IStatus execute(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					manipulator.getLauncherData().removeJvmArg(jvmArg);
					return Status.OK_STATUS;
				}

				public IStatus undo(Map parameters) {
					Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
					String jvmArg = (String) parameters.get(PARM_JVM_ARG);
					manipulator.getLauncherData().addJvmArg(jvmArg);
					return Status.OK_STATUS;
				}
			};
		}

		return null;
	}

	public TouchpointType getTouchpointType() {
		//TODO this data probably needs to come from the XML
		return TOUCHPOINT_TYPE;
	}

	public IStatus initializePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		touchpointParameters.put(PARM_MANIPULATOR, new LazyManipulator(profile));
		touchpointParameters.put(PARM_INSTALL_FOLDER, Util.getInstallFolder(profile));
		return null;
	}

	IStatus installBundle(Map parameters) {

		Profile profile = (Profile) parameters.get(PARM_PROFILE);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
		String bundleId = (String) parameters.get(PARM_BUNDLE);

		//TODO: eventually remove this. What is a fragment doing here??
		if (iu.isFragment()) {
			System.out.println("What is a fragment doing here!!! -- " + iu);
			return Status.OK_STATUS;
		}

		IArtifactKey[] artifacts = iu.getArtifacts();
		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].toString().equals(bundleId)) {
				artifactKey = artifacts[i];
				break;
			}
		}
		if (artifactKey == null)
			throw new IllegalArgumentException("No artifact found that matches: " + bundleId);

		boolean isZipped = isZipped(iu.getTouchpointData());
		File bundleFile;
		try {
			bundleFile = Util.getBundleFile(artifactKey, isZipped, profile);
			if (bundleFile == null)
				return new Status(IStatus.ERROR, ID, "The artifact " + artifactKey.toString() + " to install was not found.");

		} catch (IOException e) {
			return new Status(IStatus.ERROR, ID, e.getMessage());
		}

		// TODO: do we really need the manifest here or just the bsn and version?
		String manifest = Util.getManifest(iu.getTouchpointData());

		BundleInfo bundleInfo = Util.createBundleInfo(bundleFile, manifest);
		manipulator.getConfigData().addBundle(bundleInfo);

		return Status.OK_STATUS;
	}

	private boolean isCompletelyInRepo(IArtifactRepository repo, IArtifactKey[] toDownload) {
		for (int i = 0; i < toDownload.length; i++) {
			if (!repo.contains(toDownload[i]))
				return false;
		}
		return true;
	}

	//	private void logConfiguation(IInstallableUnit unit, String instructions, boolean isInstall) {
	//		//  TODO: temporary for debugging; replace by logging
	//		if (DEBUG) {
	//			System.out.print("[" + iuCount + "] ");
	//			if (isInstall) {
	//				System.out.println("Installing " + unit + " with: " + instructions);
	//			} else {
	//				System.out.println("Uninstalling " + unit + " with: " + instructions);
	//			}
	//			iuCount++;
	//		}
	//	}

	boolean isZipped(TouchpointData[] data) {
		if (data == null || data.length == 0)
			return false;
		for (int i = 0; i < data.length; i++) {
			if (data[i].getInstructions("zipped") != null) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	protected IStatus uninstallBundle(Map parameters) {
		Profile profile = (Profile) parameters.get(PARM_PROFILE);
		IInstallableUnit iu = (IInstallableUnit) parameters.get(PARM_IU);
		Manipulator manipulator = (Manipulator) parameters.get(PARM_MANIPULATOR);
		String bundleId = (String) parameters.get(PARM_BUNDLE);

		//TODO: eventually remove this. What is a fragment doing here??
		if (iu.isFragment()) {
			System.out.println("What is a fragment doing here!!! -- " + iu);
			return Status.OK_STATUS;
		}

		IArtifactKey[] artifacts = iu.getArtifacts();
		IArtifactKey artifactKey = null;
		for (int i = 0; i < artifacts.length; i++) {
			if (artifacts[i].toString().equals(bundleId)) {
				artifactKey = artifacts[i];
				break;
			}
		}
		if (artifactKey == null)
			throw new IllegalArgumentException("No artifact found that matches: " + bundleId);

		boolean isZipped = isZipped(iu.getTouchpointData());
		File bundleFile;
		try {
			bundleFile = Util.getBundleFile(artifactKey, isZipped, profile);
			if (bundleFile == null)
				return new Status(IStatus.ERROR, ID, "The artifact " + artifactKey.toString() + " to uninstall was not found.");

		} catch (IOException e) {
			return new Status(IStatus.ERROR, ID, e.getMessage());
		}

		// TODO: do we really need the manifest here or just the bsn and version?
		String manifest = Util.getManifest(iu.getTouchpointData());

		BundleInfo bundleInfo = Util.createBundleInfo(bundleFile, manifest);
		manipulator.getConfigData().removeBundle(bundleInfo);

		return Status.OK_STATUS;
	}
}
