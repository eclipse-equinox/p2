/*******************************************************************************
 * Copyright (c) 2008 Code 9 and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.publisher;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;

public abstract class AbstractPublishingAction implements IPublishingAction {
	public static final int AS_IS = 1;
	public static final int INCLUDE_ROOT = 2;
	public static final String CONFIG_SEGMENT_SEPARATOR = "."; //$NON-NLS-1$

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

	/**
	 * Returns a string array of { ws, os, arch } as parsed from the given string
	 * @param configSpec the string to parse
	 * @return the ws, os, arch form of the given string
	 */
	public static String[] parseConfigSpec(String configSpec) {
		String[] result = getArrayFromString(configSpec, CONFIG_SEGMENT_SEPARATOR);
		return result;
	}

	/**
	 * Returns the LDAP filter form that matches the given config spec.  Returns
	 * an empty String if the spec does not identify an ws, os or arch.
	 * @param configSpec a config spec to filter
	 * @return the LDAP filter for the given spec.  
	 */
	public static String createFilterSpec(String configSpec) {
		String[] config = parseConfigSpec(configSpec);
		if (config[0] != null || config[1] != null || config[2] != null) {
			String filterWs = config[0] != null ? "(osgi.ws=" + config[0] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterOs = config[1] != null ? "(osgi.os=" + config[1] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String filterArch = config[2] != null ? "(osgi.arch=" + config[2] + ")" : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return "(& " + filterWs + filterOs + filterArch + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return ""; //$NON-NLS-1$
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

	protected void publishArtifact(IArtifactDescriptor descriptor, File[] files, IPublisherInfo info, int mode) {
		publishArtifact(descriptor, files, null, info, mode);
	}

	protected void publishArtifact(IArtifactDescriptor descriptor, File base, File[] files, IPublisherInfo info, int mode) {
		IArtifactRepository destination = info.getArtifactRepository();
		if (descriptor == null || destination == null)
			return;

		// publish the given files
		publishArtifact(descriptor, files, null, info, mode);

		// if we are assimilating pack200 files then add the packed descriptor
		// into the repo assuming it does not already exist.
		boolean reuse = "true".equals(destination.getProperties().get(AbstractPublisherApplication.PUBLISH_PACK_FILES_AS_SIBLINGS)); //$NON-NLS-1$
		if (base != null && reuse && (info.getArtifactOptions() & IPublisherInfo.A_PUBLISH) > 0) {
			File packFile = new Path(base.getAbsolutePath()).addFileExtension("pack.gz").toFile(); //$NON-NLS-1$
			if (packFile.exists()) {
				IArtifactDescriptor ad200 = MetadataGeneratorHelper.createPack200ArtifactDescriptor(descriptor.getArtifactKey(), packFile, descriptor.getProperty(IArtifactDescriptor.ARTIFACT_SIZE));
				publishArtifact(ad200, new File[] {packFile}, null, info, AS_IS | INCLUDE_ROOT);
			}
		}
	}

	protected void publishArtifact(IArtifactDescriptor descriptor, File[] files, File root, IPublisherInfo info, int mode) {
		IArtifactRepository destination = info.getArtifactRepository();

		// if the destination already contains the descriptor, there is nothing to do.
		if (destination.contains(descriptor))
			return;
		// if all we are doing is indexing things then add the descriptor and get on with ti
		if ((info.getArtifactOptions() & IPublisherInfo.A_PUBLISH) == 0) {
			destination.addDescriptor(descriptor);
			return;
		}

		boolean overwrite = (info.getArtifactOptions() & IPublisherInfo.A_OVERWRITE) > 0;
		// if there is just one file and the mode is as-is, just copy the file into the repo
		if (((mode & AS_IS) > 0) && files.length == 1) {
			try {
				if (destination instanceof IFileArtifactRepository) {
					// TODO  need to review this logic to ensure it makes sense.
					// if the file is already in the same location the repo will put it, just add the descriptor and exit
					File descriptorFile = ((IFileArtifactRepository) destination).getArtifactFile(descriptor);
					if (files[0].equals(descriptorFile)) {
						destination.addDescriptor(descriptor);
						return;
					}
				}

				OutputStream output = destination.getOutputStream(descriptor, overwrite);
				if (output == null)
					return;
				output = new BufferedOutputStream(output);
				FileUtils.copyStream(new BufferedInputStream(new FileInputStream(files[0])), true, output, true);
			} catch (ProvisionException e) {
				LogHelper.log(e.getStatus());
			} catch (IOException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error publishing artifacts", e)); //$NON-NLS-1$
			}
		} else {
			// otherwise, zip up the files and copy the zip into the repo
			File tempFile = null;
			try {
				OutputStream output = destination.getOutputStream(descriptor, overwrite);
				if (output == null)
					return;
				output = new BufferedOutputStream(output);
				tempFile = File.createTempFile("p2.generator", ""); //$NON-NLS-1$ //$NON-NLS-2$
				if (root == null)
					FileUtils.zip(files, tempFile, (mode & INCLUDE_ROOT) > 0);
				else
					FileUtils.zip(files, tempFile, root);
				if (output != null)
					FileUtils.copyStream(new BufferedInputStream(new FileInputStream(tempFile)), true, output, true);
			} catch (ProvisionException e) {
				LogHelper.log(e.getStatus());
			} catch (IOException e) {
				LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Error publishing artifacts", e)); //$NON-NLS-1$
			} finally {
				if (tempFile != null)
					tempFile.delete();
			}
		}
	}

	public abstract IStatus perform(IPublisherInfo info, IPublisherResult results);
}
