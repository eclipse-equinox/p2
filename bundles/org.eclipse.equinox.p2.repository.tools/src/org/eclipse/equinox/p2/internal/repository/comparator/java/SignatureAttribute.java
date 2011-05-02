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

public class SignatureAttribute extends ClassFileAttribute {

	private int signatureIndex;
	private char[] signature;

	SignatureAttribute(byte[] classFileBytes, ConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		final int index = u2At(classFileBytes, 6, offset);
		this.signatureIndex = index;
		ConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(index);
		if (constantPoolEntry.getKind() != ConstantPoolConstant.CONSTANT_Utf8) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.signature = constantPoolEntry.getUtf8Value();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.ISignatureAttribute#getSignatureIndex()
	 */
	public int getSignatureIndex() {
		return this.signatureIndex;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.util.ISignatureAttribute#getSignature()
	 */
	public char[] getSignature() {
		return this.signature;
	}
}
