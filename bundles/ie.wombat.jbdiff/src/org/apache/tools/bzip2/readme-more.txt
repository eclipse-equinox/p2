The library org.apache.tools.bzip2 is a library version based on the package org.apache.tools.bzip2
of the Apache Ant 1.7 which is under the Apache License Version 2.0, January 2004, http://www.apache.org/licenses
Ant 1.7: http://www.apache.org/dist/ant/source/apache-ant-1.7.0-src.zip

To get rid of this ´hack´ the following bug report has been created to track this issue:
https://bugs.eclipse.org/bugs/show_bug.cgi?id=208996

Version 1.7.0.1
 - Extended CBZip2OutputStream such that it supports the finish() method as in java.util.GZipOutputStream
   This extension is proposed in BugZilla: http://issues.apache.org/bugzilla/show_bug.cgi?id=42713
   
   It replaces the original close() and finalize() methods with:
   
   	/**
	 * Finishes compressing to the underlying stream without closing it,
	 * so that multiple compressors can write subsequently to the same
	 * output stream.
	 *  
	 * @throws IOException
	 */
	public void finish() throws IOException {
		OutputStream outShadow = this.out;
		if ( outShadow != null && this.data != null ) {
			try {
				if ( this.runLength > 0 ) {
					writeRun();
				}
				this.currentChar = -1;
				endBlock();
				endCompression();
			} finally {
				this.data = null;
			}
		}
	}

	/**
	 * Overriden to close the stream.
	 */
	protected void finalize() throws Throwable {
		if ( this.data != null ) {
			close();
			super.finalize();
		}
	}

	public void close() throws IOException {
		finish();
		OutputStream outShadow = this.out;
		if ( outShadow != null ) {
			try {
				outShadow.close();
			} finally {
				this.out = null;
			}
		}

	}

