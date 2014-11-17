/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.service.AbstractJargonService;
import org.irods.jargon.modeshape.connector.IrodsWriteableConnector;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.value.ValueFactories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Conway - DICE
 * 
 */
public abstract class AbstractNodeTypeCreator extends AbstractJargonService {

	public static final String MIX_MIME_TYPE = "mix:mimeType";
	public static final String AVU_ID = "/avuId";

	public static final Logger log = LoggerFactory
			.getLogger(AbstractNodeTypeCreator.class);

	private IrodsWriteableConnector connector;

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
			IRODSAccount irodsAccount, final IrodsWriteableConnector connector) {
		super(irodsAccessObjectFactory, irodsAccount);

		if (connector == null) {
			throw new IllegalArgumentException("null connector");
		}
		this.connector = connector;
	}

	/**
	 * Given an id, return the corresponding ModeShape {@link Document}
	 * 
	 * @param id
	 *            <code>String</code> with the document id
	 * @return {@link Document}
	 * @throws JargonException
	 */
	public abstract Document instanceForId(final String id)
			throws JargonException;

	/**
	 * @return the pathUtilities
	 */
	protected PathUtilities getPathUtilities() {
		return connector.getPathUtilities();
	}

	/**
	 * Get a new <code>Document</code> for the id
	 * 
	 * @param id
	 *            <code>String</code> with a modeshape id
	 * @return {@link Document}
	 */
	protected DocumentWriter newDocument(final String id) {
		log.info("newDocument()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}
		return connector.createNewDocumentForId(id);

	}

	/**
	 * Get <code>ValueFactories</code> from the connector
	 * 
	 * @return {@link ValueFactories}
	 */
	protected ValueFactories factories() {
		return connector.obtainHandleToFactories();
	}

	/**
	 * Check if mimetypemixin should be added
	 * 
	 * @return <code>boolean</code> of true if mimetypemixin should be added
	 */
	protected boolean isAddMimeTypeMixin() {
		return this.connector.isAddMimeTypeMixin();
	}

	/**
	 * Check if avus should be added
	 * 
	 * @return <code>boolean</code> of true if avus should be added
	 */
	protected boolean isIncludeAvus() {
		return this.connector.isAddAvus();
	}

}
