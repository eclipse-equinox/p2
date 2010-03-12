/******************************************************************************* 
* Copyright (c) 2009,2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import org.eclipse.equinox.p2.repository.tools.analyzer.IIUAnalyzer;
import org.eclipse.equinox.p2.repository.tools.analyzer.IUAnalyzer;

import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * @since 2.0
 *
 */
public class RepositoryAnalyzer {

	private final IMetadataRepository[] repositories;

	public RepositoryAnalyzer(IMetadataRepository[] repositories) {
		this.repositories = repositories;
	}

	public IStatus analyze(IProgressMonitor monitor) {
		MultiStatus result = new MultiStatus(Activator.ID, IStatus.OK, null, null);

		SubMonitor sub = SubMonitor.convert(monitor, repositories.length * 2);
		IConfigurationElement[] config = RegistryFactory.getRegistry().getConfigurationElementsFor(IIUAnalyzer.ID);

		for (int i = 0; i < repositories.length; i++) {
			IQueryResult<IInstallableUnit> queryResult = repositories[i].query(QueryUtil.createIUAnyQuery(), sub);

			SubMonitor repositoryMonitor = SubMonitor.convert(sub, IProgressMonitor.UNKNOWN);
			for (int j = 0; j < config.length; j++) {
				try {
					IIUAnalyzer verifier = (IIUAnalyzer) config[j].createExecutableExtension("class"); //$NON-NLS-1$
					String analyizerName = config[j].getAttribute("name"); //$NON-NLS-1$
					if (verifier instanceof IUAnalyzer) {
						((IUAnalyzer) verifier).setName(analyizerName);
					}
					verifier.preAnalysis(repositories[i]);
					Iterator<IInstallableUnit> iter = queryResult.iterator();
					while (iter.hasNext()) {
						IInstallableUnit iu = iter.next();
						verifier.analyzeIU(iu);
					}
					IStatus postAnalysisResult = verifier.postAnalysis();
					if (postAnalysisResult == null)
						postAnalysisResult = new Status(IStatus.OK, Activator.ID, analyizerName);
					if (postAnalysisResult.isOK() && !postAnalysisResult.isMultiStatus())
						postAnalysisResult = new Status(IStatus.OK, Activator.ID, analyizerName);
					result.add(postAnalysisResult);
				} catch (CoreException e) {
					if (e.getCause() instanceof ClassNotFoundException) {
						result.add(new Status(IStatus.ERROR, Activator.ID, "Cannot find: " + config[j].getAttribute("class")));
					} else
						e.printStackTrace();
				}
			}
			repositoryMonitor.done();
		}
		sub.done();
		return result;
	}
}
