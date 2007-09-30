/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.prov.engine;

import java.util.*;

public class EngineSession {
	List actions = new ArrayList();

	public void record(ITouchpointAction action) {
		actions.add(action);
	}

	public void commit() {
		actions.clear();
	}

	public void rollback() {
		for (ListIterator it = actions.listIterator(actions.size()); it.hasPrevious();) {
			ITouchpointAction action = (ITouchpointAction) it.previous();
			action.undo();
		}
	}

}
