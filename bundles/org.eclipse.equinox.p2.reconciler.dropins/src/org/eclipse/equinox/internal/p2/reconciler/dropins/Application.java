package org.eclipse.equinox.internal.p2.reconciler.dropins;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class Application implements IApplication {

	public Object start(IApplicationContext context) throws Exception {
		return null;
	}

	public void stop() {
		//Nothing to do
	}

}
