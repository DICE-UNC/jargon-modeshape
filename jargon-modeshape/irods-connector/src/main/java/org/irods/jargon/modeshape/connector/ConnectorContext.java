/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.JargonRuntimeException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;

/**
 * Context for iRODS connectors
 * 
 * @author Mike Conway - DICE (www.irods.org)
 * 
 */
public class ConnectorContext {

	private final IRODSFileSystem irodsFileSystem;

	/**
	 * Default constructor
	 */
	public ConnectorContext() {
		try {
			irodsFileSystem = IRODSFileSystem.instance();
		} catch (JargonException e) {
			throw new JargonRuntimeException(
					"Unable to create IRODSFileSystem", e);
		}
	}

	/**
	 * @return the irodsAccessObjectFactory
	 */
	public IRODSAccessObjectFactory getIrodsAccessObjectFactory() {
		try {
			return irodsFileSystem.getIRODSAccessObjectFactory();
		} catch (JargonException e) {
			throw new JargonRuntimeException(
					"Unable to reference irodsAccessObjectFactory", e);
		}
	}

}
