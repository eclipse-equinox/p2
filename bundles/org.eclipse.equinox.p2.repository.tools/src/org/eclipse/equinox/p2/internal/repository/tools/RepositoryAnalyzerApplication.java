/******************************************************************************* 
* Copyright (c) 2009, 2015 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   Red Hat Inc. - Bug 460967
******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 *
 */
public class RepositoryAnalyzerApplication implements IApplication {

	private URI uri = null;

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {

		long start = System.currentTimeMillis();
		processArguments((String[]) context.getArguments().get("application.args"));
		IProvisioningAgent agent = ServiceHelper.getService(Activator.getBundleContext(), IProvisioningAgent.class);
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
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
			if (children[i].isMultiStatus() && children[i].getChildren() != null && children[i].getChildren().length > 0) {
				IStatus[] subChildren = children[i].getChildren();
				for (int j = 0; j < subChildren.length; j++) {
					System.out.println("   " + subChildren[j].getMessage());
				}
			}
		}
		return IApplication.EXIT_OK;
	}

	private void processArguments(String[] args) throws CoreException, URISyntaxException {
		for (int i = 0; i < args.length; i++) {
			if ("-m".equals(args[i]) || "-metadataRepository".equals(args[i])) { //$NON-NLS-1$ //$NON-NLS-2$
				if (i + 1 < args.length)
					uri = new URI(args[i + 1]);
			}
		}
		validateLaunch();
	}

	private void validateLaunch() throws CoreException {
		if (uri == null)
			throw new CoreException(new Status(IStatus.ERROR, Activator.ID, "-metadataRepository <metadataURI> must be specified"));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		// TODO Auto-generated method stub

	}

}
