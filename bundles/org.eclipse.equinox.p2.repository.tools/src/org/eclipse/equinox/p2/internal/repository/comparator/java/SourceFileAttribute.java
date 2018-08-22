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

public class SourceFileAttribute extends ClassFileAttribute {

	private int sourceFileIndex;
	private char[] sourceFileName;

	/**
	 * Constructor for SourceFileAttribute.
	 * @param classFileBytes
	 * @param constantPool
	 * @param offset
	 * @throws ClassFormatException
	 */
	public SourceFileAttribute(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		this.sourceFileIndex = u2At(classFileBytes, 6, offset);
		ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.sourceFileIndex);
		if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.sourceFileName = constantPoolEntry.getUtf8Value();
	}

	/*
	 * @see ISourceAttribute#getSourceFileIndex()
	 */
	public int getSourceFileIndex() {
		return this.sourceFileIndex;
	}

	/*
	 * @see ISourceAttribute#getSourceFileName()
	 */
	public char[] getSourceFileName() {
		return this.sourceFileName;
	}

}
