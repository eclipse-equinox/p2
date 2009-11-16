/******************************************************************************* 
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import java.net.URI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.tools.analyzer.RepositoryAnalyzer;

/**
 *
 */
public class RepositoryAnalyzerApplication implements IApplication {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {

		long start = System.currentTimeMillis();
		URI uri = new URI("http://download.eclipse.org/releases/galileo");
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getBundleContext(), IMetadataRepositoryManager.class.getName());
		IMetadataRepository repository = manager.loadRepository(uri, new NullProgressMonitor());
		RepositoryAnalyzer repositoryAnalyzer = new RepositoryAnalyzer(new IMetadataRepository[] {repository});
		IStatus status = repositoryAnalyzer.analyze(new NullProgressMonitor());
		IStatus[] children = status.getChildren();
		long time = (System.currentTimeMillis()) - start;
		if (status.isOK())
			System.out.println("Repository Analyzer Finished succesfuly in " + time + " ms.");
		else
			System.out.println("Repository Analyzer Finished in " + time + " ms with status with errors.");
		for (int i = 0; i < children.length; i++) {
			if (children[i].isOK())
				System.out.print("[OK] ");
			else
				System.out.print("[Error] ");
			System.out.println(children[i].getMessage());
		}
		return IApplication.EXIT_OK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		// TODO Auto-generated method stub

	}

}
