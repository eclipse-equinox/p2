package org.eclipse.equinox.internal.p2.publisher;

public interface IPublishingAdvice {

	/**
	 * Merge the given advice together with this advice.  <code>null</code> is returned 
	 * if the advice is incompatible.  
	 * @param advice the advice to merge
	 * @return the merged advice or <code>null</code>
	 */
	public IPublishingAdvice merge(IPublishingAdvice advice);
}
