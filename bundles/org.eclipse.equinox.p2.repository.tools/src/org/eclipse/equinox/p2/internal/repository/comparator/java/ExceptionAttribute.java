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

public class ExceptionAttribute extends ClassFileAttribute {
	private final int exceptionsNumber;
	private char[][] exceptionNames;
	private int[] exceptionIndexes;

	ExceptionAttribute(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.exceptionsNumber = u2At(classFileBytes, 6, offset);
		int exceptionLength = this.exceptionsNumber;
		this.exceptionNames = CharOperation.NO_CHAR_CHAR;
		this.exceptionIndexes = Utility.EMPTY_INT_ARRAY;
		if (exceptionLength != 0) {
			this.exceptionNames = new char[exceptionLength][];
			this.exceptionIndexes = new int[exceptionLength];
		}
		int readOffset = 8;
		ConstantPoolEntry constantPoolEntry;
		for (int i = 0; i < exceptionLength; i++) {
			this.exceptionIndexes[i] = u2At(classFileBytes, readOffset, offset);
			constantPoolEntry = constantPool.decodeEntry(this.exceptionIndexes[i]);
			if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Class) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.exceptionNames[i] = constantPoolEntry.getClassInfoName();
			readOffset += 2;
		}
	}

	/*
	 * @see IExceptionAttribute#getExceptionIndexes()
	 */
	public int[] getExceptionIndexes() {
		return this.exceptionIndexes;
	}

	/*
	 * @see IExceptionAttribute#getExceptionNames()
	 */
	public char[][] getExceptionNames() {
		return this.exceptionNames;
	}

	/*
	 * @see IExceptionAttribute#getExceptionsNumber()
	 */
	public int getExceptionsNumber() {
		return this.exceptionsNumber;
	}
}
