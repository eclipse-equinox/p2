/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class Application implements IApplication {

	public Object start(IApplicationContext context) throws Exception {
		Object obj = System.getProperties().get(Activator.PROP_APPLICATION_STATUS);
		// if we have a non-OK status return "unlucky" 13, otherwise return the OK return code
		if (obj != null && (obj instanceof IStatus) && !((IStatus) obj).isOK())
			return Integer.valueOf(13);
		return IApplication.EXIT_OK;
	}

	public void stop() {
		//Nothing to do
	}

}
