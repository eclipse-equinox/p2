/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.types.DataType;

public class ElementList<T> extends DataType {

	private List<T> elements = new ArrayList<T>();

	public void addConfigured(T element) {
		elements.add(element);
	}

	public List<T> getElements() {
		return elements;
	}
}
