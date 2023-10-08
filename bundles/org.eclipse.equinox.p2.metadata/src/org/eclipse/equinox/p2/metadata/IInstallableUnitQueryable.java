/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.metadata;

import org.eclipse.equinox.p2.query.IQueryable;

/**
 * A {@link IQueryable} for {@link IInstallableUnit}s, this interface is manly
 * to allow adaption in a context where the generic {@link IQueryable} would not
 * be applicable as the type information can not be preserved otherwise.
 * 
 * @since 2.8
 *
 */
public interface IInstallableUnitQueryable extends IQueryable<IInstallableUnit> {

}
