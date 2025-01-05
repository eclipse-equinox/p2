/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.core.spi;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * This component property type can be used to annotate a declarative service
 * component that provides an {@link IAgentServiceFactory} to provides the
 * required {@link IAgentServiceFactory#PROP_AGENT_SERVICE_NAME}.
 *
 * @since 2.13
 */
@Retention(CLASS)
@Target(TYPE)
@ComponentPropertyType
public @interface AgentServiceName {

	public static final String PREFIX_ = "p2."; //$NON-NLS-1$

	Class<?> value();

}