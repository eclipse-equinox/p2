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

	private static final String SCHEME_FILE = "file"; //$NON-NLS-1$
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

	public IStatus exportP2F(OutputStream output, IInstallableUnit[] ius, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.Replicator_ExportJobName, 1000);
		IMetadataRepositoryManager repoManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		URI[] uris = repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		Arrays.sort(uris, new Comparator<URI>() {
			public int compare(URI o1, URI o2) {
				String scheme1 = o1.getScheme();
				String scheme2 = o2.getScheme();
				if (scheme1.equals(scheme2))
					return 0;
				if (SCHEME_FILE.equals(scheme1)) {
					return 1;
				} else if (SCHEME_FILE.equals(scheme2)) {
					return -1;
				}
				return 0;
			}
		});
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
		List<IUDetail> features = new ArrayList<IUDetail>(ius.length);
		SubMonitor sub2 = subMonitor.newChild(450, SubMonitor.SUPPRESS_ALL_LABELS);
		sub2.setWorkRemaining(ius.length * 100);
		MultiStatus queryRepoResult = new MultiStatus(Constants.Bundle_ID, 0, null, null);
		for (IInstallableUnit iu : ius) {
			List<URI> referredRepos = new ArrayList<URI>(1);
			if (sub2.isCanceled())
				throw new OperationCanceledException();
			SubMonitor sub3 = sub2.newChild(100);
			sub3.setWorkRemaining(repos.size() * 100);
			for (IMetadataRepository repo : repos) {
				if (SCHEME_FILE.equals(repo.getLocation().getScheme()) && referredRepos.size() > 0)
					break;
				IQueryResult<IInstallableUnit> result = repo.query(QueryUtil.createIUQuery(iu.getId(), new VersionRange(iu.getVersion(), true, null, true)), sub3.newChild(100));
				if (!result.isEmpty())
					referredRepos.add(repo.getLocation());
			}
			sub3.setWorkRemaining(1).worked(1);
			if (referredRepos.size() == 0) {
				queryRepoResult.add(new Status(IStatus.WARNING, Constants.Bundle_ID, CANNOT_FIND_REPOSITORY, NLS.bind(Messages.Replicator_NotFoundInRepository, iu.getProperty(IInstallableUnit.PROP_NAME, Locale.getDefault().toString())), null));
			} else {
				if (SCHEME_FILE.equals(referredRepos.get(0).getScheme()))
					queryRepoResult.add(new Status(IStatus.INFO, Constants.Bundle_ID, IGNORE_LOCAL_REPOSITORY, NLS.bind(Messages.Replicator_InstallFromLocal, iu.getProperty(IInstallableUnit.PROP_NAME, Locale.getDefault().toString())), null));
				else {
					IUDetail feature = new IUDetail(iu, referredRepos);
					features.add(feature);
				}
			}
		}
		subMonitor.setWorkRemaining(50);
		IStatus status = exportP2F(output, features, subMonitor);
		if (status.isOK() && queryRepoResult.isOK())
			return status;
		MultiStatus rt = new MultiStatus(Constants.Bundle_ID, 0, new IStatus[] {queryRepoResult, status}, null, null);
		return rt;
	}

	public IStatus exportP2F(OutputStream output, List<IUDetail> features, IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		SubMonitor sub = SubMonitor.convert(monitor, Messages.Replicator_SaveJobName, 100);
		if (sub.isCanceled())
			throw new OperationCanceledException();
		try {
			P2FWriter writer = new P2FWriter(output, new ProcessingInstruction[] {ProcessingInstruction.makeTargetVersionInstruction(P2FConstants.P2F_ELEMENT, P2FConstants.CURRENT_VERSION)});
			writer.write(features);
			return Status.OK_STATUS;
		} catch (UnsupportedEncodingException e) {
			return new Status(IStatus.ERROR, Constants.Bundle_ID, e.getMessage(), e);
		} finally {
			sub.worked(100);
		}
	}
}
