/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.service.AbstractJargonService;
import org.irods.jargon.modeshape.connector.PathUtilities;

/**
 * @author Mike
 * 
 */
public abstract class AbstractNodeTypeCreator extends AbstractJargonService {

	private final PathUtilities pathUtilities;

	/**
	 * Constructor for factory to create a specific node type
	 * 
	 * @param irodsAccessObjectFactory
	 *            {@link IRODSAccessObjectFactory}
	 * @param irodsAccount
	 *            {@link IRODSAccount}
	 * @param pathUtilities
	 *            {@link PathUtilities}
	 */
	public AbstractNodeTypeCreator(
			IRODSAccessObjectFactory irodsAccessObjectFactory,
			IRODSAccount irodsAccount, final PathUtilities pathUtilities) {
		super(irodsAccessObjectFactory, irodsAccount);

		if (pathUtilities == null) {
			throw new IllegalArgumentException("null pathUtilities");
		}
		this.pathUtilities = pathUtilities;
	}

	/**
	 * Given an id, return the corresponding ModeShape {@link Document}
	 * 
	 * @param id
	 *            <code>String</code> with the document id
	 * @return {@link Document}
	 */
	public abstract Document instanceForId(final String id);

}
