/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal;

import java.io.*;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.importexport.IUDetail;
import org.eclipse.equinox.internal.p2.importexport.P2ImportExport;
import org.eclipse.equinox.internal.p2.importexport.persistence.*;
import org.eclipse.equinox.internal.p2.persistence.XMLWriter.ProcessingInstruction;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;

public class ImportExportImpl implements P2ImportExport {

	public static final int IGNORE_LOCAL_REPOSITORY = 1;
	public static final int CANNOT_FIND_REPOSITORY = 2;

	private IProvisioningAgent agent = null;

	public void bind(IProvisioningAgent agt) {
		this.agent = agt;
	}

	public void unbind(IProvisioningAgent agt) {
		if (this.agent == agt) {
			this.agent = null;
		}
	}

	public List<IUDetail> importP2F(InputStream input) throws IOException {
		P2FParser parser = new P2FParser(Platform.getBundle(Constants.Bundle_ID).getBundleContext(), Constants.Bundle_ID);
		parser.parse(input);
		return parser.getIUs();
	}

	public IStatus exportP2F(OutputStream output, IInstallableUnit[] ius, boolean allowEntriesWithoutRepo, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.Replicator_ExportJobName, 1000);

		//Collect repos where the IUs are going to be searched
		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		URI[] uris = repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_LOCAL | IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		List<IMetadataRepository> repos = new ArrayList<IMetadataRepository>(uris.length);
		for (URI uri : uris) {
			try {
				IMetadataRepository repo = repoManager.loadRepository(uri, subMonitor.newChild(500 / uris.length, SubMonitor.SUPPRESS_ALL_LABELS));
				repos.add(repo);
			} catch (ProvisionException e) {
				// ignore
			}
		}
		subMonitor.setWorkRemaining(500);
		List<IUDetail> rootsToExport = new ArrayList<IUDetail>(ius.length);
		SubMonitor sub2 = subMonitor.newChild(450, SubMonitor.SUPPRESS_ALL_LABELS);
		sub2.setWorkRemaining(ius.length * 100);
		MultiStatus queryRepoResult = new MultiStatus(Constants.Bundle_ID, 0, null, null);
		for (IInstallableUnit iu : ius) {
			List<URI> referredRepos = new ArrayList<URI>(1);
			if (sub2.isCanceled())
				throw new OperationCanceledException();
			SubMonitor sub3 = sub2.newChild(100);
			sub3.setWorkRemaining(repos.size() * 100);

			//Search for repo matching the given IU
			for (IMetadataRepository repo : repos) {
				IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createIUQuery(iu.getId(), new VersionRange(iu.getVersion(), true, null, true)), sub3.newChild(100));
				if (!result.isEmpty())
					referredRepos.add(repo.getLocation());
			}
			sub3.setWorkRemaining(1).worked(1);

			//Create object representing given IU
			if (referredRepos.size() != 0 || (referredRepos.size() == 0 && allowEntriesWithoutRepo)) {
				IUDetail iuToExport = new IUDetail(iu, referredRepos);
				rootsToExport.add(iuToExport);
			} else {
				if (isContainedInLocalRepo(iu))
					queryRepoResult.add(new Status(IStatus.INFO, Constants.Bundle_ID, IGNORE_LOCAL_REPOSITORY, NLS.bind(Messages.Replicator_InstallFromLocal, iu.getProperty(IInstallableUnit.PROP_NAME, Locale.getDefault().toString())), null));
				else
					queryRepoResult.add(new Status(IStatus.WARNING, Constants.Bundle_ID, CANNOT_FIND_REPOSITORY, NLS.bind(Messages.Replicator_NotFoundInRepository, iu.getProperty(IInstallableUnit.PROP_NAME, Locale.getDefault().toString())), null));
			}
		}
		subMonitor.setWorkRemaining(50);
		//Serialize
		IStatus status = exportP2F(output, rootsToExport, subMonitor);
		if (status.isOK() && queryRepoResult.isOK())
			return status;
		MultiStatus rt = new MultiStatus(Constants.Bundle_ID, 0, new IStatus[] {queryRepoResult, status}, null, null);
		return rt;
	}

	private boolean isContainedInLocalRepo(IInstallableUnit iu) {
		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		URI[] uris = repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_LOCAL | IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		for (URI uri : uris) {
			try {
				IMetadataRepository repo = repoManager.loadRepository(uri, null);
				if (!repo.query(QueryUtil.createIUQuery(iu.getId(), new VersionRange(iu.getVersion(), true, null, true)), null).isEmpty())
					return true;
			} catch (ProvisionException e) {
				// ignore
			}
		}
		return false;
	}

	public IStatus exportP2F(OutputStream output, List<IUDetail> ius, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		SubMonitor sub = SubMonitor.convert(monitor, Messages.Replicator_SaveJobName, 100);
		if (sub.isCanceled())
			throw new OperationCanceledException();
		try {
			P2FWriter writer = new P2FWriter(output, new ProcessingInstruction[] {ProcessingInstruction.makeTargetVersionInstruction(P2FConstants.P2F_ELEMENT, P2FConstants.CURRENT_VERSION)});
			writer.write(ius);
			return Status.OK_STATUS;
		} catch (UnsupportedEncodingException e) {
			return new Status(IStatus.ERROR, Constants.Bundle_ID, e.getMessage(), e);
		} finally {
			sub.worked(100);
		}
	}
}
