/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ql;

import java.util.Iterator;

/**
 * A node in the expression tree
 */
public interface IExpression {
	int TYPE_VARIABLE = 1;
	int TYPE_PARAMETER = 2;
	int TYPE_LITERAL = 3;
	int TYPE_AND = 4;
	int TYPE_OR = 5;
	int TYPE_FUNCTION = 6;
	int TYPE_ARRAY = 7;
	int TYPE_AT = 8;
	int TYPE_MEMBER = 9;
	int TYPE_ALL = 10;
	int TYPE_EXISTS = 11;
	int TYPE_SELECT = 12;
	int TYPE_COLLECT = 13;
	int TYPE_TRAVERSE = 14;
	int TYPE_LATEST = 15;
	int TYPE_FLATTEN = 16;
	int TYPE_LIMIT = 17;
	int TYPE_FIRST = 18;
	int TYPE_LAMBDA = 19;
	int TYPE_EQUALS = 20;
	int TYPE_GREATER = 21;
	int TYPE_LESS = 22;
	int TYPE_GREATER_EQUAL = 23;
	int TYPE_LESS_EQUAL = 24;
	int TYPE_MATCHES = 25;
	int TYPE_NOT = 26;
	int TYPE_ASSIGNMENT = 27;
	int TYPE_CONDITION = 28;
	int TYPE_NOT_EQUALS = 29;
	int TYPE_UNIQUE = 30;

	String VARIABLE_TRANSLATIONS = "translations"; //$NON-NLS-1$
	String VARIABLE_EVERYTHING = "everything"; //$NON-NLS-1$
	String VARIABLE_ITEM = "item"; //$NON-NLS-1$

	/**
	 * Let the visitor visit this instance and all expressions that this
	 * instance contains.
	 * @param visitor The visiting visitor.
	 * @return <code>true</code> if the visitor should continue visiting, <code>false</code> otherwise.
	 */
	boolean accept(IExpressionVisitor visitor);

	/**
	 * Evaluate this expression with given context and variables.
	 * @param context The evaluation context
	 * @return The result of the evaluation.
	 */
	Object evaluate(IEvaluationContext context);

	/**
	 * Evaluate this expression with given context and variables and return a result
	 * in the form of an iterator.
	 * @param context The evaluation context
	 * @return The result of the evaluation.
	 */
	Iterator evaluateAsIterator(IEvaluationContext context);

	/**
	 * Returns the expression type (see TYPE_xxx constants).
	 */
	int getExpressionType();

	/**
	 * Checks if this expression will need an instance of {@link ITranslationSupport} to execute
	 * @return <code>true</code> if translation support is needed.
	 */
	boolean needsTranslations();

	/**
	 * A special toString method that can be used when efficient string concatenation is
	 * desired. Avoids the need to create new StringBuffer instances for each concatenation
	 * when traversing the expression tree.
	 * @param receiver The receiver of the string representation.
	 */
	void toString(StringBuffer receiver);
}
