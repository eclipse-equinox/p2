/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.mirror;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.processors.md5.MD5ArtifactComparator;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.Messages;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceReference;

/**
 * An application that performs mirroring of artifacts between repositories.
 */
public class MirrorApplication implements IApplication {

	private URI sourceLocation;
	private URI destinationLocation;
	private URI baselineLocation;
	private IArtifactRepository source;
	private IArtifactRepository destination;
	private IArtifactRepository baseline;
	private boolean append = true;
	private boolean raw = false;
	private boolean failOnError = true;
	private boolean validate = false;
	private boolean verbose = false;
	private boolean sourceLoaded = false;
	private boolean destinationLoaded = false;
	private boolean baselineLoaded = false;
	private boolean compare = false;
	private String comparatorID = MD5ArtifactComparator.MD5_COMPARATOR_ID; //use MD5 as default
	private String destinationName;
	private IArtifactMirrorLog mirrorLog;
	private IArtifactMirrorLog comparatorLog;
	private IProvisioningAgent agent;
	private ServiceReference agentRef;

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		try {
			Map args = context.getArguments();
			initializeFromArguments((String[]) args.get(IApplicationContext.APPLICATION_ARGS));
			setupAgent();
			setupRepositories();

			Mirroring mirroring = new Mirroring(source, destination, raw);
			mirroring.setCompare(compare);
			mirroring.setComparatorId(comparatorID);
			mirroring.setBaseline(baseline);
			mirroring.setValidate(validate);
			if (comparatorLog != null)
				mirroring.setComparatorLog(comparatorLog);

			IStatus result = mirroring.run(failOnError, verbose);
			if (!result.isOK()) {
				//only noteworthy statuses should be resulted from mirroring.run
				if (result.matches(IStatus.INFO))
					System.err.println("Mirroring completed. Please check log file for more information."); //$NON-NLS-1$
				else
					System.err.println("Mirroring completed with warnings and/or errors. Please check log file for more information."); //$NON-NLS-1$
				log(result);
			}
			return IApplication.EXIT_OK;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw e;
		} finally {
			cleanup();
		}
	}

	private void setupAgent() throws ProvisionException {
		agentRef = Activator.getContext().getServiceReference(IProvisioningAgent.SERVICE_NAME);
		if (agentRef != null) {
			agent = (IProvisioningAgent) Activator.getContext().getService(agentRef);
			if (agent != null)
				return;
		}
		ServiceReference providerRef = Activator.getContext().getServiceReference(IProvisioningAgentProvider.SERVICE_NAME);
		if (providerRef == null)
			throw new RuntimeException("No provisioning agent provider is available"); //$NON-NLS-1$
		IProvisioningAgentProvider provider = (IProvisioningAgentProvider) Activator.getContext().getService(providerRef);
		if (provider == null)
			throw new RuntimeException("No provisioning agent provider is available"); //$NON-NLS-1$
		//obtain agent for currently running system
		agent = provider.createAgent(null);
		Activator.getContext().ungetService(providerRef);
	}

	/*
	 * Return the artifact repository manager. We need to check the service here
	 * as well as creating one manually in case we are running a stand-alone application
	 * in which no one has registered a manager yet.
	 */
	private IArtifactRepositoryManager getManager() {
		return (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
	}

	private void setupRepositories() throws ProvisionException {
		if (destinationLocation == null || sourceLocation == null)
			throw new IllegalStateException(Messages.exception_needSourceDestination);

		//Check if repositories are already loaded
		sourceLoaded = getManager().contains(sourceLocation);
		destinationLoaded = getManager().contains(destinationLocation);

		//must execute before initializeDestination is called
		source = getManager().loadRepository(sourceLocation, 0, null);
		destination = initializeDestination();

		if (baselineLocation != null) {
			baselineLoaded = getManager().contains(baselineLocation);
			try {
				baseline = getManager().loadRepository(baselineLocation, 0, null);
			} catch (ProvisionException e) {
				// catch the exception and log it. we will continue without doing a baseline comparison
				System.err.println("Error occurred while trying to load baseline repository."); //$NON-NLS-1$
				e.printStackTrace();
			}
		}
	}

	private IArtifactRepository initializeDestination() throws ProvisionException {
		try {
			IArtifactRepository repository = getManager().loadRepository(destinationLocation, IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, null);
			if (repository != null && repository.isModifiable()) {
				if (destinationName != null)
					repository.setName(destinationName);
				if (!append)
					repository.removeAll();
				return repository;
			}
		} catch (ProvisionException e) {
			//fall through and create a new repository below
		}
		//This code assumes source has been successfully loaded before this point
		//No existing repository; create a new repository at destinationLocation but with source's attributes.
		// TODO for now create a Simple repo by default.
		return (IArtifactRepository) RepositoryHelper.validDestinationRepository(getManager().createRepository(destinationLocation, destinationName == null ? source.getName() : destinationName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, source.getProperties()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		if (agentRef != null) {
			Activator.getContext().ungetService(agentRef);
			agentRef = null;
		}
	}

	public void initializeFromArguments(String[] args) throws Exception {
		if (args == null)
			return;

		String comparatorLogLocation = null;
		String mirrorLogLocation = null;

		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)
			if (args[i].equalsIgnoreCase("-raw")) //$NON-NLS-1$
				raw = true;
			if (args[i].equalsIgnoreCase("-ignoreErrors")) //$NON-NLS-1$
				failOnError = false;
			if (args[i].equalsIgnoreCase("-verbose")) //$NON-NLS-1$
				verbose = true;
			if (args[i].equalsIgnoreCase("-compare")) //$NON-NLS-1$
				compare = true;
			if (args[i].equalsIgnoreCase("-validate")) //$NON-NLS-1$
				validate = true;

			// check for args with parameters. If we are at the last argument or 
			// if the next one has a '-' as the first character, then we can't have 
			// an arg with a param so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];

			if (args[i - 1].equalsIgnoreCase("-comparator")) //$NON-NLS-1$
				comparatorID = arg;
			if (args[i - 1].equalsIgnoreCase("-comparatorLog")) //$NON-NLS-1$
				comparatorLogLocation = arg;
			if (args[i - 1].equalsIgnoreCase("-destinationName")) //$NON-NLS-1$
				destinationName = arg;
			if (args[i - 1].equalsIgnoreCase("-writeMode")) //$NON-NLS-1$
				if (args[i].equalsIgnoreCase("clean")) //$NON-NLS-1$
					append = false;
			if (args[i - 1].equalsIgnoreCase("-log")) //$NON-NLS-1$
				mirrorLogLocation = arg;

			try {
				if (args[i - 1].equalsIgnoreCase("-source")) //$NON-NLS-1$
					sourceLocation = RepositoryHelper.localRepoURIHelper(URIUtil.fromString(arg));
				if (args[i - 1].equalsIgnoreCase("-destination")) //$NON-NLS-1$
					destinationLocation = RepositoryHelper.localRepoURIHelper(URIUtil.fromString(arg));
				if (args[i - 1].equalsIgnoreCase("-compareAgainst")) { //$NON-NLS-1$
					baselineLocation = RepositoryHelper.localRepoURIHelper(URIUtil.fromString(arg));
					compare = true;
				}
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(NLS.bind(Messages.exception_malformedRepoURI, arg));
			}
		}
		// Create logs
		if (mirrorLogLocation != null)
			mirrorLog = getLog(mirrorLogLocation, "p2.artifact.mirror"); //$NON-NLS-1$
		if (comparatorLogLocation != null && comparatorID != null)
			comparatorLog = getLog(comparatorLogLocation, comparatorID);
	}

	public void setLog(IArtifactMirrorLog log) {
		mirrorLog = log;
	}

	/*
	 * Create a MirrorLog based on a filename
	 */
	private IArtifactMirrorLog getLog(String location, String root) {
		if (location.toLowerCase().endsWith(".xml")) //$NON-NLS-1$
			return new XMLMirrorLog(location, verbose ? IStatus.INFO : IStatus.ERROR, root);
		return new FileMirrorLog(location, verbose ? IStatus.INFO : IStatus.ERROR, root);
	}

	/*
	 * Log the result of mirroring
	 */
	private void log(IStatus status) {
		if (mirrorLog == null) {
			FrameworkLog log = (FrameworkLog) ServiceHelper.getService(Activator.getContext(), FrameworkLog.class.getName());
			if (log != null)
				System.err.println("Log file location: " + log.getFile()); //$NON-NLS-1$
			LogHelper.log(status);
		} else
			mirrorLog.log(status);
	}

	/*
	 * Cleanup
	 */
	private void cleanup() {
		//if the repository was not already loaded before the mirror application started, close it.
		if (!sourceLoaded && sourceLocation != null)
			getManager().removeRepository(sourceLocation);
		if (!destinationLoaded && destinationLocation != null)
			getManager().removeRepository(destinationLocation);
		if (baselineLocation != null && !baselineLoaded)
			getManager().removeRepository(baselineLocation);

		// Close logs
		if (mirrorLog != null)
			mirrorLog.close();
		if (comparatorLog != null)
			comparatorLog.close();
	}
}
