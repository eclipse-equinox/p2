/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  	compeople AG (Stefan Liebig) - initial API and implementation
 * 	IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.jbdiff;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.optimizers.VersionlessArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;

public class Optimizer {

	private IArtifactRepository repository;
	private int width;
	private int depth;
	private boolean nosar;

	private static final String JBPATCH_STEP_ID = "org.eclipse.equinox.p2.repository.JBPatchStep"; //$NON-NLS-1$
	private static final String JBPATCH_STEP_ZIP_ID = "org.eclipse.equinox.p2.repository.JBPatchZipStep"; //$NON-NLS-1$

	private static final Comparator<IArtifactDescriptor> ARTIFACT_DESCRIPTOR_VERSION_COMPARATOR = new ArtifactDescriptorVersionComparator();
	private static final Comparator<IArtifactKey> ARTIFACT_KEY_VERSION_COMPARATOR = new ArtifactKeyVersionComparator();

	/**
	 * This optimizer performs delta generation based on (currently) jbdiff. 
	 * The optimization can be controlled with the ´width´ and the ´depth´ parameter.
	 * ´width´ defines for how many ´related´ artifact keys a delta should be generated,
	 * starting from the most up-to-date.
	 * ´depth´ defines to how many predecessor a delta should be generated.
	 * 
	 * With AK(c-v) : AK - artifact key, c - artifact id, v - artifact version
	 * the ´repository content´ can be viewed a two dimensional array, where the
	 * artifact keys for the same component are in order of their version: 
	 * <pre><code>
	 *     w=1       w=2
	 *      |        |
	 *      | +------.------------+ d=2
	 *      | | +----.---+ d=1    |
	 *      | | |    |   |        v
	 * [    v | |    v   v        v
	 * [ AK(x,2.0) AK(x,1.5) AK(x,1.1) ]
	 * [ AK(y,2.0) AK(y,1.9) ]
	 * [ AK(z,2.0) AK(z,1.5) AK(z,1.3) AK(z,1.0) ]
	 * ]
	 * </code></pre>  
	 * E.g: with a ´width´ of one and a ´depth´ of two the optimizer would
	 * create two deltas for component ´x´ from 1.5 to 2.0 and from 1.1 to 2.0.    
	 * 
	 * @param repository
	 * @param width
	 * @param depth
	 * @param nosar 
	 */
	public Optimizer(IArtifactRepository repository, int width, int depth, boolean nosar) {
		this.repository = repository;
		this.width = width;
		this.depth = depth;
		this.nosar = nosar;
	}

	public void run() {
		System.out.println("Starting delta (jbdiff) optimizations (width=" + width + ", depth=" + depth + ", nosar=" + nosar + ")");
		IQueryResult<IArtifactKey> queryResult = repository.query(ArtifactKeyQuery.ALL_KEYS, null);
		IArtifactKey[][] keys = getSortedRelatedArtifactKeys(queryResult);
		for (int i = 0; i < keys.length; i++) {
			if (keys[i].length < 2)
				// Nothing to diff here!
				continue;
			int minWidth = Math.min(width, keys[i].length);
			for (int j = 0; j < minWidth; j++) {
				IArtifactKey key = keys[i][j];
				boolean isArchive = key.getClassifier().equals("plugin"); //$NON-NLS-1$
				String proposedStrategy = isArchive && !nosar ? JBPATCH_STEP_ZIP_ID : JBPATCH_STEP_ID;
				optimize(keys[i], key, proposedStrategy);
			}
		}
		System.out.println("Done.");

	}

	private void optimize(IArtifactKey[] keys, IArtifactKey key, String proposedStrategy) throws OutOfMemoryError {
		boolean retry;
		do {
			retry = false;
			try {
				IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
				IArtifactDescriptor complete = null;
				for (int k = 0; k < descriptors.length; k++) {
					IArtifactDescriptor descriptor = descriptors[k];
					if (isCanonical(descriptor))
						complete = descriptor;
					else if (isOptimized(descriptor, proposedStrategy)) {
						proposedStrategy = null;
						break;
					}
				}
				if (proposedStrategy != null && complete != null)
					optimize(complete, proposedStrategy, keys);
			} catch (OutOfMemoryError e) {
				if (JBPATCH_STEP_ID.equals(proposedStrategy))
					throw e;
				proposedStrategy = JBPATCH_STEP_ID;
				System.out.println("Retry with " + proposedStrategy);
				retry = true;
			}
		} while (retry);
	}

	/**
	 * This method retrieves a list of list of IArtifactKeys. The artifact keys in the
	 * list of artifact keys are all ´strongly related´ to each other such that are  
	 * equal but not considering the versions. This list is sorted such that the 
	 * newer versions are first in the list.<p>
	 * With AK(c-v) : AK - artifact key, c - artifact id, v - artifact version
	 * the result is than, e.g.
	 * <pre><code>
	 * [
	 * [ AK(x,2.0) AK(x,1.5) AK(x,1.1) ]
	 * [ AK(y,2.0) AK(y,1.9) ]
	 * [ AK(z,2.0) AK(z,1.5) AK(z,1.3) AK(z,1.0) ]
	 * ]
	 * </code></pre>  
	 * @param artifactKeys
	 * @return the sorted artifact keys
	 */
	private IArtifactKey[][] getSortedRelatedArtifactKeys(IQueryResult<IArtifactKey> artifactKeys) {
		Map<IArtifactKey, List<IArtifactKey>> map = new HashMap<IArtifactKey, List<IArtifactKey>>();
		for (Iterator<IArtifactKey> iter = artifactKeys.iterator(); iter.hasNext();) {
			IArtifactKey nxt = iter.next();
			IArtifactKey freeKey = new VersionlessArtifactKey(nxt);
			List<IArtifactKey> values = map.get(freeKey);
			if (values == null) {
				values = new ArrayList<IArtifactKey>();
				map.put(freeKey, values);
			}
			values.add(nxt);
		}
		IArtifactKey[][] lists = new IArtifactKey[map.size()][];
		int i = 0;
		for (List<IArtifactKey> artifactKeyList : map.values()) {
			IArtifactKey[] relatedArtifactKeys = artifactKeyList.toArray(new IArtifactKey[artifactKeyList.size()]);
			Arrays.sort(relatedArtifactKeys, ARTIFACT_KEY_VERSION_COMPARATOR);
			lists[i++] = relatedArtifactKeys;
		}
		int candidates = 0;
		for (int ii = 0; ii < lists.length; ii++) {
			for (int jj = 0; jj < lists[ii].length; jj++) {
				System.out.println(lists[ii][jj] + ", ");
			}
			System.out.println("");
			if (lists[ii].length > 1)
				candidates++;
		}
		System.out.println("Candidates found: " + candidates);
		return lists;
	}

	private void optimize(IArtifactDescriptor complete, String strategy, IArtifactKey[] relatedArtifactKeys) {
		System.out.println("Optimizing " + complete);

		IArtifactDescriptor[] descriptors = getSortedCompletePredecessors(complete.getArtifactKey(), relatedArtifactKeys);

		int minDepth = Math.min(depth, descriptors.length);
		for (int i = 0; i < minDepth; i++) {

			System.out.println("\t with " + strategy + " against " + descriptors[i].getArtifactKey());
			String predecessorData = descriptors[i].getArtifactKey().toExternalForm();
			ArtifactDescriptor newDescriptor = new ArtifactDescriptor(complete);
			IProcessingStepDescriptor patchStep = new ProcessingStepDescriptor(strategy, predecessorData, true);
			IProcessingStepDescriptor[] steps = new IProcessingStepDescriptor[] {patchStep};
			newDescriptor.setProcessingSteps(steps);
			newDescriptor.setProperty(IArtifactDescriptor.FORMAT, strategy);
			OutputStream repositoryStream = null;
			try {
				repositoryStream = repository.getOutputStream(newDescriptor);

				// Add in all the processing steps needed to optimize (e.g., pack200, ...)
				ProcessingStep diffStep = getProcessingStep(strategy);
				diffStep.initialize(repository.getProvisioningAgent(), patchStep, newDescriptor);
				ProcessingStepHandler handler = new ProcessingStepHandler();
				OutputStream destination = handler.link(new ProcessingStep[] {diffStep}, repositoryStream, null);

				// Do the actual work by asking the repo to get the artifact and put it in the destination.
				IStatus status = repository.getArtifact(complete, destination, new NullProgressMonitor());
				if (!status.isOK()) {
					System.out.println("Getting the artifact is not ok."); //$NON-NLS-1$
					System.out.println(status);
				}
			} catch (ProvisionException e) {
				System.out.println("Skipping optimization of: " + descriptors[i].getArtifactKey()); //$NON-NLS-1$
				System.out.println(e.getMessage());
				e.printStackTrace();
			} finally {
				if (repositoryStream != null)
					try {
						repositoryStream.close();
						IStatus status = ProcessingStepHandler.checkStatus(repositoryStream);
						if (!status.isOK()) {
							System.out.println("Skipping optimization of: " + descriptors[i].getArtifactKey()); //$NON-NLS-1$
							System.out.println(status.toString());
						}
					} catch (IOException e) {
						System.out.println("Skipping optimization of: " + descriptors[i].getArtifactKey()); //$NON-NLS-1$
						System.out.println(e.getMessage());
						e.printStackTrace();
					}
			}
		}
	}

	private ProcessingStep getProcessingStep(String strategy) {
		if (strategy.equals(JBPATCH_STEP_ID))
			return new JBDiffStep(repository);
		return new JBDiffZipStep(repository);
	}

	private IArtifactDescriptor[] getSortedCompletePredecessors(IArtifactKey artifactKey, IArtifactKey[] relatedArtifactKeys) {
		// get all artifact keys
		List<IArtifactDescriptor> completeDescriptors = new ArrayList<IArtifactDescriptor>(relatedArtifactKeys.length);
		for (int i = 0; i < relatedArtifactKeys.length; i++) {
			// if we find ´our self´ skip
			if (relatedArtifactKeys[i].equals(artifactKey))
				continue;
			// look for a complete artifact descriptor of the current key  
			IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(relatedArtifactKeys[i]);
			for (int j = 0; j < descriptors.length; j++) {
				if (isCanonical(descriptors[j])) {
					completeDescriptors.add(descriptors[j]);
					break;
				}
			}
		}

		IArtifactDescriptor[] completeSortedDescriptors = completeDescriptors.toArray(new IArtifactDescriptor[completeDescriptors.size()]);
		// Sort, so to allow a depth lookup!
		Arrays.sort(completeSortedDescriptors, ARTIFACT_DESCRIPTOR_VERSION_COMPARATOR);
		return completeSortedDescriptors;
	}

	private boolean isOptimized(IArtifactDescriptor descriptor, String stepId) {
		if (descriptor.getProcessingSteps().length != 1)
			return false;
		return stepId.equals(descriptor.getProcessingSteps()[0].getProcessorId());
	}

	private boolean isCanonical(IArtifactDescriptor descriptor) {
		// TODO length != 0 is not necessarily an indicator for not being complete!   
		return descriptor.getProcessingSteps().length == 0;
	}

	static final class ArtifactDescriptorVersionComparator implements Comparator<IArtifactDescriptor> {
		public int compare(IArtifactDescriptor artifactDescriptor0, IArtifactDescriptor artifactDescriptor1) {
			return -1 * artifactDescriptor0.getArtifactKey().getVersion().compareTo(artifactDescriptor1.getArtifactKey().getVersion());
		}
	}

	static final class ArtifactKeyVersionComparator implements Comparator<IArtifactKey> {
		public int compare(IArtifactKey artifactKey0, IArtifactKey artifactKey1) {
			return -1 * artifactKey0.getVersion().compareTo(artifactKey1.getVersion());
		}
	}
}
