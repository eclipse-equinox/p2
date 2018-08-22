/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.types.DataType;

public class ElementList<T> extends DataType {

	private List<T> elements = new ArrayList<>();

	public void addConfigured(T element) {
		elements.add(element);
	}

	public List<T> getElements() {
		return elements;
	}
}
