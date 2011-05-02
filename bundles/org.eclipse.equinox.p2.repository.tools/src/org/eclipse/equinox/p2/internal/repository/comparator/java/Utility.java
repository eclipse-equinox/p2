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

import java.io.*;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utility {
	public static final int[] EMPTY_INT_ARRAY = new int[0];
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final int DEFAULT_READING_SIZE = 8192;
	private static final char[] BOOLEAN = "boolean".toCharArray(); //$NON-NLS-1$
	private static final char[] BYTE = "byte".toCharArray(); //$NON-NLS-1$
	private static final char[] CHAR = "char".toCharArray(); //$NON-NLS-1$
	private static final char[] DOUBLE = "double".toCharArray(); //$NON-NLS-1$
	private static final char[] FLOAT = "float".toCharArray(); //$NON-NLS-1$
	private static final char[] INT = "int".toCharArray(); //$NON-NLS-1$
	private static final char[] LONG = "long".toCharArray(); //$NON-NLS-1$
	private static final char[] SHORT = "short".toCharArray(); //$NON-NLS-1$
	private static final char[] VOID = "void".toCharArray(); //$NON-NLS-1$
	private static final char[] INIT = "<init>".toCharArray(); //$NON-NLS-1$

	/**
	 * Returns the contents of the given zip entry as a byte array.
	 * @throws IOException if a problem occured reading the zip entry.
	 */
	public static byte[] getZipEntryByteContent(ZipEntry ze, ZipFile zip) throws IOException {

		InputStream stream = null;
		try {
			stream = zip.getInputStream(ze);
			if (stream == null)
				throw new IOException("Invalid zip entry name : " + ze.getName()); //$NON-NLS-1$
			return getInputStreamAsByteArray(stream, (int) ze.getSize());
		} finally {
			close(stream);
		}
	}

	public static void close(Object object) {
		if (object == null)
			return;
		try {
			if (object instanceof InputStream)
				((InputStream) object).close();
			else if (object instanceof OutputStream)
				((OutputStream) object).close();
			else if (object instanceof ZipFile)
				((ZipFile) object).close();
		} catch (IOException e) {
			//ignore
		}
	}

	/**
	 * Returns the given input stream's contents as a byte array.
	 * If a length is specified (ie. if length != -1), only length bytes
	 * are returned. Otherwise all bytes in the stream are returned.
	 * Note this doesn't close the stream.
	 * @throws IOException if a problem occured reading the stream.
	 */
	public static byte[] getInputStreamAsByteArray(InputStream stream, int length) throws IOException {
		byte[] contents;
		if (length == -1) {
			contents = new byte[0];
			int contentsLength = 0;
			int amountRead = -1;
			do {
				int amountRequested = Math.max(stream.available(), DEFAULT_READING_SIZE); // read at least 8K

				// resize contents if needed
				if (contentsLength + amountRequested > contents.length) {
					System.arraycopy(contents, 0, contents = new byte[contentsLength + amountRequested], 0, contentsLength);
				}

				// read as many bytes as possible
				amountRead = stream.read(contents, contentsLength, amountRequested);

				if (amountRead > 0) {
					// remember length of contents
					contentsLength += amountRead;
				}
			} while (amountRead != -1);

			// resize contents if necessary
			if (contentsLength < contents.length) {
				System.arraycopy(contents, 0, contents = new byte[contentsLength], 0, contentsLength);
			}
		} else {
			contents = new byte[length];
			int len = 0;
			int readSize = 0;
			while ((readSize != -1) && (len != length)) {
				// See PR 1FMS89U
				// We record first the read size. In this case len is the actual read size.
				len += readSize;
				readSize = stream.read(contents, len, length - len);
			}
		}

		return contents;
	}

	public static ClassFileAttribute getAttribute(MethodInfo methodInfo, char[] attributeName) {
		ClassFileAttribute[] attributes = methodInfo.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (Arrays.equals(attributes[i].getAttributeName(), attributeName)) {
				return attributes[i];
			}
		}
		return null;
	}

	public static ClassFileAttribute getAttribute(FieldInfo fieldInfo, char[] attributeName) {
		ClassFileAttribute[] attributes = fieldInfo.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (Arrays.equals(attributes[i].getAttributeName(), attributeName)) {
				return attributes[i];
			}
		}
		return null;
	}

	public static ClassFileAttribute getAttribute(ClassFileReader classFileReader, char[] attributeName) {
		ClassFileAttribute[] attributes = classFileReader.getAttributes();
		for (int i = 0, max = attributes.length; i < max; i++) {
			if (Arrays.equals(attributes[i].getAttributeName(), attributeName)) {
				return attributes[i];
			}
		}
		return null;
	}

	/**
	 * Scans the given string for a type signature starting at the given index
	 * and returns the index of the last character.
	 * <pre>
	 * TypeSignature:
	 *  |  BaseTypeSignature
	 *  |  ArrayTypeSignature
	 *  |  ClassTypeSignature
	 *  |  TypeVariableSignature
	 * </pre>
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a type signature
	 */
	public static int scanTypeSignature(char[] string, int start) {
		// need a minimum 1 char
		if (start >= string.length) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		switch (c) {
			case Signature.C_ARRAY :
				return scanArrayTypeSignature(string, start);
			case Signature.C_RESOLVED :
			case Signature.C_UNRESOLVED :
				return scanClassTypeSignature(string, start);
			case Signature.C_TYPE_VARIABLE :
				return scanTypeVariableSignature(string, start);
			case Signature.C_BOOLEAN :
			case Signature.C_BYTE :
			case Signature.C_CHAR :
			case Signature.C_DOUBLE :
			case Signature.C_FLOAT :
			case Signature.C_INT :
			case Signature.C_LONG :
			case Signature.C_SHORT :
			case Signature.C_VOID :
				return scanBaseTypeSignature(string, start);
			case Signature.C_CAPTURE :
				return scanCaptureTypeSignature(string, start);
			case Signature.C_EXTENDS :
			case Signature.C_SUPER :
			case Signature.C_STAR :
				return scanTypeBoundSignature(string, start);
			default :
				throw new IllegalArgumentException();
		}
	}

	/**
	 * Scans the given string for a base type signature starting at the given index
	 * and returns the index of the last character.
	 * <pre>
	 * BaseTypeSignature:
	 *     <b>B</b> | <b>C</b> | <b>D</b> | <b>F</b> | <b>I</b>
	 *   | <b>J</b> | <b>S</b> | <b>V</b> | <b>Z</b>
	 * </pre>
	 * Note that although the base type "V" is only allowed in method return types,
	 * there is no syntactic ambiguity. This method will accept them anywhere
	 * without complaint.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a base type signature
	 */
	public static int scanBaseTypeSignature(char[] string, int start) {
		// need a minimum 1 char
		if (start >= string.length) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		if ("BCDFIJSVZ".indexOf(c) >= 0) { //$NON-NLS-1$
			return start;
		}
		throw new IllegalArgumentException();
	}

	/**
	 * Scans the given string for an array type signature starting at the given
	 * index and returns the index of the last character.
	 * <pre>
	 * ArrayTypeSignature:
	 *     <b>[</b> TypeSignature
	 * </pre>
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not an array type signature
	 */
	public static int scanArrayTypeSignature(char[] string, int start) {
		int length = string.length;
		// need a minimum 2 char
		if (start >= length - 1) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		if (c != Signature.C_ARRAY) {
			throw new IllegalArgumentException();
		}

		c = string[++start];
		while (c == Signature.C_ARRAY) {
			// need a minimum 2 char
			if (start >= length - 1) {
				throw new IllegalArgumentException();
			}
			c = string[++start];
		}
		return scanTypeSignature(string, start);
	}

	/**
	 * Scans the given string for a capture of a wildcard type signature starting at the given
	 * index and returns the index of the last character.
	 * <pre>
	 * CaptureTypeSignature:
	 *     <b>!</b> TypeBoundSignature
	 * </pre>
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a capture type signature
	 */
	public static int scanCaptureTypeSignature(char[] string, int start) {
		// need a minimum 2 char
		if (start >= string.length - 1) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		if (c != Signature.C_CAPTURE) {
			throw new IllegalArgumentException();
		}
		return scanTypeBoundSignature(string, start + 1);
	}

	/**
	 * Scans the given string for a type variable signature starting at the given
	 * index and returns the index of the last character.
	 * <pre>
	 * TypeVariableSignature:
	 *     <b>T</b> Identifier <b>;</b>
	 * </pre>
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a type variable signature
	 */
	public static int scanTypeVariableSignature(char[] string, int start) {
		// need a minimum 3 chars "Tx;"
		if (start >= string.length - 2) {
			throw new IllegalArgumentException();
		}
		// must start in "T"
		char c = string[start];
		if (c != Signature.C_TYPE_VARIABLE) {
			throw new IllegalArgumentException();
		}
		int id = scanIdentifier(string, start + 1);
		c = string[id + 1];
		if (c == Signature.C_SEMICOLON) {
			return id + 1;
		}
		throw new IllegalArgumentException();
	}

	/**
	 * Scans the given string for an identifier starting at the given
	 * index and returns the index of the last character.
	 * Stop characters are: ";", ":", "&lt;", "&gt;", "/", ".".
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not an identifier
	 */
	public static int scanIdentifier(char[] string, int start) {
		// need a minimum 1 char
		if (start >= string.length) {
			throw new IllegalArgumentException();
		}
		int p = start;
		while (true) {
			char c = string[p];
			if (c == '<' || c == '>' || c == ':' || c == ';' || c == '.' || c == '/') {
				return p - 1;
			}
			p++;
			if (p == string.length) {
				return p - 1;
			}
		}
	}

	/**
	 * Scans the given string for a class type signature starting at the given
	 * index and returns the index of the last character.
	 * <pre>
	 * ClassTypeSignature:
	 *     { <b>L</b> | <b>Q</b> } Identifier
	 *           { { <b>/</b> | <b>.</b> Identifier [ <b>&lt;</b> TypeArgumentSignature* <b>&gt;</b> ] }
	 *           <b>;</b>
	 * </pre>
	 * Note that although all "/"-identifiers most come before "."-identifiers,
	 * there is no syntactic ambiguity. This method will accept them without
	 * complaint.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a class type signature
	 */
	public static int scanClassTypeSignature(char[] string, int start) {
		// need a minimum 3 chars "Lx;"
		if (start >= string.length - 2) {
			throw new IllegalArgumentException();
		}
		// must start in "L" or "Q"
		char c = string[start];
		if (c != Signature.C_RESOLVED && c != Signature.C_UNRESOLVED) {
			return -1;
		}
		int p = start + 1;
		while (true) {
			if (p >= string.length) {
				throw new IllegalArgumentException();
			}
			c = string[p];
			if (c == Signature.C_SEMICOLON) {
				// all done
				return p;
			} else if (c == Signature.C_GENERIC_START) {
				int e = scanTypeArgumentSignatures(string, p);
				p = e;
			} else if (c == Signature.C_DOT || c == '/') {
				int id = scanIdentifier(string, p + 1);
				p = id;
			}
			p++;
		}
	}

	/**
	 * Scans the given string for a type bound signature starting at the given
	 * index and returns the index of the last character.
	 * <pre>
	 * TypeBoundSignature:
	 *     <b>[-+]</b> TypeSignature <b>;</b>
	 *     <b>*</b></b>
	 * </pre>
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a type variable signature
	 */
	public static int scanTypeBoundSignature(char[] string, int start) {
		// need a minimum 1 char for wildcard
		if (start >= string.length) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		switch (c) {
			case Signature.C_STAR :
				return start;
			case Signature.C_SUPER :
			case Signature.C_EXTENDS :
				// need a minimum 3 chars "+[I"
				if (start >= string.length - 2) {
					throw new IllegalArgumentException();
				}
				break;
			default :
				// must start in "+/-"
				throw new IllegalArgumentException();

		}
		c = string[++start];
		switch (c) {
			case Signature.C_CAPTURE :
				return scanCaptureTypeSignature(string, start);
			case Signature.C_SUPER :
			case Signature.C_EXTENDS :
				return scanTypeBoundSignature(string, start);
			case Signature.C_RESOLVED :
			case Signature.C_UNRESOLVED :
				return scanClassTypeSignature(string, start);
			case Signature.C_TYPE_VARIABLE :
				return scanTypeVariableSignature(string, start);
			case Signature.C_ARRAY :
				return scanArrayTypeSignature(string, start);
			case Signature.C_STAR :
				return start;
			default :
				throw new IllegalArgumentException();
		}
	}

	/**
	 * Scans the given string for a list of type argument signatures starting at
	 * the given index and returns the index of the last character.
	 * <pre>
	 * TypeArgumentSignatures:
	 *     <b>&lt;</b> TypeArgumentSignature* <b>&gt;</b>
	 * </pre>
	 * Note that although there is supposed to be at least one type argument, there
	 * is no syntactic ambiguity if there are none. This method will accept zero
	 * type argument signatures without complaint.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a list of type arguments
	 * signatures
	 */
	public static int scanTypeArgumentSignatures(char[] string, int start) {
		// need a minimum 2 char "<>"
		if (start >= string.length - 1) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		if (c != Signature.C_GENERIC_START) {
			throw new IllegalArgumentException();
		}
		int p = start + 1;
		while (true) {
			if (p >= string.length) {
				throw new IllegalArgumentException();
			}
			c = string[p];
			if (c == Signature.C_GENERIC_END) {
				return p;
			}
			int e = scanTypeArgumentSignature(string, p);
			p = e + 1;
		}
	}

	/**
	 * Scans the given string for a type argument signature starting at the given
	 * index and returns the index of the last character.
	 * <pre>
	 * TypeArgumentSignature:
	 *     <b>&#42;</b>
	 *  |  <b>+</b> TypeSignature
	 *  |  <b>-</b> TypeSignature
	 *  |  TypeSignature
	 * </pre>
	 * Note that although base types are not allowed in type arguments, there is
	 * no syntactic ambiguity. This method will accept them without complaint.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a type argument signature
	 */
	public static int scanTypeArgumentSignature(char[] string, int start) {
		// need a minimum 1 char
		if (start >= string.length) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		switch (c) {
			case Signature.C_STAR :
				return start;
			case Signature.C_EXTENDS :
			case Signature.C_SUPER :
				return scanTypeBoundSignature(string, start);
			default :
				return scanTypeSignature(string, start);
		}
	}

	static void appendTypeSignature(char[] string, int start, StringBuffer buffer, boolean compact) {
		char c = string[start];
		switch (c) {
			case Signature.C_ARRAY :
				appendArrayTypeSignature(string, start, buffer, compact);
				break;
			case Signature.C_RESOLVED :
				appendClassTypeSignature(string, start, buffer, compact);
				break;
			case Signature.C_TYPE_VARIABLE :
				int e = scanTypeVariableSignature(string, start);
				buffer.append(string, start + 1, e - start - 1);
				break;
			case Signature.C_BOOLEAN :
				buffer.append(BOOLEAN);
				break;
			case Signature.C_BYTE :
				buffer.append(BYTE);
				break;
			case Signature.C_CHAR :
				buffer.append(CHAR);
				break;
			case Signature.C_DOUBLE :
				buffer.append(DOUBLE);
				break;
			case Signature.C_FLOAT :
				buffer.append(FLOAT);
				break;
			case Signature.C_INT :
				buffer.append(INT);
				break;
			case Signature.C_LONG :
				buffer.append(LONG);
				break;
			case Signature.C_SHORT :
				buffer.append(SHORT);
				break;
			case Signature.C_VOID :
				buffer.append(VOID);
				break;
		}
	}

	private static void appendArrayTypeSignature(char[] string, int start, StringBuffer buffer, boolean compact) {
		int length = string.length;
		// need a minimum 2 char
		if (start >= length - 1) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		if (c != Signature.C_ARRAY) {
			throw new IllegalArgumentException();
		}

		int index = start;
		c = string[++index];
		while (c == Signature.C_ARRAY) {
			// need a minimum 2 char
			if (index >= length - 1) {
				throw new IllegalArgumentException();
			}
			c = string[++index];
		}

		appendTypeSignature(string, index, buffer, compact);

		for (int i = 0, dims = index - start; i < dims; i++) {
			buffer.append('[').append(']');
		}
	}

	private static void appendClassTypeSignature(char[] string, int start, StringBuffer buffer, boolean compact) {
		char c = string[start];
		if (c != Signature.C_RESOLVED) {
			return;
		}
		int p = start + 1;
		int checkpoint = buffer.length();
		while (true) {
			c = string[p];
			switch (c) {
				case Signature.C_SEMICOLON :
					// all done
					return;
				case Signature.C_DOT :
				case '/' :
					// erase package prefix
					if (compact) {
						buffer.setLength(checkpoint);
					} else {
						buffer.append('.');
					}
					break;
				case Signature.C_DOLLAR :
					/**
					 * Convert '$' in resolved type signatures into '.'.
					 * NOTE: This assumes that the type signature is an inner type
					 * signature. This is true in most cases, but someone can define a
					 * non-inner type name containing a '$'.
					 */
					buffer.append('.');
					break;
				default :
					buffer.append(c);
			}
			p++;
		}
	}

	public static String toString(char[] declaringClass, char[] methodName, char[] methodSignature, boolean includeReturnType, boolean compact) {
		final boolean isConstructor = Arrays.equals(methodName, INIT);
		int firstParen = CharOperation.indexOf(Signature.C_PARAM_START, methodSignature);
		if (firstParen == -1) {
			return ""; //$NON-NLS-1$
		}

		StringBuffer buffer = new StringBuffer(methodSignature.length + 10);

		// decode declaring class name
		// it can be either an array signature or a type signature
		if (declaringClass.length > 0) {
			char[] declaringClassSignature = null;
			if (declaringClass[0] == Signature.C_ARRAY) {
				CharOperation.replace(declaringClass, '/', '.');
				declaringClassSignature = Signature.toCharArray(declaringClass);
			} else {
				CharOperation.replace(declaringClass, '/', '.');
				declaringClassSignature = declaringClass;
			}
			int lastIndexOfSlash = CharOperation.lastIndexOf('.', declaringClassSignature);
			if (compact && lastIndexOfSlash != -1) {
				buffer.append(declaringClassSignature, lastIndexOfSlash + 1, declaringClassSignature.length - lastIndexOfSlash - 1);
			} else {
				buffer.append(declaringClassSignature);
			}
		}

		// selector
		if (!isConstructor) {
			buffer.append('.');
			if (methodName != null) {
				buffer.append(methodName);
			}
		}

		// parameters
		buffer.append('(');
		char[][] pts = Signature.getParameterTypes(methodSignature);
		for (int i = 0, max = pts.length; i < max; i++) {
			appendTypeSignature(pts[i], 0, buffer, compact);
			if (i != pts.length - 1) {
				buffer.append(',');
				buffer.append(' ');
			}
		}
		buffer.append(')');

		if (!isConstructor) {
			buffer.append(" : "); //$NON-NLS-1$
			// return type
			if (includeReturnType) {
				char[] rts = Signature.getReturnType(methodSignature);
				appendTypeSignature(rts, 0, buffer, compact);
			}
		}
		return String.valueOf(buffer);
	}
}
