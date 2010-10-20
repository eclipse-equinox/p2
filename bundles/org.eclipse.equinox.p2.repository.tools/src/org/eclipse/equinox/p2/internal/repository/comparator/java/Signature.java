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

import java.util.ArrayList;

/*
 * Provides methods for encoding and decoding type and method signature strings.
 * <p>
 * Signatures obtained from parsing source files (i.e. files with one of the
 * {@link JavaCore#getJavaLikeExtensions() Java-like extensions}) differ subtly
 * from ones obtained from pre-compiled binary (".class") files in class names are
 * usually left unresolved in the former. For example, the normal resolved form
 * of the type "String" embeds the class's package name ("Ljava.lang.String;"
 * or "Ljava/lang/String;"), whereas the unresolved form contains only what is
 * written "QString;".
 * </p>
 * <p>
 * Generic types introduce to the Java language in J2SE 1.5 add three new
 * facets to signatures: type variables, parameterized types with type arguments,
 * and formal type parameters. <i>Rich</i> signatures containing these facets
 * only occur when dealing with code that makes overt use of the new language
 * features. All other code, and certainly all Java code written or compiled
 * with J2SE 1.4 or earlier, involved only <i>simple</i> signatures.
 * </p>
 * <p>
 * Note that the "Q" and "!" formats are specific to Eclipse; the remainder
 * are specified in the JVM spec.
 * </p>
 * <p>
 * The syntax for a type signature is:
 * <pre>
 * TypeSignature ::=
 *     "B"  // byte
 *   | "C"  // char
 *   | "D"  // double
 *   | "F"  // float
 *   | "I"  // int
 *   | "J"  // long
 *   | "S"  // short
 *   | "V"  // void
 *   | "Z"  // boolean
 *   | "T" + Identifier + ";" // type variable
 *   | "[" + TypeSignature  // array X[]
 *   | "!" + TypeSignature  // capture-of ?
 *   | ResolvedClassTypeSignature
 *   | UnresolvedClassTypeSignature
 *
 * ResolvedClassTypeSignature ::= // resolved named type (in compiled code)
 *     "L" + Identifier + OptionalTypeArguments
 *           ( ( "." | "/" ) + Identifier + OptionalTypeArguments )* + ";"
 *     | OptionalTypeParameters + "L" + Identifier +
 *           ( ( "." | "/" ) + Identifier )* + ";"
 *
 * UnresolvedClassTypeSignature ::= // unresolved named type (in source code)
 *     "Q" + Identifier + OptionalTypeArguments
 *           ( ( "." | "/" ) + Identifier + OptionalTypeArguments )* + ";"
 *     | OptionalTypeParameters "Q" + Identifier +
 *           ( ( "." | "/" ) + Identifier )* + ";"
 *
 * OptionalTypeArguments ::=
 *     "&lt;" + TypeArgument+ + "&gt;"
 *   |
 *
 * TypeArgument ::=
 *   | TypeSignature
 *   | "*" // wildcard ?
 *   | "+" TypeSignature // wildcard ? extends X
 *   | "-" TypeSignature // wildcard ? super X
 *
 * OptionalTypeParameters ::=
 *     "&lt;" + FormalTypeParameterSignature+ + "&gt;"
 *   |
 * </pre>
 * </p>
 * <p>
 * Examples:
 * <ul>
 *   <li><code>"[[I"</code> denotes <code>int[][]</code></li>
 *   <li><code>"Ljava.lang.String;"</code> denotes <code>java.lang.String</code> in compiled code</li>
 *   <li><code>"QString;"</code> denotes <code>String</code> in source code</li>
 *   <li><code>"Qjava.lang.String;"</code> denotes <code>java.lang.String</code> in source code</li>
 *   <li><code>"[QString;"</code> denotes <code>String[]</code> in source code</li>
 *   <li><code>"QMap&lt;QString;*&gt;;"</code> denotes <code>Map&lt;String,?&gt;</code> in source code</li>
 *   <li><code>"Qjava.util.List&ltTV;&gt;;"</code> denotes <code>java.util.List&lt;V&gt;</code> in source code</li>
 *   <li><code>"&ltE;&gt;Ljava.util.List;"</code> denotes <code>&lt;E&gt;java.util.List</code> in source code</li>
 * </ul>
 * </p>
 * <p>
 * The syntax for a method signature is:
 * <pre>
 * MethodSignature ::= OptionalTypeParameters + "(" + ParamTypeSignature* + ")" + ReturnTypeSignature
 * ParamTypeSignature ::= TypeSignature
 * ReturnTypeSignature ::= TypeSignature
 * </pre>
 * <p>
 * Examples:
 * <ul>
 *   <li><code>"()I"</code> denotes <code>int foo()</code></li>
 *   <li><code>"([Ljava.lang.String;)V"</code> denotes <code>void foo(java.lang.String[])</code> in compiled code</li>
 *   <li><code>"(QString;)QObject;"</code> denotes <code>Object foo(String)</code> in source code</li>
 * </ul>
 * </p>
 * <p>
 * The syntax for a formal type parameter signature is:
 * <pre>
 * FormalTypeParameterSignature ::=
 *     TypeVariableName + OptionalClassBound + InterfaceBound*
 * TypeVariableName ::= Identifier
 * OptionalClassBound ::=
 *     ":"
 *   | ":" + TypeSignature
 * InterfaceBound ::=
 *     ":" + TypeSignature
 * </pre>
 * <p>
 * Examples:
 * <ul>
 *   <li><code>"X:"</code> denotes <code>X</code></li>
 *   <li><code>"X:QReader;"</code> denotes <code>X extends Reader</code> in source code</li>
 *   <li><code>"X:QReader;:QSerializable;"</code> denotes <code>X extends Reader & Serializable</code> in source code</li>
 * </ul>
 * </p>
 * <p>
 * This class provides static methods and constants only.
 * </p>
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class Signature {
	/*
	 * Character constant indicating the primitive type boolean in a signature.
	 * Value is <code>'Z'</code>.
	 */
	public static final char C_BOOLEAN = 'Z';

	/*
	 * Character constant indicating the primitive type byte in a signature.
	 * Value is <code>'B'</code>.
	 */
	public static final char C_BYTE = 'B';

	/*
	 * Character constant indicating the primitive type char in a signature.
	 * Value is <code>'C'</code>.
	 */
	public static final char C_CHAR = 'C';

	/*
	 * Character constant indicating the primitive type double in a signature.
	 * Value is <code>'D'</code>.
	 */
	public static final char C_DOUBLE = 'D';

	/*
	 * Character constant indicating the primitive type float in a signature.
	 * Value is <code>'F'</code>.
	 */
	public static final char C_FLOAT = 'F';

	/*
	 * Character constant indicating the primitive type int in a signature.
	 * Value is <code>'I'</code>.
	 */
	public static final char C_INT = 'I';

	/*
	 * Character constant indicating the semicolon in a signature.
	 * Value is <code>';'</code>.
	 */
	public static final char C_SEMICOLON = ';';

	/*
	 * Character constant indicating the colon in a signature.
	 * Value is <code>':'</code>.
	 * @since 3.0
	 */
	public static final char C_COLON = ':';

	/*
	 * Character constant indicating the primitive type long in a signature.
	 * Value is <code>'J'</code>.
	 */
	public static final char C_LONG = 'J';

	/*
	 * Character constant indicating the primitive type short in a signature.
	 * Value is <code>'S'</code>.
	 */
	public static final char C_SHORT = 'S';

	/*
	 * Character constant indicating result type void in a signature.
	 * Value is <code>'V'</code>.
	 */
	public static final char C_VOID = 'V';

	/*
	 * Character constant indicating the start of a resolved type variable in a
	 * signature. Value is <code>'T'</code>.
	 * @since 3.0
	 */
	public static final char C_TYPE_VARIABLE = 'T';

	/*
	 * Character constant indicating an unbound wildcard type argument
	 * in a signature.
	 * Value is <code>'*'</code>.
	 * @since 3.0
	 */
	public static final char C_STAR = '*';

	/*
	 * Character constant indicating an exception in a signature.
	 * Value is <code>'^'</code>.
	 * @since 3.1
	 */
	public static final char C_EXCEPTION_START = '^';

	/*
	 * Character constant indicating a bound wildcard type argument
	 * in a signature with extends clause.
	 * Value is <code>'+'</code>.
	 * @since 3.1
	 */
	public static final char C_EXTENDS = '+';

	/*
	 * Character constant indicating a bound wildcard type argument
	 * in a signature with super clause.
	 * Value is <code>'-'</code>.
	 * @since 3.1
	 */
	public static final char C_SUPER = '-';

	/*
	 * Character constant indicating the dot in a signature.
	 * Value is <code>'.'</code>.
	 */
	public static final char C_DOT = '.';

	/*
	 * Character constant indicating the dollar in a signature.
	 * Value is <code>'$'</code>.
	 */
	public static final char C_DOLLAR = '$';

	/*
	 * Character constant indicating an array type in a signature.
	 * Value is <code>'['</code>.
	 */
	public static final char C_ARRAY = '[';

	/*
	 * Character constant indicating the start of a resolved, named type in a
	 * signature. Value is <code>'L'</code>.
	 */
	public static final char C_RESOLVED = 'L';

	/*
	 * Character constant indicating the start of an unresolved, named type in a
	 * signature. Value is <code>'Q'</code>.
	 */
	public static final char C_UNRESOLVED = 'Q';

	/*
	 * Character constant indicating the end of a named type in a signature.
	 * Value is <code>';'</code>.
	 */
	public static final char C_NAME_END = ';';

	/*
	 * Character constant indicating the start of a parameter type list in a
	 * signature. Value is <code>'('</code>.
	 */
	public static final char C_PARAM_START = '(';

	/*
	 * Character constant indicating the end of a parameter type list in a
	 * signature. Value is <code>')'</code>.
	 */
	public static final char C_PARAM_END = ')';

	/*
	 * Character constant indicating the start of a formal type parameter
	 * (or type argument) list in a signature. Value is <code>'&lt;'</code>.
	 * @since 3.0
	 */
	public static final char C_GENERIC_START = '<';

	/*
	 * Character constant indicating the end of a generic type list in a
	 * signature. Value is <code>'&gt;'</code>.
	 * @since 3.0
	 */
	public static final char C_GENERIC_END = '>';

	/*
	 * Character constant indicating a capture of a wildcard type in a
	 * signature. Value is <code>'!'</code>.
	 * @since 3.1
	 */
	public static final char C_CAPTURE = '!';

	/*
	 * String constant for the signature of the primitive type boolean.
	 * Value is <code>"Z"</code>.
	 */
	public static final String SIG_BOOLEAN = "Z"; //$NON-NLS-1$

	/*
	 * String constant for the signature of the primitive type byte.
	 * Value is <code>"B"</code>.
	 */
	public static final String SIG_BYTE = "B"; //$NON-NLS-1$

	/*
	 * String constant for the signature of the primitive type char.
	 * Value is <code>"C"</code>.
	 */
	public static final String SIG_CHAR = "C"; //$NON-NLS-1$

	/*
	 * String constant for the signature of the primitive type double.
	 * Value is <code>"D"</code>.
	 */
	public static final String SIG_DOUBLE = "D"; //$NON-NLS-1$

	/*
	 * String constant for the signature of the primitive type float.
	 * Value is <code>"F"</code>.
	 */
	public static final String SIG_FLOAT = "F"; //$NON-NLS-1$

	/*
	 * String constant for the signature of the primitive type int.
	 * Value is <code>"I"</code>.
	 */
	public static final String SIG_INT = "I"; //$NON-NLS-1$

	/*
	 * String constant for the signature of the primitive type long.
	 * Value is <code>"J"</code>.
	 */
	public static final String SIG_LONG = "J"; //$NON-NLS-1$

	/*
	 * String constant for the signature of the primitive type short.
	 * Value is <code>"S"</code>.
	 */
	public static final String SIG_SHORT = "S"; //$NON-NLS-1$

	/* String constant for the signature of result type void.
	 * Value is <code>"V"</code>.
	 */
	public static final String SIG_VOID = "V"; //$NON-NLS-1$

	/*
	 * Kind constant for a class type signature.
	 * @see #getTypeSignatureKind(String)
	 * @since 3.0
	 */
	public static final int CLASS_TYPE_SIGNATURE = 1;

	/*
	 * Kind constant for a base (primitive or void) type signature.
	 * @see #getTypeSignatureKind(String)
	 * @since 3.0
	 */
	public static final int BASE_TYPE_SIGNATURE = 2;

	/*
	 * Kind constant for a type variable signature.
	 * @see #getTypeSignatureKind(String)
	 * @since 3.0
	 */
	public static final int TYPE_VARIABLE_SIGNATURE = 3;

	/*
	 * Kind constant for an array type signature.
	 * @see #getTypeSignatureKind(String)
	 * @since 3.0
	 */
	public static final int ARRAY_TYPE_SIGNATURE = 4;

	/*
	 * Kind constant for a wildcard type signature.
	 * @see #getTypeSignatureKind(String)
	 * @since 3.1
	 */
	public static final int WILDCARD_TYPE_SIGNATURE = 5;

	/*
	 * Kind constant for the capture of a wildcard type signature.
	 * @see #getTypeSignatureKind(String)
	 * @since 3.1
	 */
	public static final int CAPTURE_TYPE_SIGNATURE = 6;

	private static final char[] BOOLEAN = "boolean".toCharArray(); //$NON-NLS-1$
	private static final char[] BYTE = "byte".toCharArray(); //$NON-NLS-1$
	private static final char[] CHAR = "char".toCharArray(); //$NON-NLS-1$
	private static final char[] DOUBLE = "double".toCharArray(); //$NON-NLS-1$
	private static final char[] FLOAT = "float".toCharArray(); //$NON-NLS-1$
	private static final char[] INT = "int".toCharArray(); //$NON-NLS-1$
	private static final char[] LONG = "long".toCharArray(); //$NON-NLS-1$
	private static final char[] SHORT = "short".toCharArray(); //$NON-NLS-1$
	private static final char[] VOID = "void".toCharArray(); //$NON-NLS-1$
	//	private static final char[] EXTENDS = "extends".toCharArray(); //$NON-NLS-1$
	//	private static final char[] SUPER = "super".toCharArray(); //$NON-NLS-1$
	private static final char[] CAPTURE = "capture-of".toCharArray(); //$NON-NLS-1$

	/*
	 * Returns the number of parameter types in the given method signature.
	 *
	 * @param methodSignature the method signature
	 * @return the number of parameters
	 * @exception IllegalArgumentException if the signature is not syntactically
	 *   correct
	 * @since 2.0
	 */
	public static int getParameterCount(char[] methodSignature) throws IllegalArgumentException {
		try {
			int count = 0;
			int i = CharOperation.indexOf(C_PARAM_START, methodSignature);
			if (i < 0) {
				throw new IllegalArgumentException();
			}
			i++;
			for (;;) {
				if (methodSignature[i] == C_PARAM_END) {
					return count;
				}
				int e = Utility.scanTypeSignature(methodSignature, i);
				if (e < 0) {
					throw new IllegalArgumentException();
				}
				i = e + 1;
				count++;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException();
		}
	}

	/*
	 * Extracts the parameter type signatures from the given method signature.
	 * The method signature is expected to be dot-based.
	 *
	 * @param methodSignature the method signature
	 * @return the list of parameter type signatures
	 * @exception IllegalArgumentException if the signature is syntactically
	 *   incorrect
	 *
	 * @since 2.0
	 */
	public static char[][] getParameterTypes(char[] methodSignature) throws IllegalArgumentException {
		try {
			int count = getParameterCount(methodSignature);
			char[][] result = new char[count][];
			if (count == 0) {
				return result;
			}
			int i = CharOperation.indexOf(C_PARAM_START, methodSignature);
			if (i < 0) {
				throw new IllegalArgumentException();
			}
			i++;
			int t = 0;
			for (;;) {
				if (methodSignature[i] == C_PARAM_END) {
					return result;
				}
				int e = Utility.scanTypeSignature(methodSignature, i);
				if (e < 0) {
					throw new IllegalArgumentException();
				}
				result[t] = CharOperation.subarray(methodSignature, i, e + 1);
				t++;
				i = e + 1;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException();
		}
	}

	/*
	 * Extracts the return type from the given method signature. The method signature is
	 * expected to be dot-based.
	 *
	 * @param methodSignature the method signature
	 * @return the type signature of the return type
	 * @exception IllegalArgumentException if the signature is syntactically
	 *   incorrect
	 *
	 * @since 2.0
	 */
	public static char[] getReturnType(char[] methodSignature) throws IllegalArgumentException {
		// skip type parameters
		int paren = CharOperation.lastIndexOf(C_PARAM_END, methodSignature);
		if (paren == -1) {
			throw new IllegalArgumentException();
		}
		// there could be thrown exceptions behind, thus scan one type exactly
		int last = Utility.scanTypeSignature(methodSignature, paren + 1);
		return CharOperation.subarray(methodSignature, paren + 1, last + 1);
	}

	/*
	 * Extracts the class and interface bounds from the given formal type
	 * parameter signature. The class bound, if present, is listed before
	 * the interface bounds. The signature is expected to be dot-based.
	 *
	 * @param formalTypeParameterSignature the formal type parameter signature
	 * @return the (possibly empty) list of type signatures for the bounds
	 * @exception IllegalArgumentException if the signature is syntactically
	 *   incorrect
	 * @since 3.0
	 */
	public static char[][] getTypeParameterBounds(char[] formalTypeParameterSignature) throws IllegalArgumentException {
		int p1 = CharOperation.indexOf(C_COLON, formalTypeParameterSignature);
		if (p1 < 0) {
			// no ":" means can't be a formal type parameter signature
			throw new IllegalArgumentException();
		}
		if (p1 == formalTypeParameterSignature.length - 1) {
			// no class or interface bounds
			return CharOperation.NO_CHAR_CHAR;
		}
		int p2 = CharOperation.indexOf(C_COLON, formalTypeParameterSignature, p1 + 1);
		char[] classBound;
		if (p2 < 0) {
			// no interface bounds
			classBound = CharOperation.subarray(formalTypeParameterSignature, p1 + 1, formalTypeParameterSignature.length);
			return new char[][] {classBound};
		}
		if (p2 == p1 + 1) {
			// no class bound, but 1 or more interface bounds
			classBound = null;
		} else {
			classBound = CharOperation.subarray(formalTypeParameterSignature, p1 + 1, p2);
		}
		char[][] interfaceBounds = CharOperation.splitOn(C_COLON, formalTypeParameterSignature, p2 + 1, formalTypeParameterSignature.length);
		if (classBound == null) {
			return interfaceBounds;
		}
		int resultLength = interfaceBounds.length + 1;
		char[][] result = new char[resultLength][];
		result[0] = classBound;
		System.arraycopy(interfaceBounds, 0, result, 1, interfaceBounds.length);
		return result;
	}

	/*
	 * Extracts the type parameter signatures from the given method or type signature.
	 * The method or type signature is expected to be dot-based.
	 *
	 * @param methodOrTypeSignature the method or type signature
	 * @return the list of type parameter signatures
	 * @exception IllegalArgumentException if the signature is syntactically
	 *   incorrect
	 *
	 * @since 3.1
	 */
	public static char[][] getTypeParameters(char[] methodOrTypeSignature) throws IllegalArgumentException {
		try {
			int length = methodOrTypeSignature.length;
			if (length == 0)
				return CharOperation.NO_CHAR_CHAR;
			if (methodOrTypeSignature[0] != C_GENERIC_START)
				return CharOperation.NO_CHAR_CHAR;

			ArrayList<char[]> paramList = new ArrayList<char[]>(1);
			int paramStart = 1, i = 1; // start after leading '<'
			while (i < length) {
				if (methodOrTypeSignature[i] == C_GENERIC_END) {
					int size = paramList.size();
					if (size == 0)
						throw new IllegalArgumentException();
					char[][] result;
					paramList.toArray(result = new char[size][]);
					return result;
				}
				i = CharOperation.indexOf(C_COLON, methodOrTypeSignature, i);
				if (i < 0 || i >= length)
					throw new IllegalArgumentException();
				// iterate over bounds
				while (methodOrTypeSignature[i] == ':') {
					i++; // skip colon
					switch (methodOrTypeSignature[i]) {
						case ':' :
							// no class bound
							break;
						case C_GENERIC_END :
							break;
						case C_RESOLVED :
							try {
								i = Utility.scanClassTypeSignature(methodOrTypeSignature, i);
								i++; // position at start of next param if any
							} catch (IllegalArgumentException e) {
								// not a class type signature -> it is a new type parameter
							}
							break;
						case C_ARRAY :
							try {
								i = Utility.scanArrayTypeSignature(methodOrTypeSignature, i);
								i++; // position at start of next param if any
							} catch (IllegalArgumentException e) {
								// not an array type signature -> it is a new type parameter
							}
							break;
						case C_TYPE_VARIABLE :
							try {
								i = Utility.scanTypeVariableSignature(methodOrTypeSignature, i);
								i++; // position at start of next param if any
							} catch (IllegalArgumentException e) {
								// not a type variable signature -> it is a new type parameter
							}
							break;
						// default: another type parameter is starting
					}
				}
				paramList.add(CharOperation.subarray(methodOrTypeSignature, paramStart, i));
				paramStart = i; // next param start from here
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			// invalid signature, fall through
		}
		throw new IllegalArgumentException();
	}

	/*
	 * Converts the given type signature to a readable string. The signature is expected to
	 * be dot-based.
	 *
	 * <p>
	 * For example:
	 * <pre>
	 * <code>
	 * toString({'[', 'L', 'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g', '.', 'S', 't', 'r', 'i', 'n', 'g', ';'}) -> {'j', 'a', 'v', 'a', '.', 'l', 'a', 'n', 'g', '.', 'S', 't', 'r', 'i', 'n', 'g', '[', ']'}
	 * toString({'I'}) -> {'i', 'n', 't'}
	 * toString({'+', 'L', 'O', 'b', 'j', 'e', 'c', 't', ';'}) -> {'?', ' ', 'e', 'x', 't', 'e', 'n', 'd', 's', ' ', 'O', 'b', 'j', 'e', 'c', 't'}
	 * </code>
	 * </pre>
	 * </p>
	 * <p>
	 * Note: This method assumes that a type signature containing a <code>'$'</code>
	 * is an inner type signature. While this is correct in most cases, someone could
	 * define a non-inner type name containing a <code>'$'</code>. Handling this
	 * correctly in all cases would have required resolving the signature, which
	 * generally not feasible.
	 * </p>
	 *
	 * @param signature the type signature
	 * @return the string representation of the type
	 * @exception IllegalArgumentException if the signature is not syntactically
	 *   correct
	 *
	 * @since 2.0
	 */
	public static char[] toCharArray(char[] signature) throws IllegalArgumentException {
		int sigLength = signature.length;
		if (sigLength == 0 || signature[0] == C_PARAM_START || signature[0] == C_GENERIC_START) {
			return toCharArray(signature, CharOperation.NO_CHAR, null, true, true);
		}

		StringBuffer buffer = new StringBuffer(signature.length + 10);
		appendTypeSignature(signature, 0, true, buffer);
		char[] result = new char[buffer.length()];
		buffer.getChars(0, buffer.length(), result, 0);
		return result;
	}

	/*
	 * Converts the given method signature to a readable form. The method signature is expected to
	 * be dot-based.
	 * <p>
	 * For example:
	 * <pre>
	 * <code>
	 * toString("([Ljava.lang.String;)V", "main", new String[] {"args"}, false, true) -> "void main(String[] args)"
	 * </code>
	 * </pre>
	 * </p>
	 *
	 * @param methodSignature the method signature to convert
	 * @param methodName the name of the method to insert in the result, or
	 *   <code>null</code> if no method name is to be included
	 * @param parameterNames the parameter names to insert in the result, or
	 *   <code>null</code> if no parameter names are to be included; if supplied,
	 *   the number of parameter names must match that of the method signature
	 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
	 *   qualified, and <code>false</code> to use only simple names
	 * @param includeReturnType <code>true</code> if the return type is to be
	 *   included
	 * @param isVargArgs <code>true</code> if the last argument should be displayed as a
	 * variable argument,  <code>false</code> otherwise.
	 * @return the char array representation of the method signature
	 *
	 * @since 3.1
	 */
	public static char[] toCharArray(char[] methodSignature, char[] methodName, char[][] parameterNames, boolean fullyQualifyTypeNames, boolean includeReturnType, boolean isVargArgs) {
		int firstParen = CharOperation.indexOf(C_PARAM_START, methodSignature);
		if (firstParen == -1) {
			throw new IllegalArgumentException();
		}

		StringBuffer buffer = new StringBuffer(methodSignature.length + 10);

		// return type
		if (includeReturnType) {
			char[] rts = getReturnType(methodSignature);
			appendTypeSignature(rts, 0, fullyQualifyTypeNames, buffer);
			buffer.append(' ');
		}

		// selector
		if (methodName != null) {
			buffer.append(methodName);
		}

		// parameters
		buffer.append('(');
		char[][] pts = getParameterTypes(methodSignature);
		// search for the last array in the signature
		int max = pts.length;
		int index = max - 1;
		loop: for (int i = index; i >= 0; i--) {
			if (pts[i][0] == Signature.C_ARRAY) {
				break loop;
			}
			index--;
		}
		for (int i = 0; i < max; i++) {
			if (i == index) {
				appendTypeSignature(pts[i], 0, fullyQualifyTypeNames, buffer, isVargArgs);
			} else {
				appendTypeSignature(pts[i], 0, fullyQualifyTypeNames, buffer);
			}
			if (parameterNames != null) {
				buffer.append(' ');
				buffer.append(parameterNames[i]);
			}
			if (i != pts.length - 1) {
				buffer.append(',');
				buffer.append(' ');
			}
		}
		buffer.append(')');
		char[] result = new char[buffer.length()];
		buffer.getChars(0, buffer.length(), result, 0);
		return result;
	}

	/*
	 * Scans the given string for a type signature starting at the given
	 * index and appends it to the given buffer, and returns the index of the last
	 * character.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
	 *   qualified, and <code>false</code> to use only simple names
	 * @param buffer the string buffer to append to
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a type signature
	 * @see Utility#scanTypeSignature(char[], int)
	 */
	private static int appendTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer) {
		return appendTypeSignature(string, start, fullyQualifyTypeNames, buffer, false);
	}

	/*
	 * Scans the given string for a type signature starting at the given
	 * index and appends it to the given buffer, and returns the index of the last
	 * character.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
	 *   qualified, and <code>false</code> to use only simple names
	 * @param buffer the string buffer to append to
	 * @param isVarArgs <code>true</code> if the type must be displayed as a
	 * variable argument, <code>false</code> otherwise. In this case, the type must be an array type
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a type signature, or if isVarArgs is <code>true</code>,
	 * and the type is not an array type signature.
	 * @see Utility#scanTypeSignature(char[], int)
	 */
	private static int appendTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer, boolean isVarArgs) {
		// need a minimum 1 char
		if (start >= string.length) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		if (isVarArgs) {
			switch (c) {
				case C_ARRAY :
					return appendArrayTypeSignature(string, start, fullyQualifyTypeNames, buffer, true);
				case C_RESOLVED :
				case C_UNRESOLVED :
				case C_TYPE_VARIABLE :
				case C_BOOLEAN :
				case C_BYTE :
				case C_CHAR :
				case C_DOUBLE :
				case C_FLOAT :
				case C_INT :
				case C_LONG :
				case C_SHORT :
				case C_VOID :
				case C_STAR :
				case C_EXTENDS :
				case C_SUPER :
				case C_CAPTURE :
				default :
					throw new IllegalArgumentException(); // a var args is an array type
			}
		}
		switch (c) {
			case C_ARRAY :
				return appendArrayTypeSignature(string, start, fullyQualifyTypeNames, buffer);
			case C_RESOLVED :
			case C_UNRESOLVED :
				return appendClassTypeSignature(string, start, fullyQualifyTypeNames, buffer);
			case C_TYPE_VARIABLE :
				int e = Utility.scanTypeVariableSignature(string, start);
				buffer.append(string, start + 1, e - start - 1);
				return e;
			case C_BOOLEAN :
				buffer.append(BOOLEAN);
				return start;
			case C_BYTE :
				buffer.append(BYTE);
				return start;
			case C_CHAR :
				buffer.append(CHAR);
				return start;
			case C_DOUBLE :
				buffer.append(DOUBLE);
				return start;
			case C_FLOAT :
				buffer.append(FLOAT);
				return start;
			case C_INT :
				buffer.append(INT);
				return start;
			case C_LONG :
				buffer.append(LONG);
				return start;
			case C_SHORT :
				buffer.append(SHORT);
				return start;
			case C_VOID :
				buffer.append(VOID);
				return start;
			case C_CAPTURE :
				return appendCaptureTypeSignature(string, start, fullyQualifyTypeNames, buffer);
			case C_STAR :
			case C_EXTENDS :
			case C_SUPER :
				return appendTypeArgumentSignature(string, start, fullyQualifyTypeNames, buffer);
			default :
				throw new IllegalArgumentException();
		}
	}

	/*
	 * Scans the given string for an array type signature starting at the given
	 * index and appends it to the given buffer, and returns the index of the last
	 * character.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
	 *   qualified, and <code>false</code> to use only simple names
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not an array type signature
	 * @see Utility#scanArrayTypeSignature(char[], int)
	 */
	private static int appendArrayTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer) {
		return appendArrayTypeSignature(string, start, fullyQualifyTypeNames, buffer, false);
	}

	/*
	 * Scans the given string for an array type signature starting at the given
	 * index and appends it to the given buffer, and returns the index of the last
	 * character.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
	 *   qualified, and <code>false</code> to use only simple names
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not an array type signature
	 * @see Utility#scanArrayTypeSignature(char[], int)
	 */
	private static int appendCaptureTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer) {
		// need a minimum 2 char
		if (start >= string.length - 1) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		if (c != C_CAPTURE) {
			throw new IllegalArgumentException();
		}
		buffer.append(CAPTURE).append(' ');
		return appendTypeArgumentSignature(string, start + 1, fullyQualifyTypeNames, buffer);
	}

	/*
	 * Scans the given string for an array type signature starting at the given
	 * index and appends it to the given buffer, and returns the index of the last
	 * character.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
	 *   qualified, and <code>false</code> to use only simple names
	 * @param isVarArgs <code>true</code> if the array type must be displayed as a
	 * variable argument, <code>false</code> otherwise
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not an array type signature
	 * @see Utility#scanArrayTypeSignature(char[], int)
	 */
	private static int appendArrayTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer, boolean isVarArgs) {
		int length = string.length;
		// need a minimum 2 char
		if (start >= length - 1) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		if (c != C_ARRAY) {
			throw new IllegalArgumentException();
		}

		int index = start;
		c = string[++index];
		while (c == C_ARRAY) {
			// need a minimum 2 char
			if (index >= length - 1) {
				throw new IllegalArgumentException();
			}
			c = string[++index];
		}

		int e = appendTypeSignature(string, index, fullyQualifyTypeNames, buffer);

		for (int i = 1, dims = index - start; i < dims; i++) {
			buffer.append('[').append(']');
		}

		if (isVarArgs) {
			buffer.append('.').append('.').append('.');
		} else {
			buffer.append('[').append(']');
		}
		return e;
	}

	/*
	 * Scans the given string for a class type signature starting at the given
	 * index and appends it to the given buffer, and returns the index of the last
	 * character.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
	 *   qualified, and <code>false</code> to use only simple names
	 * @param buffer the string buffer to append to
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a class type signature
	 * @see Utility#scanClassTypeSignature(char[], int)
	 */
	private static int appendClassTypeSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer) {
		// need a minimum 3 chars "Lx;"
		if (start >= string.length - 2) {
			throw new IllegalArgumentException();
		}
		// must start in "L" or "Q"
		char c = string[start];
		if (c != C_RESOLVED && c != C_UNRESOLVED) {
			throw new IllegalArgumentException();
		}
		boolean resolved = (c == C_RESOLVED);
		boolean removePackageQualifiers = !fullyQualifyTypeNames;
		if (!resolved) {
			// keep everything in an unresolved name
			removePackageQualifiers = false;
		}
		int p = start + 1;
		int checkpoint = buffer.length();
		int innerTypeStart = -1;
		boolean inAnonymousType = false;
		while (true) {
			if (p >= string.length) {
				throw new IllegalArgumentException();
			}
			c = string[p];
			switch (c) {
				case C_SEMICOLON :
					// all done
					return p;
				case C_GENERIC_START :
					int e = appendTypeArgumentSignatures(string, p, fullyQualifyTypeNames, buffer);
					// once we hit type arguments there are no more package prefixes
					removePackageQualifiers = false;
					p = e;
					break;
				case C_DOT :
					if (removePackageQualifiers) {
						// erase package prefix
						buffer.setLength(checkpoint);
					} else {
						buffer.append('.');
					}
					break;
				case '/' :
					if (removePackageQualifiers) {
						// erase package prefix
						buffer.setLength(checkpoint);
					} else {
						buffer.append('/');
					}
					break;
				case C_DOLLAR :
					innerTypeStart = buffer.length();
					inAnonymousType = false;
					if (resolved) {
						// once we hit "$" there are no more package prefixes
						removePackageQualifiers = false;
						/*
						 * Convert '$' in resolved type signatures into '.'.
						 * NOTE: This assumes that the type signature is an inner type
						 * signature. This is true in most cases, but someone can define a
						 * non-inner type name containing a '$'.
						 */
						buffer.append('.');
					}
					break;
				default :
					if (innerTypeStart != -1 && !inAnonymousType && Character.isDigit(c)) {
						inAnonymousType = true;
						buffer.setLength(innerTypeStart); // remove '.'
						buffer.insert(checkpoint, "new "); //$NON-NLS-1$
						buffer.append("(){}"); //$NON-NLS-1$
					}
					if (!inAnonymousType)
						buffer.append(c);
					innerTypeStart = -1;
			}
			p++;
		}
	}

	/*
	 * Scans the given string for a list of type arguments signature starting at the
	 * given index and appends it to the given buffer, and returns the index of the
	 * last character.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
	 *   qualified, and <code>false</code> to use only simple names
	 * @param buffer the string buffer to append to
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a list of type argument
	 * signatures
	 * @see Utility#scanTypeArgumentSignatures(char[], int)
	 */
	private static int appendTypeArgumentSignatures(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer) {
		// need a minimum 2 char "<>"
		if (start >= string.length - 1) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		if (c != C_GENERIC_START) {
			throw new IllegalArgumentException();
		}
		buffer.append('<');
		int p = start + 1;
		int count = 0;
		while (true) {
			if (p >= string.length) {
				throw new IllegalArgumentException();
			}
			c = string[p];
			if (c == C_GENERIC_END) {
				buffer.append('>');
				return p;
			}
			if (count != 0) {
				buffer.append(',');
			}
			int e = appendTypeArgumentSignature(string, p, fullyQualifyTypeNames, buffer);
			count++;
			p = e + 1;
		}
	}

	/*
	 * Scans the given string for a type argument signature starting at the given
	 * index and appends it to the given buffer, and returns the index of the last
	 * character.
	 *
	 * @param string the signature string
	 * @param start the 0-based character index of the first character
	 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
	 *   qualified, and <code>false</code> to use only simple names
	 * @param buffer the string buffer to append to
	 * @return the 0-based character index of the last character
	 * @exception IllegalArgumentException if this is not a type argument signature
	 * @see Utility#scanTypeArgumentSignature(char[], int)
	 */
	private static int appendTypeArgumentSignature(char[] string, int start, boolean fullyQualifyTypeNames, StringBuffer buffer) {
		// need a minimum 1 char
		if (start >= string.length) {
			throw new IllegalArgumentException();
		}
		char c = string[start];
		switch (c) {
			case C_STAR :
				buffer.append('?');
				return start;
			case C_EXTENDS :
				buffer.append("? extends "); //$NON-NLS-1$
				return appendTypeSignature(string, start + 1, fullyQualifyTypeNames, buffer);
			case C_SUPER :
				buffer.append("? super "); //$NON-NLS-1$
				return appendTypeSignature(string, start + 1, fullyQualifyTypeNames, buffer);
			default :
				return appendTypeSignature(string, start, fullyQualifyTypeNames, buffer);
		}
	}

	/*
	 * Converts the given method signature to a readable form. The method signature is expected to
	 * be dot-based.
	 * <p>
	 * For example:
	 * <pre>
	 * <code>
	 * toString("([Ljava.lang.String;)V", "main", new String[] {"args"}, false, true) -> "void main(String[] args)"
	 * </code>
	 * </pre>
	 * </p>
	 *
	 * @param methodSignature the method signature to convert
	 * @param methodName the name of the method to insert in the result, or
	 *   <code>null</code> if no method name is to be included
	 * @param parameterNames the parameter names to insert in the result, or
	 *   <code>null</code> if no parameter names are to be included; if supplied,
	 *   the number of parameter names must match that of the method signature
	 * @param fullyQualifyTypeNames <code>true</code> if type names should be fully
	 *   qualified, and <code>false</code> to use only simple names
	 * @param includeReturnType <code>true</code> if the return type is to be
	 *   included
	 * @return the char array representation of the method signature
	 *
	 * @since 2.0
	 */
	public static char[] toCharArray(char[] methodSignature, char[] methodName, char[][] parameterNames, boolean fullyQualifyTypeNames, boolean includeReturnType) {
		return toCharArray(methodSignature, methodName, parameterNames, fullyQualifyTypeNames, includeReturnType, false);
	}
}
