/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.comparator.java;

public interface IModifierConstants {

	int ACC_PUBLIC = 0x0001;
	int ACC_PRIVATE = 0x0002;
	int ACC_PROTECTED = 0x0004;
	int ACC_STATIC = 0x0008;
	int ACC_FINAL = 0x0010;
	int ACC_SUPER = 0x0020;
	int ACC_SYNCHRONIZED = 0x0020;
	int ACC_VOLATILE = 0x0040;

	/**
	 * Indicates a bridge method (added in J2SE 1.5).
	 * @since 3.0
	 */
	int ACC_BRIDGE = 0x0040;
	int ACC_TRANSIENT = 0x0080;

	/**
	 * Indicates a variable arity method (added in J2SE 1.5).
	 * @since 3.0
	 */
	int ACC_VARARGS = 0x0080;
	int ACC_NATIVE = 0x0100;
	int ACC_INTERFACE = 0x0200;
	int ACC_ABSTRACT = 0x0400;
	int ACC_STRICT = 0x0800;
	/**
	 * Indicates a synthetic member.
	 * @since 3.0
	 */
	int ACC_SYNTHETIC = 0x1000;

	/**
	 * Indicates an annotation (added in J2SE 1.5).
	 * @since 3.0
	 */
	int ACC_ANNOTATION = 0x2000;

	/**
	 * Indicates an enum (added in J2SE 1.5).
	 * @since 3.0
	 */
	int ACC_ENUM = 0x4000;

	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String VERSION_1_1 = "1.1"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @category OptionValue
	 */
	public static final String VERSION_1_2 = "1.2"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String VERSION_1_3 = "1.3"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 2.0
	 * @category OptionValue
	 */
	public static final String VERSION_1_4 = "1.4"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.0
	 * @category OptionValue
	 */
	public static final String VERSION_1_5 = "1.5"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.2
	 * @category OptionValue
	 */
	public static final String VERSION_1_6 = "1.6"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.3
	 * @category OptionValue
	 */
	public static final String VERSION_1_7 = "1.7"; //$NON-NLS-1$
	/**
	 * Configurable option value: {@value}.
	 * @since 3.4
	 * @category OptionValue
	 */
	public static final String VERSION_CLDC_1_1 = "cldc1.1"; //$NON-NLS-1$
}
