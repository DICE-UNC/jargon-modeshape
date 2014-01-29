/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import org.irods.jargon.core.pub.IRODSAccessObjectFactory;

/**
 * Context for iRODS connectors
 * 
 * @author Mike Conway - DICE (www.irods.org)
 * 
 */
public class ConnectorContext {

	private IRODSAccessObjectFactory irodsAccessObjectFactory;

	/**
	 * Default constructor
	 */
	public ConnectorContext() {
	}

	/**
	 * @return the irodsAccessObjectFactory
	 */
	public IRODSAccessObjectFactory getIrodsAccessObjectFactory() {
		return irodsAccessObjectFactory;
	}

	/**
	 * @param irodsAccessObjectFactory
	 *            the irodsAccessObjectFactory to set
	 */
	public void setIrodsAccessObjectFactory(
			IRODSAccessObjectFactory irodsAccessObjectFactory) {
		this.irodsAccessObjectFactory = irodsAccessObjectFactory;
	}

}
