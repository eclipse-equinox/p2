/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: 
 * Sonatype, Inc. - initial implementation and ideas 
 ******************************************************************************/

package org.eclipse.equinox.p2.tests;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE})
public @interface IUDescription {
	public String content();
}
