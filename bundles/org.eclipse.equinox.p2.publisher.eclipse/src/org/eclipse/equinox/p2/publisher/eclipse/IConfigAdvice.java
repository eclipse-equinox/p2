/*******************************************************************************
 * Copyright (c) 2008, 2014 Code 9 and others.
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 *   Code 9 - initial API and implementation
 *   Rapicorp Corporation - ongoing enhancements
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher.eclipse;

import java.util.Map;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;

public interface IConfigAdvice extends IPublisherAdvice {

	public BundleInfo[] getBundles();

	public Map<String, String> getProperties();

}
