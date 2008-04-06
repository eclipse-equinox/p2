package org.eclipse.equinox.internal.provisional.p2.metadata.generator;

public interface IPublishingAdvice {

	/**
	 * Merge the given advice together with this advice.  <code>null</code> is returned 
	 * if the advice is incompatible.  
	 * @param advice the advice to merge
	 * @return the merged advice or <code>null</code>
	 */
	public IPublishingAdvice merge(IPublishingAdvice advice);
}
