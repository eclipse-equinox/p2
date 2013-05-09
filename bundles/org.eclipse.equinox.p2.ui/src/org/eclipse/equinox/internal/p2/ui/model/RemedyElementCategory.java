/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		elements = new ArrayList<RemedyIUDetail>();
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
