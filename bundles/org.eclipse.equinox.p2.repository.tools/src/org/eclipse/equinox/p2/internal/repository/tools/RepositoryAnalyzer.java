/*******************************************************************************
* Copyright (c) 2009,2010 EclipseSource and others.
 *
 * This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
* available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.tools.analyzer.IIUAnalyzer;
import org.eclipse.equinox.p2.repository.tools.analyzer.IUAnalyzer;

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
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		if (registry == null) {
			return Status.CANCEL_STATUS;
		}
		IConfigurationElement[] config = registry.getConfigurationElementsFor(IIUAnalyzer.ID);

		for (IMetadataRepository repository : repositories) {
			IQueryResult<IInstallableUnit> queryResult = repository.query(QueryUtil.createIUAnyQuery(), sub);
			SubMonitor repositoryMonitor = SubMonitor.convert(sub, IProgressMonitor.UNKNOWN);
			for (IConfigurationElement config1 : config) {
				try {
					IIUAnalyzer verifier = (IIUAnalyzer) config1.createExecutableExtension("class"); //$NON-NLS-1$
					String analyizerName = config1.getAttribute("name"); //$NON-NLS-1$
					if (verifier instanceof IUAnalyzer) {
						((IUAnalyzer) verifier).setName(analyizerName);
					}
					verifier.preAnalysis(repository);
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
						result.add(new Status(IStatus.ERROR, Activator.ID,
								"Cannot find: " + config1.getAttribute("class"))); //$NON-NLS-1$ //$NON-NLS-2$
					} else {
						e.printStackTrace();
					}
				}
			}
			repositoryMonitor.done();
		}
		sub.done();
		return result;
	}
}
