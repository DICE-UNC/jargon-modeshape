/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.FileNotFoundException;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.JargonRuntimeException;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Conway - DICE (www.irods.org)
 * 
 */
class DocumentMapper {

	private static final Logger log = LoggerFactory
			.getLogger(DocumentMapper.class);
	private final ConnectorContext connectorContext;
	private final IRODSAccount irodsAccount;

	/**
	 * Default constuctor
	 * 
	 * @param connectorContext
	 *            {@link ConnectorContext} with access and environmental
	 *            information
	 */
	DocumentMapper(ConnectorContext connectorContext) {
		if (connectorContext == null) {
			throw new IllegalArgumentException("null connectorContext");
		}
		this.connectorContext = null;
		// FIXME: auth shim here until I understand the pluggable auth for
		// modeshape
		try {
			irodsAccount = IRODSAccount.instance("localhost", 1247, "test1",
					"test", "", "test1", "test1-resc");
		} catch (JargonException je) {
			throw new JargonRuntimeException("exception getting irods account",
					je);
		}
	}

	Document retrieveDocumentForId(final String id)
			throws FileNotFoundException, JargonException {
		log.info("retrieveDocumentForId()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}

		log.info("id:{}", id);

		log.info("get irodsFileFactory...");
		IRODSFile docFile = connectorContext.getIrodsAccessObjectFactory()
				.getIRODSFileFactory(irodsAccount).instanceIRODSFile(id);

		if (!docFile.exists()) {
			throw new FileNotFoundException("file not found");
		}

		if (docFile.isDirectory()) {
			return retriveCollectionForId(docFile);
		} else {
			return retriveDataObjectForId(docFile);
		}

	}

	private Document retriveDataObjectForId(IRODSFile docFile) {
		// TODO Auto-generated method stub
		return null;
	}

	private Document retriveCollectionForId(IRODSFile docFile) {
		// TODO Auto-generated method stub
		return null;
	}
}
