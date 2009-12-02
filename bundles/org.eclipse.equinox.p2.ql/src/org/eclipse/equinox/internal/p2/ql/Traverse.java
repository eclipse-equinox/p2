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
package org.eclipse.equinox.internal.p2.ql;

import java.util.*;

/**
 * An expression that will collect items recursively based on a <code>rule</code>.
 * The <code>rule</code> is applied for each item in the <code>collection</code> and
 * is supposed to create a new collection. The <code>rule</code> is then applied for each item
 * in the new collection. All items are collected into a set and items that are already
 * in that set will not be perused again. The set becomes the result of the traversal.
 */
public final class Traverse extends CollectionFilter {

	private static final int maxParallelThreads = 2;

	public static final String OPERATOR = "traverse"; //$NON-NLS-1$

	static class TraverseRequest {
		Object parent;
		VariableScope scope;
	}

	final LinkedList queue = new LinkedList();
	int waitCount = 0;

	private class Worker extends Thread {
		private final Map collector;
		private final ExpressionContext context;

		Worker(Map collector, ExpressionContext context) {
			this.collector = collector;
			this.context = context;
		}

		public void run() {
			while (!interrupted()) {
				TraverseRequest request;
				synchronized (queue) {
					while (queue.isEmpty()) {
						try {
							if (++waitCount == maxParallelThreads) {
								// The queue is empty and everyone else is waiting so we're done!
								queue.notifyAll();
								return;
							}
							queue.wait();
							--waitCount;
						} catch (InterruptedException e) {
							queue.clear();
							queue.notifyAll();
							return;
						}
					}
					request = (TraverseRequest) queue.removeFirst();
					if (collector.put(request.parent, Boolean.TRUE) != null)
						continue;
				}

				VariableScope scope = new VariableScope(request.scope);
				variable.setValue(scope, request.parent);
				Iterator subIterator = lambda.evaluateAsIterator(context, scope);
				while (subIterator.hasNext()) {
					TraverseRequest subRequest = new TraverseRequest();
					subRequest.parent = subIterator.next();
					subRequest.scope = scope;
					synchronized (queue) {
						queue.addLast(subRequest);
						queue.notifyAll();
					}
				}
			}
		}
	}

	public Traverse(Expression collection, LambdaExpression lambda) {
		super(collection, lambda);
	}

	Object evaluate(ExpressionContext context, VariableScope scope, Iterator iterator) {
		Map collector = new IdentityHashMap();
		Worker[] workers = new Worker[maxParallelThreads];
		for (int idx = 0; idx < maxParallelThreads; ++idx)
			workers[idx] = new Worker(collector, context);
		while (iterator.hasNext()) {
			TraverseRequest subRequest = new TraverseRequest();
			subRequest.parent = iterator.next();
			subRequest.scope = scope;
			queue.addLast(subRequest);
		}
		for (int idx = 0; idx < maxParallelThreads; ++idx)
			workers[idx].start();
		try {
			for (int idx = 0; idx < maxParallelThreads; ++idx)
				workers[idx].join();
		} catch (InterruptedException e) {
			return null;
		}
		return collector.keySet();
	}

	String getOperator() {
		return OPERATOR;
	}
}
