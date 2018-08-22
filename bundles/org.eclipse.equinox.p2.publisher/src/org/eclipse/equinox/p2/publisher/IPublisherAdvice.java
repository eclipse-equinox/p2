/*******************************************************************************
 * Copyright (c) 2008, 2010 Code 9 and others.
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
 ******************************************************************************/
package org.eclipse.equinox.p2.publisher;

import org.eclipse.equinox.p2.metadata.Version;

public interface IPublisherAdvice {

	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version);

}
