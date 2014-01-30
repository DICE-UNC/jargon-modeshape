/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.federation.spi.DocumentChanges;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.federation.spi.ExtraPropertiesStore;
import org.modeshape.jcr.federation.spi.PageKey;
import org.modeshape.jcr.federation.spi.Pageable;
import org.modeshape.jcr.federation.spi.WritableConnector;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ModeShape SPI connector
 * 
 * @author Mike Conway - DICE (www.irods.org)
 * 
 */
public class IRODSWriteableConnector extends WritableConnector implements
		ExtraPropertiesStore, Pageable {

	private DocumentMapper documentMapper;
	private ConnectorContext connectorContext;
	public static final Logger log = LoggerFactory
			.getLogger(IRODSWriteableConnector.class);

	/**
	 * Get the default document writer
	 * 
	 * @param id
	 * @return
	 */
	protected DocumentWriter newDocumentWriter(final String id) {
		return super.newDocument(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.federation.spi.Connector#initialize(javax.jcr.
	 * NamespaceRegistry, org.modeshape.jcr.api.nodetype.NodeTypeManager)
	 */
	@Override
	public void initialize(final NamespaceRegistry registry,
			final NodeTypeManager nodeTypeManager) throws RepositoryException,
			IOException {
		super.initialize(registry, nodeTypeManager);
		connectorContext = new ConnectorContext();
		log.info("getting documentMapper");
		documentMapper = new DocumentMapper(connectorContext);
		log.info("initialized");

	}

	@Override
	public Document getChildren(final PageKey arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Name, Property> getProperties(final String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeProperties(final String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void storeProperties(final String arg0,
			final Map<Name, Property> arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateProperties(final String arg0,
			final Map<Name, Property> arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public Document getDocumentById(final String id) {
		log.info("getDocumentById()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null id");
		}

		log.info("id:{}", id);
		
		Document document = documentMapper.

	}

	@Override
	public String getDocumentId(final String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getDocumentPathsById(final String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasDocument(final String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String newDocumentId(final String arg0, final Name arg1,
			final Name arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeDocument(final String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void storeDocument(final Document arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateDocument(final DocumentChanges arg0) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the documentMapper
	 */
	DocumentMapper getDocumentMapper() {
		return documentMapper;
	}

	/**
	 * @param documentMapper
	 *            the documentMapper to set
	 */
	void setDocumentMapper(DocumentMapper documentMapper) {
		this.documentMapper = documentMapper;
	}

	/**
	 * @return the connectorContext
	 */
	ConnectorContext getConnectorContext() {
		return connectorContext;
	}

	/**
	 * @param connectorContext
	 *            the connectorContext to set
	 */
	void setConnectorContext(ConnectorContext connectorContext) {
		this.connectorContext = connectorContext;
	}

}
