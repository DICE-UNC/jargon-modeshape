/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.modeshape.connector.PathUtilities;

/**
 * Create a content node (iRODS file) based in its id
 * 
 * @author Mike Conway - DICE
 * 
 */
public class ContentNodeCreator extends AbstractNodeTypeCreator {

	/**
	 * @param irodsAccessObjectFactory
	 * @param irodsAccount
	 * @param pathUtilities
	 */
	public ContentNodeCreator(
			IRODSAccessObjectFactory irodsAccessObjectFactory,
			IRODSAccount irodsAccount, PathUtilities pathUtilities) {
		super(irodsAccessObjectFactory, irodsAccount, pathUtilities);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.irods.jargon.modeshape.connector.nodetypes.AbstractNodeTypeCreator
	 * #instanceForId(java.lang.String)
	 */
	@Override
	public Document instanceForId(String id) {
		return null;
	}

}
