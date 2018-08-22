/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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

package org.eclipse.equinox.p2.publisher.eclipse;

public class URLEntry {
	private String annotation;
	private String url;

	public URLEntry() {
		//empty
	}

	public URLEntry(String url, String annotation) {
		this.url = url;
		this.annotation = annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public String getURL() {
		return url;
	}
}
