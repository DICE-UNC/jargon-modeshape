/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.service.AbstractJargonService;
import org.irods.jargon.modeshape.connector.IrodsNodeTypes;
import org.irods.jargon.modeshape.connector.IrodsWriteableConnector;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.irods.jargon.modeshape.connector.exceptions.UnknownNodeTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for node types
 * 
 * @author Mike Conway - DICE
 * 
 */
public class NodeTypeFactoryImpl extends AbstractJargonService implements
		NodeTypeFactory {

	private final IrodsWriteableConnector irodsWriteableConnector;

	public static final Logger log = LoggerFactory
			.getLogger(NodeTypeFactoryImpl.class);

	/**
	 * Constructor for factory to create node types as described by the id
	 * 
	 * @param irodsAccessObjectFactory
	 *            {@link IRODSAccessObjectFactory}
	 * @param irodsAccount
	 *            {@link IRODSAccount}
	 * @param pathUtilities
	 *            {@link PathUtilities}
	 */
	public NodeTypeFactoryImpl(
			IRODSAccessObjectFactory irodsAccessObjectFactory,
			IRODSAccount irodsAccount,
			final IrodsWriteableConnector irodsWriteableConnector) {
		super(irodsAccessObjectFactory, irodsAccount);

		if (irodsWriteableConnector == null) {
			throw new IllegalArgumentException("null irodsWriteableConnector");
		}
		this.irodsWriteableConnector = irodsWriteableConnector;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.irods.jargon.modeshape.connector.nodetypes.NodeTypeFactory#instanceForId
	 * (java.lang.String, int)
	 */
	@Override
	public Document instanceForId(final String id, final int offset)
			throws JargonException {
		log.info("instanceForId()");

		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("id is null or missing");
		}

		log.info("getting creator for node type based on id..");
		AbstractNodeTypeCreator creator = instanceNodeTypeCreatorForId(id);
		log.info("got creator, get instance of document...");
		return creator.instanceForId(id, offset);

	}

	private AbstractNodeTypeCreator instanceNodeTypeCreatorForId(final String id)
			throws UnknownNodeTypeException {

		log.info("instanceNodeTypeCreatorForId()");

		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("id is null or missing");
		}

		IrodsNodeTypes irodsNodeType = irodsWriteableConnector
				.getPathUtilities().getNodeTypeForId(id);
		log.info("resolved node type:{}", irodsNodeType);

		AbstractNodeTypeCreator abstractNodeTypeCreator = null;
		switch (irodsNodeType) {
		case ROOT_NODE:
			break;
		case CONTENT_NODE:
			break;
		case AVU_NODE:
			break;
		default:
			return new FileNodeCreator(irodsAccessObjectFactory, irodsAccount,
					irodsWriteableConnector);
		}

		return abstractNodeTypeCreator;

	}

}
