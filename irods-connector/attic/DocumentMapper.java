/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.File;
import java.io.FileNotFoundException;

import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.JargonRuntimeException;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.federation.FederatedDocumentWriter;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.value.BinaryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create appropriate ModeShape documents based on the underlying iRDOS type
 * 
 * see http://grepcode.com/file/repository.jboss.org/nexus/content/repositories/releases/org.modeshape/modeshape-jcr/3.7.1.Final/org/modeshape/connector/filesystem/FileSystemConnector.java?av=f
 * 
 * 
 * @author Mike Conway - DICE (www.irods.org)
 * 
 */
class DocumentMapper {

	private static final Logger log = LoggerFactory
			.getLogger(DocumentMapper.class);
	private final ConnectorContext connectorContext;
	private final IRODSAccount irodsAccount;
	private DocumentTranslator translator;

	private static final String FILE_SEPARATOR = System
			.getProperty("file.separator");
	private static final String DELIMITER = "/";
	private static final String NT_FOLDER = "nt:folder";
	private static final String NT_FILE = "nt:file";
	private static final String NT_RESOURCE = "nt:resource";
	private static final String MIX_MIME_TYPE = "mix:mimeType";
	private static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
	private static final String JCR_DATA = "jcr:data";
	private static final String JCR_MIME_TYPE = "jcr:mimeType";
	private static final String JCR_ENCODING = "jcr:encoding";
	private static final String JCR_CREATED = "jcr:created";
	private static final String JCR_CREATED_BY = "jcr:createdBy";
	private static final String JCR_LAST_MODIFIED = "jcr:lastModified";
	private static final String JCR_LAST_MODIFIED_BY = "jcr:lastModified";
	private static final String JCR_CONTENT = "jcr:content";
	private static final String JCR_CONTENT_SUFFIX = DELIMITER + JCR_CONTENT;
	private static final int JCR_CONTENT_SUFFIX_LENGTH = JCR_CONTENT_SUFFIX
			.length();

	private static final String EXTRA_PROPERTIES_JSON = "json";
	private static final String EXTRA_PROPERTIES_LEGACY = "legacy";
	private static final String EXTRA_PROPERTIES_NONE = "none";
	private final IRODSWriteableConnector irodsConnector;

	/**
	 * Default constuctor
	 * 
	 * @param connectorContext
	 *            {@link ConnectorContext} with access and environmental
	 *            information
	 */
	DocumentMapper(ConnectorContext connectorContext, IRODSWriteableConnector irodsConnector) {
		if (connectorContext == null) {
			throw new IllegalArgumentException("null connectorContext");
		}
		if (irodsConnector == null) {
			throw new IllegalArgumentException("null irodsConnector");
		}
		this.irodsConnector = irodsConnector;
		this.connectorContext = connectorContext;
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
		
		 boolean isResource = isContentNode(id);
		 
		 if (isResource) {
			
		 }
		
		
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
	
	private Document resourceNodeForId(final String id) {
		DocumentWriter writer = null;

		  writer = newDocument(id);
          BinaryValue binaryValue = binaryFor(file);
         writer.setPrimaryType(NT_RESOURCE);
           writer.addProperty(JCR_DATA, binaryValue);
          if (addMimeTypeMixin) {
              String mimeType = null;
              String encoding = null; // We don't really know this
              try {
                   mimeType = binaryValue.getMimeType();
            } catch (Throwable e) {
                   getLogger().error(e, JcrI18n.couldNotGetMimeType, getSourceName(), id, e.getMessage());
               }
              writer.addProperty(JCR_ENCODING, encoding);
              writer.addProperty(JCR_MIME_TYPE, mimeType);
          }
          writer.addProperty(JCR_LAST_MODIFIED, factories().getDateFactory().create(file.lastModified()));
           writer.addProperty(JCR_LAST_MODIFIED_BY, null); // ignored

          // make these binary not queryable. If we really want to query them, we need to switch to external binaries
          writer.setNotQueryable();
         parentFile = file;
	}

	private Document retriveDataObjectForId(IRODSFile docFile) {
		DocumentWriter writer = null;
		File parentFile = docFile.getParentFile();

		writer = newDocument(docFile.getAbsolutePath());
		writer.setPrimaryType(NT_FILE);
		writer.addProperty(JCR_CREATED,
				irodsConnector.getConnectorFactories().getDateFactory().create(docFile.lastModified()));
		writer.addProperty(JCR_CREATED_BY, null); // ignored
		String childId = isRoot(docFile.getAbsolutePath()) ? JCR_CONTENT_SUFFIX : docFile.getAbsolutePath() + JCR_CONTENT_SUFFIX;
		writer.addChild(childId, JCR_CONTENT);
		return writer.document();

	}

	private Document retriveCollectionForId(IRODSFile docFile) {
		// TODO Auto-generated method stub
		return null;
	}

	protected DocumentWriter newDocument(String id) {
		return new FederatedDocumentWriter(translator).setId(id);
	}
	
	 private boolean isRoot( String id ) {
		         return DELIMITER.equals(id);
	 }
	 
	 protected boolean isContentNode( String id ) {
		return id.endsWith(JCR_CONTENT_SUFFIX);
	 }

}
