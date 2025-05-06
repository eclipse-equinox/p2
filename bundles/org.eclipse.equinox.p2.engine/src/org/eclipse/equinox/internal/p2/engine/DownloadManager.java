/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *     WindRiver - https://bugs.eclipse.org/bugs/show_bug.cgi?id=227372
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.phases.Collect;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.expression.ExpressionUtil;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.osgi.util.NLS;

public class DownloadManager {
	private ProvisioningContext provContext = null;
	ArrayList<IArtifactRequest> requestsToProcess = new ArrayList<>();
	private IProvisioningAgent agent = null;


	/**
	 * This comparator sorts the repositories such that local repositories are first
	 */
	private static final Comparator<IArtifactRepository> LOCAL_FIRST_COMPARATOR = (arg0, arg1) -> DownloadManager.LOCAL_FIRST_URI_COMPARATOR.compare(arg0.getLocation(), arg1.getLocation());

	/**
	 * A pattern that will recognize a local URI also of the form jar:file:.
	 */
	private static final Pattern LOCAL_URI_PATTERN = Pattern.compile("^(file:|jar:file:)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	/**
	 * This comparator sorts the repository URIs such that local URIs are first.
	 */
	public static final Comparator<URI> LOCAL_FIRST_URI_COMPARATOR = (arg0, arg1) -> {
		boolean isLocal0 = LOCAL_URI_PATTERN.matcher(arg0.toString()).find();
		boolean isLocal1 = LOCAL_URI_PATTERN.matcher(arg1.toString()).find();
		if (isLocal0 != isLocal1) {
			return isLocal0 ? -1 : 1;
		}
		return 0;
	};

	private final Set<IInstallableUnit> ius;

	public DownloadManager(ProvisioningContext context, IProvisioningAgent agent) {
		this(context, Set.of(), agent);
	}

	public DownloadManager(ProvisioningContext context, Set<IInstallableUnit> ius, IProvisioningAgent agent) {
		provContext = context;
		this.ius = ius;
		this.agent = agent;
	}

	/*
	 * Add the given artifact to the download queue. When it
	 * is downloaded, put it in the specified location.
	 */
	public void add(IArtifactRequest toAdd) {
		Assert.isNotNull(toAdd);
		requestsToProcess.add(toAdd);
	}

	public void add(IArtifactRequest[] toAdd) {
		Assert.isNotNull(toAdd);
		for (IArtifactRequest element : toAdd) {
			add(element);
		}
	}

	private void filterUnfetched() {
		for (Iterator<IArtifactRequest> iterator = requestsToProcess.iterator(); iterator.hasNext();) {
			IArtifactRequest request = iterator.next();
			if (request.getResult() != null && request.getResult().isOK()) {
				iterator.remove();
			}
		}
	}

	/*
	 * Start the downloads. Return a status message indicating success or failure of
	 * the overall operation
	 */
	public IStatus start(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.download_artifact, 1000);
		if (requestsToProcess.isEmpty()) {
			return Status.OK_STATUS;
		}

		if (provContext == null) {
			provContext = new ProvisioningContext(agent);
		}

		IArtifactRepository[] repositories = getArtifactRepositories(subMonitor);
		if (repositories.length == 0) {
			return new Status(IStatus.ERROR, EngineActivator.ID, Messages.download_no_repository,
					new Exception(Collect.NO_ARTIFACT_REPOSITORIES_AVAILABLE));
		}
		fetch(repositories, subMonitor.newChild(500));
		return overallStatus(monitor, repositories);
	}

	/**
	 * @return artifact repositories sorted according to LOCAL_FIRST_COMPARATOR
	 */
	private IArtifactRepository[] getArtifactRepositories(SubMonitor subMonitor) {
		IQuery<IArtifactRepository> queryArtifactRepositories = new ExpressionMatchQuery<>(IArtifactRepository.class, ExpressionUtil.TRUE_EXPRESSION);
		IQueryable<IArtifactRepository> artifactRepositories = provContext.getArtifactRepositories(subMonitor.newChild(250));
		IQueryResult<IArtifactRepository> queryResult = artifactRepositories.query(queryArtifactRepositories, subMonitor.newChild(250));
		IArtifactRepository[] repositories = queryResult.toArray(IArtifactRepository.class);

		// Although we get a sorted list back from the ProvisioningContext above, it
		// gets unsorted when we convert the queryable into an array so we must re-sort it.
		// See https://bugs.eclipse.org/335153.
		Arrays.sort(repositories, LOCAL_FIRST_COMPARATOR);

		return repositories;
	}

	private void fetch(IArtifactRepository[] repositories, IProgressMonitor mon) {
		SubMonitor monitor = SubMonitor.convert(mon, requestsToProcess.size());
		for (int i = 0; i < repositories.length && !requestsToProcess.isEmpty() && !monitor.isCanceled(); i++) {
			IArtifactRequest[] requests = getRequestsForRepository(repositories[i]);
			publishDownloadEvent(new CollectEvent(CollectEvent.TYPE_REPOSITORY_START, repositories[i], provContext, requests));
			IStatus dlStatus = repositories[i].getArtifacts(requests, monitor.newChild(requests.length));
			publishDownloadEvent(new CollectEvent(CollectEvent.TYPE_REPOSITORY_END, repositories[i], provContext, requests));
			if (dlStatus.getSeverity() == IStatus.CANCEL) {
				return;
			}
			filterUnfetched();
			monitor.setWorkRemaining(requestsToProcess.size());
		}
	}

	private void publishDownloadEvent(CollectEvent event) {
		IProvisioningEventBus bus = agent.getService(IProvisioningEventBus.class);
		if (bus != null) {
			bus.publishEvent(event);
		}
	}

	private IArtifactRequest[] getRequestsForRepository(IArtifactRepository repository) {
		ArrayList<IArtifactRequest> applicable = new ArrayList<>();
		for (IArtifactRequest request : requestsToProcess) {
			if (repository.contains(request.getArtifactKey())) {
				applicable.add(request);
			}
		}
		return applicable.toArray(new IArtifactRequest[applicable.size()]);
	}

	private IStatus overallStatus(IProgressMonitor monitor, IArtifactRepository[] repositories) {
		if (monitor != null && monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}

		if (requestsToProcess.size() == 0) {
			return Status.OK_STATUS;
		}

		MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		for (IArtifactRequest request : requestsToProcess) {
			IStatus failed = request.getResult();
			if (failed != null && !failed.isOK()) {
				result.add(failed);
				IArtifactKey key = request.getArtifactKey();
				IInstallableUnit unit = getUnit(key);
				if (unit != null) {
					String dependencyPath = computeDependency(unit);
					result.add(Status.error(NLS.bind(Messages.DownloadManager_cant_find_artifact,
							request.getArtifactKey().toString(), dependencyPath, Arrays.stream(repositories).map(repo -> repo.getLocation()).filter(Objects::nonNull)
									.map(URI::toString).collect(Collectors.joining(System.lineSeparator(),
											System.lineSeparator(), ""))))); //$NON-NLS-1$
				}
			}
		}
		return result;
	}

	private IInstallableUnit getUnit(IArtifactKey artifactKey) {
		if (ius != null) {
			for (IInstallableUnit unit : ius) {
				if (unit.getArtifacts().contains(artifactKey)) {
					return unit;
				}
			}
		}
		return null;
	}

	private String computeDependency(IInstallableUnit unit) {
		List<String> requiredBy = new ArrayList<>();
		requiredBy.add(toIdAndVersion(unit));
		if (ius != null) {
			for (IInstallableUnit other : ius) {
				List<IRequirement> requirement = other.getRequirements().stream().filter(req -> unit.satisfies(req))
						.toList();
				if (!requirement.isEmpty()) {
					requiredBy.add(toIdAndVersion(other));
				}
			}
		}
		return requiredBy.stream().collect(Collectors.joining(", ")); //$NON-NLS-1$
	}

	private static String toIdAndVersion(IInstallableUnit unit) {
		return String.format("%s[%s]", unit.getId(), unit.getVersion()); //$NON-NLS-1$
	}
}
