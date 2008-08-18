/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils.IPathComputer;
import org.eclipse.equinox.internal.p2.publisher.Activator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.publisher.actions.ICapabilityAdvice;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public abstract class AbstractPublisherAction implements IPublisherAction {
	public static final int AS_IS = 1;
	public static final String CONFIG_SEGMENT_SEPARATOR = "."; //$NON-NLS-1$
	private static final String CONFIG_ANY = "ANY"; //$NON-NLS-1$

	public static IArtifactDescriptor createPack200ArtifactDescriptor(IArtifactKey key, File pathOnDisk, String installSize) {
		final String PACKED_FORMAT = "packed"; //$NON-NLS-1$
		//TODO this size calculation is bogus
		ArtifactDescriptor result = new ArtifactDescriptor(key);
		if (pathOnDisk != null) {
			result.setProperty(IArtifactDescriptor.ARTIFACT_SIZE, installSize);
			// TODO - this is wrong but I'm testing a work-around for bug 205842
			result.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, Long.toString(pathOnDisk.length()));
		}
		ProcessingStepDescriptor[] steps = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
		result.setProcessingSteps(steps);
		result.setProperty(IArtifactDescriptor.FORMAT, PACKED_FORMAT);
		return result;
	}

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified.
	 */
	public static String[] getArrayFromString(String list, String separator) {
		if (list == null || list.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		List result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				result.add(token);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	public static ProvidedCapability createSelfCapability(String installableUnitId, Version installableUnitVersion) {
		return MetadataFactory.createProvidedCapability(PublisherHelper.IU_NAMESPACE, installableUnitId, installableUnitVersion);
	}

	/**
	 * Returns a string array of { ws, os, arch } as parsed from the given string
	 * @param configSpec the string to parse
	 * @return the ws, os, arch form of the given string
	 */
	public static String[] parseConfigSpec(String configSpec) {
		String[] result = getArrayFromString(configSpec, CONFIG_SEGMENT_SEPARATOR);
		for (int i = 0; i < result.length; i++)
			if (result[i].equals("*")) //$NON-NLS-1$
				result[i] = CONFIG_ANY;
		return result;
	}

	/**
	 * Returns the LDAP filter form that matches the given config spec.  Returns
	 * an empty String if the spec does not identify an ws, os or arch.
	 * @param configSpec a config spec to filter
	 * @return the LDAP filter for the given spec.  <code>null</code> if the given spec does not 
	 * parse into a filter.
	 */
	public static String createFilterSpec(String configSpec) {
		String[] config = parseConfigSpec(configSpec);
		if (config[0] != null || config[1] != null || config[2] != null) {
			String filterWs = config[0] != null && config[0] != CONFIG_ANY ? "(osgi.ws=" + config[0] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterOs = config[1] != null && config[1] != CONFIG_ANY ? "(osgi.os=" + config[1] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterArch = config[2] != null && config[2] != CONFIG_ANY ? "(osgi.arch=" + config[2] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (filterWs.length() == 0 && filterOs.length() == 0 && filterArch.length() == 0)
				return null;
			return "(& " + filterWs + filterOs + filterArch + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null; //$NON-NLS-1$
	}

	/**
	 * Returns the normalized string form of the given config spec.  This is useful for putting
	 * in IU ids etc. Note that the result is not intended to be machine readable (i.e., parseConfigSpec
	 * may not work on the result).
	 * @param configSpec the config spec to format
	 * @return the readable format of the given config spec
	 */
	public static String createIdString(String configSpec) {
		String[] config = parseConfigSpec(configSpec);
		return config[0] + '.' + config[1] + '.' + config[2];
	}

	/**
	 * Returns the canonical form of config spec with the given ws, os and arch.
	 * Note that the result is intended to be machine readable (i.e., parseConfigSpec
	 * will parse the the result).
	 * @param ws the window system
	 * @param os the operating system
	 * @param arch the machine architecture
	 * @return the machine readable format of the given config spec
	 */
	public static String createConfigSpec(String ws, String os, String arch) {
		return ws + '.' + os + '.' + arch;
	}

	//This is to hide FileUtils from other actions
	public static IPathComputer createRootPrefixComputer(final File root) {
		return FileUtils.createRootPathComputer(root);
	}

	public static Collection createIURequirements(Collection children) {
		ArrayList result = new ArrayList(children.size());
		for (Iterator i = children.iterator(); i.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) i.next();
			VersionRange range = new VersionRange(iu.getVersion(), true, iu.getVersion(), true);
			result.add(MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), range, iu.getFilter(), false, false));
		}
		return result;
	}

	//This is to hide FileUtils from other actions
	public static IPathComputer createParentPrefixComputer(int segmentsToKeep) {
		return FileUtils.createParentPrefixComputer(segmentsToKeep);
	}

	/**
	 * Publishes the artifact by zipping the <code>files</code> using <code>root</code>
	 * as a base for relative paths. Then copying the zip into the repository.
	 * @param descriptor used to identify the zip.
	 * @param inclusions and folders to be included in the zip. files can be null.
	 * @param root the base used to generate relative paths within the zip. root can be null.
	 * @param info the publisher info.
	 * @param mode of operation (include root, as is...). 
	 */
	protected void publishArtifact(IArtifactDescriptor descriptor, File[] inclusions, File[] exclusions, IPublisherInfo info, IPathComputer prefixComputer) {
		// no files to publish so this is done.
		if (inclusions == null || inclusions.length < 1)
			return;
		// if the destination already contains the descriptor, there is nothing to do.
		IArtifactRepository destination = info.getArtifactRepository();
		if (destination == null || destination.contains(descriptor))
			return;
		// if all we are doing is indexing things then add the descriptor and get on with it
		if ((info.getArtifactOptions() & IPublisherInfo.A_PUBLISH) == 0) {
			destination.addDescriptor(descriptor);
			return;
		}

		// TODO need to implement the overwrite story in the repos
		//		boolean overwrite = (info.getArtifactOptions() & IPublisherInfo.A_OVERWRITE) > 0;
		// if there is just one file and the mode is as-is, just copy the file into the repo
		// otherwise, zip up the files and copy the zip into the repo
		File tempFile = null;
		try {
			OutputStream output = destination.getOutputStream(descriptor);
			if (output == null)
				return;
			output = new BufferedOutputStream(output);
			tempFile = File.createTempFile("p2.generator", ""); //$NON-NLS-1$ //$NON-NLS-2$
			FileUtils.zip(inclusions, exclusions, tempFile, prefixComputer);
			if (output != null)
				FileUtils.copyStream(new BufferedInputStream(new FileInputStream(tempFile)), true, output, true);
		} catch (ProvisionException e) {
			LogHelper.log(e.getStatus());
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error publishing artifacts", e)); //$NON-NLS-1$
			e.printStackTrace();
		} finally {
			if (tempFile != null)
				tempFile.delete();
		}
	}

	/**
	 * Publishes the artifact by zipping the <code>files</code> using <code>root</code>
	 * as a base for relative paths. Then copying the zip into the repository.
	 * @param descriptor used to identify the zip.
	 * @param inclusions and folders to be included in the zip. files can be null.
	 * @param root the base used to generate relative paths within the zip. root can be null.
	 * @param info the publisher info.
	 * @param mode of operation (include root, as is...). 
	 */
	protected void publishArtifact(IArtifactDescriptor descriptor, File inclusion, IPublisherInfo info) {
		// no files to publish so this is done.
		if (inclusion == null)
			return;
		// if the destination already contains the descriptor, there is nothing to do.
		IArtifactRepository destination = info.getArtifactRepository();
		if (destination == null || destination.contains(descriptor))
			return;

		if (destination instanceof IFileArtifactRepository) {
			// TODO  need to review this logic to ensure it makes sense.
			// if the file is already in the same location the repo will put it, just add the descriptor and exit
			File descriptorFile = ((IFileArtifactRepository) destination).getArtifactFile(descriptor);
			if (inclusion.equals(descriptorFile)) {
				destination.addDescriptor(descriptor);
				return;
			}
		}

		// if all we are doing is indexing things then add the descriptor and get on with it
		if ((info.getArtifactOptions() & IPublisherInfo.A_PUBLISH) == 0) {
			destination.addDescriptor(descriptor);
			return;
		}
		try {
			if (destination instanceof IFileArtifactRepository) {
				// TODO  need to review this logic to ensure it makes sense.
				// if the file is already in the same location the repo will put it, just add the descriptor and exit
				File descriptorFile = ((IFileArtifactRepository) destination).getArtifactFile(descriptor);
				if (inclusion.equals(descriptorFile)) {
					destination.addDescriptor(descriptor);
					return;
				}
			}

			// TODO need to implement the overwrite story in the repos
			//			boolean overwrite = (info.getArtifactOptions() & IPublisherInfo.A_OVERWRITE) > 0;
			OutputStream output = destination.getOutputStream(descriptor);
			if (output == null)
				return;
			output = new BufferedOutputStream(output);
			FileUtils.copyStream(new BufferedInputStream(new FileInputStream(inclusion)), true, output, true);
		} catch (ProvisionException e) {
			LogHelper.log(e.getStatus());
		} catch (IOException e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error publishing artifacts", e)); //$NON-NLS-1$
		}
	}

	public abstract IStatus perform(IPublisherInfo info, IPublisherResult results);

	/**
	 * Add all of the advised provided and required capabilities for the given installable unit.
	 * @param iu the IU to decorate
	 * @param info the publisher info supplying the advice
	 */
	protected void processCapabilityAdvice(InstallableUnitDescription iu, IPublisherInfo info) {
		Collection advice = info.getAdvice(null, false, iu.getId(), iu.getVersion(), ICapabilityAdvice.class);
		for (Iterator i = advice.iterator(); i.hasNext();) {
			ICapabilityAdvice entry = (ICapabilityAdvice) i.next();
			RequiredCapability[] requiredAdvice = entry.getRequiredCapabilities(iu);
			ProvidedCapability[] providedAdvice = entry.getProvidedCapabilities(iu);
			if (providedAdvice != null) {
				RequiredCapability[] current = iu.getRequiredCapabilities();
				RequiredCapability[] result = new RequiredCapability[requiredAdvice.length + current.length];
				System.arraycopy(requiredAdvice, 0, result, 0, requiredAdvice.length);
				System.arraycopy(current, 0, result, requiredAdvice.length, current.length);
				iu.setRequiredCapabilities(result);
			}
			if (providedAdvice != null) {
				ProvidedCapability[] current = iu.getProvidedCapabilities();
				ProvidedCapability[] result = new ProvidedCapability[providedAdvice.length + current.length];
				System.arraycopy(providedAdvice, 0, result, 0, providedAdvice.length);
				System.arraycopy(current, 0, result, providedAdvice.length, current.length);
				iu.setCapabilities(result);
			}
		}
	}

	public InstallableUnitDescription createParentIU(Collection children, String id, Version version) {
		InstallableUnitDescription root = createIUShell(id, version);
		root.addRequiredCapabilities(createIURequirements(children));
		addSelfCapability(root);
		return root;
	}

	public void addSelfCapability(InstallableUnitDescription root) {
		root.setCapabilities(new ProvidedCapability[] {createSelfCapability(root.getId(), root.getVersion())});
	}

	public InstallableUnitDescription createIUShell(String id, Version version) {
		InstallableUnitDescription root = new MetadataFactory.InstallableUnitDescription();
		root.setId(id);
		root.setVersion(version);
		return root;
	}

}
