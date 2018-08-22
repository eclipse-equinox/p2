/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.p2.operations.RemedyIUDetail;

public class RemedyElementCategory {

	private String name;
	private List<RemedyIUDetail> elements;

	public RemedyElementCategory(String name) {
		this.name = name;
		elements = new ArrayList<>();
	}

	public List<RemedyIUDetail> getElements() {
		return elements;
	}

	public String getName() {
		return name;
	}

	public void add(RemedyIUDetail element) {
		elements.add(element);
	}

}
