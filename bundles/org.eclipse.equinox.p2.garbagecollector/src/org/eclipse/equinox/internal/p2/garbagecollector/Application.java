/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.garbagecollector;

import java.util.Map;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;

/**
 * @since 1.0
 */
public class Application implements IApplication {

	private String profileId;
	private IProvisioningAgent agent;

	/*
	 * Return the profile with the given id, or null if not found.
	 */
	private IProfile getProfile(String id) {
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		return profileRegistry == null ? null : profileRegistry.getProfile(id);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		Map<?, ?> allArgs = context.getArguments();
		String[] args = (String[]) allArgs.get(IApplicationContext.APPLICATION_ARGS);
		processArguments(args);
		// if the user didn't give us a profile id, then use the default SDK one
		if (profileId == null)
			profileId = IProfileRegistry.SELF;

		initializeServices();
		IProfile profile = getProfile(profileId);
		if (profile == null)
			throw new IllegalArgumentException("\"" + profileId + "\" is not a valid profile identifier."); //$NON-NLS-1$//$NON-NLS-2$
		GarbageCollector gc = (GarbageCollector) agent.getService(GarbageCollector.SERVICE_NAME);
		gc.runGC(profile);
		agent.stop();
		return null;
	}

	private void initializeServices() throws ProvisionException {
		IProvisioningAgentProvider provider = GCActivator.getService(IProvisioningAgentProvider.class);
		agent = provider.createAgent(null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		// nothing to do
	}

	/*
	 * Iterate over the command-line arguments and pull out the ones which are important to us.
	 */
	public void processArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {
			String opt = args[i];
			// check for args with parameters. If we are at the last argument or if the next 
			// one has a '-' as the first character, then we can't have an arg with a parm so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;
			String arg = args[++i];
			if (opt.equalsIgnoreCase("-profile")) //$NON-NLS-1$
				profileId = arg;
		}
	}
}