/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *  	Compeople AG (Stefan Liebig) - various ongoing maintenance
 *   	Genuitec LLC - various bug fixes
 *      Sonatype, Inc. - transport split
 *      Yatta Solutions GmbH - bug 457619
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.osgi.util.NLS;

/**
 * A request to mirror (copy) an artifact into a given destination artifact repository.
 */
public class MirrorRequest extends ArtifactRequest {

	/**
	 * A Status code that represents an error while processing the artifact. This error is not
	 * related to transport, but rather a problem with the processing step.
	 */
	public static final int ARTIFACT_PROCESSING_ERROR = 2;

	/**
	 * Maximum number of times a request for a single artifact should be tried
	 */
	private static final int MAX_RETRY_REQUEST = 200;

	/**
	 * The name of a repository property on an artifact repository, indicating the base URI
	 * to be used for reporting download statistics.
	 */
	private static final String PROP_STATS_URI = "p2.statsURI"; //$NON-NLS-1$

	/**
	 * The name of a property on an artifact descriptor, indicating the relative download URI
	 * to be used to report download statistics for that artifact. The value of this property,
	 * if present, is appended to the {@link #PROP_STATS_URI} to create the full URI
	 * for reporting download statistics for that artifact.
	 */
	private static final String PROP_DOWNLOAD_STATS = "download.stats"; //$NON-NLS-1$
	/**
	 * The additional parameters for downloading statistics 
	 */
	private String downloadStatsParamters;

	protected final IArtifactRepository target;

	private final Map<String, String> targetDescriptorProperties;
	private final Map<String, String> targetRepositoryProperties;
	protected IArtifactDescriptor descriptor;

	public MirrorRequest(IArtifactKey key, IArtifactRepository targetRepository, Map<String, String> targetDescriptorProperties, Map<String, String> targetRepositoryProperties, Transport transport) {
		this(key, targetRepository, targetDescriptorProperties, targetRepositoryProperties, transport, null);
	}

	public MirrorRequest(IArtifactKey key, IArtifactRepository targetRepository, Map<String, String> targetDescriptorProperties, Map<String, String> targetRepositoryProperties, Transport transport, String statsParameters) {
		super(key, transport);
		target = targetRepository;
		if (targetDescriptorProperties == null || targetDescriptorProperties.isEmpty()) {
			this.targetDescriptorProperties = null;
		} else {
			this.targetDescriptorProperties = new HashMap<String, String>();
			this.targetDescriptorProperties.putAll(targetDescriptorProperties);
		}

		if (targetRepositoryProperties == null || targetRepositoryProperties.isEmpty()) {
			this.targetRepositoryProperties = null;
		} else {
			this.targetRepositoryProperties = new HashMap<String, String>();
			this.targetRepositoryProperties.putAll(targetRepositoryProperties);
		}
		this.downloadStatsParamters = statsParameters;
	}

	@Override
	protected void setSourceRepository(IArtifactRepository value)
	{
		if (value != getSourceRepository())
		{
			// This refreshes the descriptor from the new repository as it might be different.
			// If a MirrorRequest fails for a local repository the descriptor might indicate a "folder based" artifact
			// which causes the MirrorRequest to fail again with a remote repository if the descriptor is not fetched again.
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=457619
			descriptor = null;
		}
		super.setSourceRepository(value);
	}

	public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor) {
		monitor.subTask(NLS.bind(Messages.downloading, getArtifactKey().getId()));
		setSourceRepository(sourceRepository);
		// Do we already have the artifact in the target?
		if (target.contains(getArtifactKey())) {
			setResult(new Status(IStatus.OK, Activator.ID, NLS.bind(Messages.available_already_in, getArtifactKey())));
			return;
		}

		// if the request does not have a descriptor then try to fill one in by getting
		// the list of all and randomly picking one that appears to be optimized.
		IArtifactDescriptor optimized = null;
		IArtifactDescriptor canonical = null;
		if (descriptor == null) {
			IArtifactDescriptor[] descriptors = source.getArtifactDescriptors(getArtifactKey());
			if (descriptors.length > 0) {
				for (int i = 0; i < descriptors.length; i++) {
					if (descriptors[i].getProperty(IArtifactDescriptor.FORMAT) == null)
						canonical = descriptors[i];
					else if (ProcessingStepHandler.canProcess(descriptors[i]))
						optimized = descriptors[i];
				}
				boolean chooseCanonical = source.getLocation().getScheme().equals("file"); //$NON-NLS-1$
				// If the source repo is local then look for a canonical descriptor so we don't waste processing time.
				descriptor = chooseCanonical ? canonical : optimized;
				// if the descriptor is still null then we could not find our first choice of format so switch the logic.
				if (descriptor == null)
					descriptor = !chooseCanonical ? canonical : optimized;
			}
		}

		// if the descriptor is not set now then the repo does not have the requested artifact
		// TODO improve the reporting here.  It may be the case that the repo has the artifact
		// but the client does not have a processor
		if (descriptor == null) {
			setResult(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.artifact_not_found, getArtifactKey())));
			return;
		}

		IArtifactDescriptor destinationDescriptor = getDestinationDescriptor(descriptor);
		IStatus status = transfer(destinationDescriptor, descriptor, monitor);
		// if ok, cancelled or transfer has already been done with the canonical form return with status set 
		if (status.getSeverity() == IStatus.CANCEL) {
			setResult(status);
			return;
		}
		if (monitor.isCanceled()) {
			setResult(Status.CANCEL_STATUS);
			return;
		}
		if (status.isOK()) {
			setResult(status);
			return;
		}

		// failed, first remove possibly erroneously added descriptor
		if (target.contains(destinationDescriptor))
			target.removeDescriptor(destinationDescriptor);

		if (descriptor == canonical || canonical == null) {
			setResult(status);
			return;
		}

		IStatus canonicalStatus = transfer(getDestinationDescriptor(canonical), canonical, monitor);
		// To prevent the optimized transfer status severity from dominating the canonical, only merge 
		// if the canonical severity is equal to or higher than the optimized transfer severity.   
		if (canonicalStatus.getSeverity() < status.getSeverity())
			setResult(canonicalStatus);
		else
			setResult(new MultiStatus(Activator.ID, canonicalStatus.getCode() != 0 ? canonicalStatus.getCode() : status.getCode(), new IStatus[] {status, canonicalStatus}, Messages.MirrorRequest_multipleDownloadProblems, null));
	}

	private IArtifactDescriptor getDestinationDescriptor(IArtifactDescriptor sourceDescriptor) {
		// Get the descriptor to use to store the artifact
		// Since we are mirroring, ensure we clear out data from the original descriptor that may
		// not apply in the new repo location.
		// TODO this is brittle.  perhaps the repo itself should do this?  there are cases where
		// we really do need to give the repo the actual descriptor to use however...
		IArtifactDescriptor destinationDescriptor = target.createArtifactDescriptor(sourceDescriptor.getArtifactKey());
		//		destinationDescriptor.setProcessingSteps(EMPTY_STEPS);
		//		destinationDescriptor.setProperty(IArtifactDescriptor.DOWNLOAD_MD5, null);
		//		destinationDescriptor.setProperty(IArtifactDescriptor.DOWNLOAD_CONTENTTYPE, null);
		//		destinationDescriptor.setProperty(IArtifactDescriptor.FORMAT, null);
		if (targetDescriptorProperties != null && destinationDescriptor instanceof ArtifactDescriptor)
			((ArtifactDescriptor) destinationDescriptor).addProperties(targetDescriptorProperties);
		if (targetRepositoryProperties != null && destinationDescriptor instanceof SimpleArtifactDescriptor)
			((SimpleArtifactDescriptor) destinationDescriptor).addRepositoryProperties(targetRepositoryProperties);
		return destinationDescriptor;
	}

	/**
	 * Keep retrying the source repository until it reports back that it will be impossible
	 * to get the artifact from it.
	 * @param destinationDescriptor
	 * @param sourceDescriptor
	 * @param monitor
	 * @return the status of the transfer operation
	 */
	protected IStatus transfer(IArtifactDescriptor destinationDescriptor, IArtifactDescriptor sourceDescriptor, IProgressMonitor monitor) {
		MultiStatus allResults = new MultiStatus(Activator.ID, 0, NLS.bind(Messages.MirrorRequest_transferFailed, sourceDescriptor), null);
		IStatus lastResult = Status.OK_STATUS;
		// go until we get one (OK), there are no more mirrors to consider or the operation is cancelled.
		// Put a hard limit of MAX_RETRY_REQUEST so we don't end up in an infinite loop.  
		// Really, if you've tried MAX_RETRY_REQUEST times without success, it's time to give up
		// TODO Should we log that we gave up because of the retry count?
		// TODO this needs to be redone with a much better mirror management scheme.

		int counter = 0;
		do {
			lastResult = transferSingle(destinationDescriptor, sourceDescriptor, monitor);
			allResults.add(lastResult);
			if (lastResult.getException() instanceof Error)
			{
				// Error is more severe than Exception - e.g. an OutOfMemoryError should not be ignored and might hinder further processing
				throw (Error) lastResult.getException();
			}
		} while (lastResult.getSeverity() == IStatus.ERROR && lastResult.getCode() == IArtifactRepository.CODE_RETRY && counter++ < MAX_RETRY_REQUEST);
		IProvisioningEventBus bus = (IProvisioningEventBus) source.getProvisioningAgent().getService(IProvisioningEventBus.SERVICE_NAME);
		if (bus != null)
			bus.publishEvent(new MirrorEvent(source, sourceDescriptor, lastResult.isOK() ? lastResult : (allResults.getChildren().length <= 1 ? lastResult : allResults)));
		if (lastResult.isOK()) {
			collectStats(sourceDescriptor, monitor);
			return lastResult;
		} else if (allResults.getChildren().length <= 1) {
			return lastResult;
		}
		return allResults;
	}

	/**
	 * Collect download statistics, if specified by the descriptor and the source repository
	 */
	private void collectStats(IArtifactDescriptor sourceDescriptor, IProgressMonitor monitor) {
		final String statsProperty = sourceDescriptor.getProperty(PROP_DOWNLOAD_STATS);
		if (statsProperty == null)
			return;
		String statsRoot = sourceDescriptor.getRepository().getProperties().get(PROP_STATS_URI);
		if (statsRoot == null)
			return;
		URI statsURI;
		try {
			statsURI = URIUtil.append(new URI(statsRoot), statsProperty);
			if (downloadStatsParamters != null) {
				try {
					statsURI = new URI(statsURI.getScheme(), statsURI.getUserInfo(), statsURI.getHost(), statsURI.getPort(), statsURI.getPath(), statsURI.getQuery() == null ? downloadStatsParamters : downloadStatsParamters + "&" + statsURI.getQuery(), statsURI.getFragment()); //$NON-NLS-1$
				} catch (URISyntaxException e) {
					LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Unable to create download statistics due to invalid URL query: " + statsRoot + " suffix: " + statsProperty + " query: " + downloadStatsParamters)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$		
				}
			}
		} catch (URISyntaxException e) {
			LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Unable to report download statistics due to invalid URL: " + statsRoot + " suffix: " + statsProperty)); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		try {
			transport.getLastModified(statsURI, monitor);
		} catch (FileNotFoundException e) {
			//ignore because it is expected that the statistics URI doesn't represent an existing file
		} catch (Exception e) {
			LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Failure reporting download statistics to URL: " + statsURI, e)); //$NON-NLS-1$
		}
	}

	private IStatus transferSingle(IArtifactDescriptor destinationDescriptor, IArtifactDescriptor sourceDescriptor, IProgressMonitor monitor) {
		OutputStream destination;
		try {
			destination = target.getOutputStream(destinationDescriptor);
		} catch (ProvisionException e) {
			return e.getStatus();
		}

		IStatus status = null;
		Throwable priorException = null;
		// Do the actual transfer
		try {
			status = getArtifact(sourceDescriptor, destination, monitor);
			if (destination instanceof IStateful && status != null && !status.isOK()) {
				IStatus destStatus = ((IStateful) destination).getStatus();
				IStatus root = extractRootCause(status);
				Throwable e = root != null ? root.getException() : null;
				((IStateful) destination).setStatus(new MultiStatus(Activator.ID, status.getCode(), new IStatus[] {status, destStatus}, status.getMessage(), e));
			}
		} catch (RuntimeException e)
		{
			priorException = e;
			throw e;
		} catch (Error e) {
			priorException = e;
			throw e;
		} finally {
			try {
				destination.close();
			} catch (IOException e) {
				if (priorException == null) // don't mask a formerly thrown Exception/Error
				{
					if (status != null && status.getSeverity() == IStatus.ERROR && status.getCode() == IArtifactRepository.CODE_RETRY)
						return new MultiStatus(Activator.ID, status.getCode(), new IStatus[] {status}, NLS.bind(Messages.error_closing_stream, getArtifactKey(), target.getLocation()), e);
					return new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.error_closing_stream, getArtifactKey(), target.getLocation()), e);
				}
				// otherwise it is already thrown
			}
		}
		return status;
	}

	protected IStatus getArtifact(IArtifactDescriptor sourceDescriptor, OutputStream destination, IProgressMonitor monitor) {
		return getSourceRepository().getArtifact(sourceDescriptor, destination, monitor);
	}

	/**
	 * Extract the root cause. The root cause is the first severe non-MultiStatus status 
	 * containing an exception when searching depth first otherwise null.
	 * @param status
	 * @return root cause
	 */
	private static IStatus extractRootCause(IStatus status) {
		if (status == null)
			return null;
		if (!status.isMultiStatus())
			return constraintStatus(status);

		IStatus[] children = ((MultiStatus) status).getChildren();
		if (children == null)
			return constraintStatus(status);

		for (int i = 0; i < children.length; i++) {
			IStatus deeper = extractRootCause(children[i]);
			if (deeper != null)
				return deeper;
		}

		return constraintStatus(status);
	}

	private static IStatus constraintStatus(IStatus status) {
		return status.getSeverity() == IStatus.ERROR && status.getException() != null ? status : null;
	}

	public String toString() {
		return Messages.mirroring + getArtifactKey() + " into " + target; //$NON-NLS-1$
	}
}
