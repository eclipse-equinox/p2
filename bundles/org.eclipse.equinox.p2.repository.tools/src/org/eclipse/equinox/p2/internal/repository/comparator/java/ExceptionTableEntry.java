/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.p2.internal.repository.comparator.java;

public class ExceptionTableEntry extends ClassFileStruct {

	private final int startPC;
	private final int endPC;
	private final int handlerPC;
	private final int catchTypeIndex;
	private char[] catchType;

	ExceptionTableEntry(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		this.startPC = u2At(classFileBytes, 0, offset);
		this.endPC = u2At(classFileBytes, 2, offset);
		this.handlerPC = u2At(classFileBytes, 4, offset);
		this.catchTypeIndex = u2At(classFileBytes, 6, offset);
		if (this.catchTypeIndex != 0) {
			ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.catchTypeIndex);
			if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Class) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.catchType = constantPoolEntry.getClassInfoName();
		}
	}

	/*
	 * @see IExceptionTableEntry#getStartPC()
	 */
	public int getStartPC() {
		return this.startPC;
	}

	/*
	 * @see IExceptionTableEntry#getEndPC()
	 */
	public int getEndPC() {
		return this.endPC;
	}

	/*
	 * @see IExceptionTableEntry#getHandlerPC()
	 */
	public int getHandlerPC() {
		return this.handlerPC;
	}

	/*
	 * @see IExceptionTableEntry#getCatchTypeIndex()
	 */
	public int getCatchTypeIndex() {
		return this.catchTypeIndex;
	}

	/*
	 * @see IExceptionTableEntry#getCatchType()
	 */
	public char[] getCatchType() {
		return this.catchType;
	}

}
