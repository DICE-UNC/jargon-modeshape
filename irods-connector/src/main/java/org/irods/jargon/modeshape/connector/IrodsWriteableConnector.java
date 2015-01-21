/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.AuthScheme;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.JargonRuntimeException;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.IRODSFileSystemSingletonWrapper;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.modeshape.connector.exceptions.IrodsConnectorRuntimeException;
import org.irods.jargon.modeshape.connector.exceptions.UnknownNodeTypeException;
import org.irods.jargon.modeshape.connector.nodetypes.AbstractNodeTypeCreator;
import org.irods.jargon.modeshape.connector.nodetypes.FileFromIdConverter;
import org.irods.jargon.modeshape.connector.nodetypes.FileFromIdConverterImpl;
import org.irods.jargon.modeshape.connector.nodetypes.IrodsBinaryValue;
import org.irods.jargon.modeshape.connector.nodetypes.NodeTypeFactory;
import org.irods.jargon.modeshape.connector.nodetypes.NodeTypeFactoryImpl;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.spi.federation.Connector;
import org.modeshape.jcr.spi.federation.DocumentChanges;
import org.modeshape.jcr.spi.federation.DocumentReader;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.spi.federation.ExtraPropertiesStore;
import org.modeshape.jcr.spi.federation.PageKey;
import org.modeshape.jcr.spi.federation.Pageable;
import org.modeshape.jcr.spi.federation.WritableConnector;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Conway - DICE ref
 *         https://github.com/ModeShape/modeshape/tree/master
 *         /connectors/modeshape
 *         -connector-git/src/main/java/org/modeshape/connector/git
 */
public class IrodsWriteableConnector extends WritableConnector implements
		Pageable {

	/**
	 * The string path for a {@link File} object that represents the top-level
	 * directory accessed by this connector. This is set via reflection and is
	 * required for this connector.
	 */
	private String directoryPath;

	public static final Logger log = LoggerFactory
			.getLogger(IrodsWriteableConnector.class);

	/**
	 * A boolean flag that specifies whether this connector should add the
	 * 'mix:mimeType' mixin to the 'nt:resource' nodes to include the
	 * 'jcr:mimeType' property. If set to <code>true</code>, the MIME type is
	 * computed immediately when the 'nt:resource' node is accessed, which might
	 * be expensive for larger files. This is <code>false</code> by default.
	 */
	private boolean addMimeTypeMixin = false;

	/**
	 * The regular expression that, if matched by a file or folder, indicates
	 * that the file or folder should be included.
	 */
	private String inclusionPattern;

	/**
	 * The regular expression that, if matched by a file or folder, indicates
	 * that the file or folder should be ignored.
	 */
	private String exclusionPattern;

	/**
	 * Configuration flag that can cause this connector to use the file contents
	 * to create a sha1 hash
	 */
	private final boolean contentBasedSha1 = false;

	/**
	 * Configuration flag that can cause this connector to operare in read-only
	 * mode if desired
	 */
	private boolean readOnly = false;

	/**
	 * Configuration of iRODS host for proxy account
	 */
	private String irodsHost = "";

	/**
	 * Configuration of iRODS port for proxy account
	 */
	private int irodsPort = 1247;

	/**
	 * Configuration of iRODS zone for proxy account
	 */
	private String irodsZone = "";

	/**
	 * Configuration of iRODS default resource for proxy account
	 */
	private String irodsDefaultResource = "";

	/**
	 * Configuration of iRODS user for proxy account
	 */
	private String irodsUser = "";

	/**
	 * Configuration of iRODS password for proxy account
	 */
	private String irodsPassword = "";

	/**
	 * Configuration of iRODS auth type for proxy account STANDARD | PAM
	 */
	private String irodsAuthType = "STANDARD";

	/**
	 * Configuration flag that can cause AVU nodes to be added when creating or
	 * reading documents
	 */
	private boolean addAvus = false;

	/**
	 * The {@link FilenameFilter} implementation that is instantiated in the
	 * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method.
	 */
	private InclusionExclusionFilenameFilter filenameFilter;

	/**
	 * Created during init phase, this will handle all the node type detection
	 * and path munging for ids
	 */
	private PathUtilities pathUtilities;

	private IRODSFileSystem irodsFileSystem;

	/**
	 * The maximum number of children a folder will expose at any given time.
	 */
	public static final int PAGE_SIZE = 5000;

	@Override
	public Document getDocumentById(String id) {

		try {
			log.info("getDocumentById()");

			if (id == null || id.isEmpty()) {
				throw new IllegalArgumentException("null or empty id");
			}

			log.info("id:{}", id);
			Document document = null;
			try {
				document = instanceNodeTypeFactory(getIrodsAccount())
						.instanceForId(id, 0);
				log.debug("returning document:{}", document);
				return document;
			} catch (UnknownNodeTypeException e) {
				log.error("unknown node type for id:{}", id, e);
				throw new DocumentStoreException(id, e);
			} catch (JargonException e) {
				log.error("jargon exception getting  node type for id:{}", id,
						e);
				throw new DocumentStoreException(id, e);
			} catch (RepositoryException e) {
				log.error("repository exception getting  node type for id:{}",
						id, e);
				throw new DocumentStoreException(id, e);
			}

		} finally {
			// this.getIrodsFileSystem().closeAndEatExceptions();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.spi.federation.Connector#getDocumentId(java.lang.String
	 * )
	 */
	@Override
	public String getDocumentId(final String path) {

		// this connector treats the ID as the path
		log.info("getDocumentId()");

		if (path == null) {
			throw new IllegalArgumentException("null path");
		}

		log.info("path:{}", path);

		try {
			FileFromIdConverter fileFromIdConverter = new FileFromIdConverterImpl(
					this.getIrodsFileSystem().getIRODSAccessObjectFactory(),
					this.getIrodsAccount(), this.pathUtilities);

			IRODSFile irodsFile = fileFromIdConverter.fileFor(path);
			return irodsFile.exists() ? path : null;

		} catch (JargonException e) {
			log.error("jargon exception getting document id", e);
			throw new DocumentStoreException(path, e);
		}

	}

	@Override
	public Collection<String> getDocumentPathsById(String id) {
		log.info("getDocumentPathsById()");
		if (id == null) {
			throw new IllegalArgumentException("null id");
		}
		log.info("id:{}", id);
		// this connector treats the ID as the path
		return Collections.singletonList(id);
	}

	@Override
	public boolean hasDocument(String id) {
		log.info("hasDocument()");
		if (id == null) {
			throw new IllegalArgumentException("null id");
		}
		log.info("id:{}", id);

		try {
			FileFromIdConverter fileFromIdConverter = new FileFromIdConverterImpl(
					this.getIrodsFileSystem().getIRODSAccessObjectFactory(),
					this.getIrodsAccount(), this.pathUtilities);
			return fileFromIdConverter.fileFor(id).exists();
		} catch (JargonException e) {
			log.error("jargon error getting file from id", e);
			throw new JargonRuntimeException(e);
		}

	}

	@Override
	public String newDocumentId(final String parentId,
			final Name newDocumentName, final Name newDocumentPrimaryType) {
		log.info("newDocumentId()");
		StringBuilder id = new StringBuilder(parentId);
		if (!parentId.endsWith(PathUtilities.DELIMITER)) {
			id.append(PathUtilities.DELIMITER);
		}

		// We're only using the name to check, which can be a bit dangerous if
		// users don't follow the JCR conventions.
		// However, it matches what "isContentNode(...)" does.
		String childNameStr = getContext().getValueFactories()
				.getStringFactory().create(newDocumentName);
		if (PathUtilities.JCR_CONTENT.equals(childNameStr)) {
			// This is for the "jcr:content" node underneath a file node. Since
			// this doesn't actually result in a file or folder
			// on the file system (it's merged into the file for the parent
			// 'nt:file' node), we'll keep the "jcr" namespace
			// prefix in the ID so that 'isContentNode(...)' works properly ...
			id.append(childNameStr);
		} else {
			// File systems don't universally deal well with ':' in the names,
			// and when they do it can be a bit awkward. Since we
			// don't often expect the node NAMES to contain namespaces (at leat
			// with this connector), we'll just
			// use the local part for the ID ...
			id.append(newDocumentName.getLocalName());
		}
		return id.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.spi.federation.Connector#removeDocument(java.lang.String
	 * )
	 */

	@Override
	public boolean removeDocument(String id) {
		log.info("removeDocument()");
		if (id == null) {
			throw new IllegalArgumentException("null id");
		}
		try {
			log.info("id:{}", id);
			FileFromIdConverter fileFromIdConverter = new FileFromIdConverterImpl(
					this.getIrodsFileSystem().getIRODSAccessObjectFactory(),
					this.getIrodsAccount(), this.pathUtilities);
			IRODSFile file = fileFromIdConverter.fileFor(id);
			checkFileNotExcluded(id, (File) file);
			// Remove the extra properties at the old location ...
			extraPropertiesStore().removeProperties(id);
			// Now remove the file (if it is there) ...
			if (!file.exists()) {
				return false;
			}

			IRODSFile irodsFile = file;
			irodsFile.delete();
			return true;
		} catch (JargonException e) {
			log.error("error deleting file with id:{}", id, e);
			throw new DocumentStoreException(id, "Error deleting file");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.spi.federation.Connector#storeDocument(org.infinispan
	 * .schematic.document.Document)
	 */
	@Override
	public void storeDocument(Document document) {
		log.info("storeDocument()");
		if (document == null) {
			throw new IllegalArgumentException("null document");
		}
		try {
			AbstractNodeTypeCreator nodeCreator = instanceNodeTypeFactory(
					getIrodsAccount()).instanceCreatorForDocument(document);
			nodeCreator.store(document);
			log.info("store successful");
		} catch (UnknownNodeTypeException e) {
			log.error("unknown node type, cannot store", e);
			throw new IrodsConnectorRuntimeException(e);
		} catch (RepositoryException e) {
			log.error("store failed with repository exception", e);
			throw new IrodsConnectorRuntimeException(e);
		} catch (JargonException e) {
			log.error("store failed with jargon exception", e);
			throw new IrodsConnectorRuntimeException(e);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.spi.federation.Connector#updateDocument(org.modeshape
	 * .jcr.spi.federation.DocumentChanges)
	 */
	@Override
	public void updateDocument(DocumentChanges documentChanges) {
		log.info("updateDocument()");
		if (documentChanges == null) {
			throw new IllegalArgumentException("null documentChanges");
		}

		AbstractNodeTypeCreator nodeCreator;
		try {
			nodeCreator = instanceNodeTypeFactory(getIrodsAccount())
					.instanceCreatorForDocumentChanges(documentChanges);
		} catch (UnknownNodeTypeException e) {
			log.error("unknown node type, cannot update", e);
			throw new IrodsConnectorRuntimeException(e);
		} catch (RepositoryException e) {
			log.error("update failed with repository exception", e);
			throw new IrodsConnectorRuntimeException(e);
		} catch (JargonException e) {
			log.error("update failed with jargon exception", e);
			throw new IrodsConnectorRuntimeException(e);
		}
		nodeCreator.update(documentChanges);
		log.info("updateSuccessful");

	}

	/**
	 * @return the addMimeTypeMixin
	 */
	public boolean isAddMimeTypeMixin() {
		return addMimeTypeMixin;
	}

	/**
	 * @param addMimeTypeMixin
	 *            the addMimeTypeMixin to set
	 */
	public void setAddMimeTypeMixin(boolean addMimeTypeMixin) {
		this.addMimeTypeMixin = addMimeTypeMixin;
	}

	/**
	 * @return the inclusionPattern
	 */
	public String getInclusionPattern() {
		return inclusionPattern;
	}

	/**
	 * @param inclusionPattern
	 *            the inclusionPattern to set
	 */
	public void setInclusionPattern(String inclusionPattern) {
		this.inclusionPattern = inclusionPattern;
	}

	/**
	 * @return the exclusionPattern
	 */
	public String getExclusionPattern() {
		return exclusionPattern;
	}

	/**
	 * @param exclusionPattern
	 *            the exclusionPattern to set
	 */
	public void setExclusionPattern(String exclusionPattern) {
		this.exclusionPattern = exclusionPattern;
	}

	/**
	 * @return the readOnly
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * @param readOnly
	 *            the readOnly to set
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.spi.federation.Connector#initialize(javax.jcr.
	 * NamespaceRegistry, org.modeshape.jcr.api.nodetype.NodeTypeManager)
	 */
	@Override
	public void initialize(NamespaceRegistry registry,
			NodeTypeManager nodeTypeManager) throws RepositoryException,
			IOException {
		try {
			super.initialize(registry, nodeTypeManager);

			this.irodsFileSystem = IRODSFileSystemSingletonWrapper.instance();

			checkFieldNotNull(directoryPath, "directoryPath");

			// Initialize the filename filter ...
			filenameFilter = new InclusionExclusionFilenameFilter();
			if (exclusionPattern != null)
				filenameFilter.setExclusionPattern(exclusionPattern);
			if (inclusionPattern != null)
				filenameFilter.setInclusionPattern(inclusionPattern);

			this.pathUtilities = new PathUtilities(directoryPath,
					filenameFilter, this);

			log.info("initialized");
		} finally {
			this.getIrodsFileSystem().closeAndEatExceptions();
		}
	}

	/**
	 * use the config info to get an irods proxy account
	 * 
	 * @return {@link IRODSAccount}
	 */
	public IRODSAccount getIrodsAccount() {
		try {

			IRODSAccount irodsAccount = IRODSAccount.instance(
					this.getIrodsHost(), this.getIrodsPort(),
					this.getIrodsUser(), this.getIrodsPassword(), "",
					this.getIrodsZone(), this.getIrodsDefaultResource());

			if (this.getIrodsAuthType() == null
					|| this.getIrodsAuthType().isEmpty()) {
				log.info("normal auth");
			} else if (this.getIrodsAuthType().equals(
					AuthScheme.STANDARD.toString())) {
				log.info("normal auth");

			} else if (this.getIrodsAuthType()
					.equals(AuthScheme.PAM.toString())) {
				log.info("auth type pam");
				irodsAccount.setAuthenticationScheme(AuthScheme.PAM);
			} else {
				log.error("unknown auth type:{}", this.getIrodsAuthType());
				throw new JargonRuntimeException("unknown auth type");
			}

			return irodsAccount;

		} catch (JargonException e) {
			throw new JargonRuntimeException("unable to create irodsAccount", e);
		}
	}

	/**
	 * @return the directoryPath
	 */
	public String getDirectoryPath() {
		return directoryPath;
	}

	/**
	 * @param directoryPath
	 *            the directoryPath to set
	 */
	public void setDirectoryPath(String directoryPath) {
		this.directoryPath = directoryPath;
	}

	public PathUtilities getPathUtilities() {
		return pathUtilities;
	}

	/**
	 * Wraps the <code>newDocument()</code> method to allow factories to create
	 * new documents
	 * 
	 * @param id
	 *            <code>String</code> with the document id
	 * @return {@link DocumentWriter}
	 */
	public DocumentWriter createNewDocumentForId(final String id) {

		log.info("createNewDocumentForId()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}

		return this.newDocument(id);

	}

	/**
	 * Allow objects to get a handle to factories for various types
	 * 
	 * @return {@link ValueFactories} as obtained in the
	 *         <code>factories()</code> method of a connector
	 */
	public ValueFactories obtainHandleToFactories() {
		return this.factories();
	}

	public boolean isAddAvus() {
		return addAvus;
	}

	public void setAddAvus(boolean addAvus) {
		this.addAvus = addAvus;
	}

	/**
	 * Set a reference to the IRODSFileSystem
	 * 
	 * @return {@link IRODSFileSystem}
	 */
	public IRODSFileSystem getIrodsFileSystem() {
		return this.irodsFileSystem;
	}

	public void setIrodsFileSystem(final IRODSFileSystem irodsFileSystem) {
		this.setIrodsFileSystem(irodsFileSystem);
	}

	@Override
	public Document getChildren(PageKey pageKey) {
		log.info("getChildren()");
		return null;
	}

	/**
	 * @return the nodeTypeFactory
	 * @throws JargonException
	 */
	public NodeTypeFactory instanceNodeTypeFactory(
			final IRODSAccount irodsAccount) throws JargonException {

		if (irodsAccount == null) {
			throw new IllegalArgumentException("null irodsAcount");
		}

		return new NodeTypeFactoryImpl(this.getIrodsFileSystem()
				.getIRODSAccessObjectFactory(), irodsAccount, this);
	}

	/**
	 * @return the contentBasedSha1
	 */
	public boolean isContentBasedSha1() {
		return contentBasedSha1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.spi.federation.Connector#getBinaryValue(java.lang.String
	 * )
	 */

	@Override
	public ExternalBinaryValue getBinaryValue(final String id) {

		log.info("getBinaryValue()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}

		log.info("id:{}", id);

		try {

			IRODSFile file = this.getIrodsFileSystem()
					.getIRODSAccessObjectFactory()
					.getIRODSFileFactory(getIrodsAccount())
					.instanceIRODSFile(id);

			return new IrodsBinaryValue(this.getPathUtilities().sha1(file),
					getSourceName(), file.getAbsolutePath(), file.length(),
					file.getName(), this.getMimeTypeDetector(),
					this.getIrodsFileSystem().getIRODSAccessObjectFactory(),
					this.getIrodsAccount());

		} catch (JargonException e) {
			log.error("jargon error getting file from id", e);
			throw new JargonRuntimeException(e);
		}
	}

	/**
	 * Wrapper to allow cooperating objects to ref extra properties {@link
	 * Controller.extraPropertiesFor(String, boolean)}
	 * 
	 */
	public ExtraProperties retrieveExtraPropertiesForId(final String id,
			final boolean update) {
		return this.extraPropertiesFor(id, update);
	}

	/**
	 * Delegate to {@link Connector.extraPropertiesStore} for various node type
	 * handlers
	 * 
	 * @return
	 */
	public ExtraPropertiesStore retrieveExtraPropertiesStore() {
		return this.extraPropertiesStore();
	}

	protected void checkFileNotExcluded(final String id, final File file) {
		boolean isExcluded = !filenameFilter.accept(file.getParentFile(),
				file.getName());
		if (isExcluded) {
			log.error("file is excluded:{}", file);
			throw new DocumentStoreException(id, "file is excluded");
		}
	}

	/**
	 * @return the filenameFilter
	 */
	public InclusionExclusionFilenameFilter getFilenameFilter() {
		return filenameFilter;
	}

	public DocumentReader produceDocumentReaderFromDocument(
			final Document document) {
		if (document == null) {
			throw new IllegalArgumentException("null document");
		}

		return this.readDocument(document);
	}

	/**
	 * @return the irodsHost
	 */
	public String getIrodsHost() {
		return irodsHost;
	}

	/**
	 * @param irodsHost
	 *            the irodsHost to set
	 */
	public void setIrodsHost(String irodsHost) {
		this.irodsHost = irodsHost;
	}

	/**
	 * @return the irodsPort
	 */
	public int getIrodsPort() {
		return irodsPort;
	}

	/**
	 * @param irodsPort
	 *            the irodsPort to set
	 */
	public void setIrodsPort(int irodsPort) {
		this.irodsPort = irodsPort;
	}

	/**
	 * @return the irodsZone
	 */
	public String getIrodsZone() {
		return irodsZone;
	}

	/**
	 * @param irodsZone
	 *            the irodsZone to set
	 */
	public void setIrodsZone(String irodsZone) {
		this.irodsZone = irodsZone;
	}

	/**
	 * @return the irodsDefaultResource
	 */
	public String getIrodsDefaultResource() {
		return irodsDefaultResource;
	}

	/**
	 * @param irodsDefaultResource
	 *            the irodsDefaultResource to set
	 */
	public void setIrodsDefaultResource(String irodsDefaultResource) {
		this.irodsDefaultResource = irodsDefaultResource;
	}

	/**
	 * @return the irodsUser
	 */
	public String getIrodsUser() {
		return irodsUser;
	}

	/**
	 * @param irodsUser
	 *            the irodsUser to set
	 */
	public void setIrodsUser(String irodsUser) {
		this.irodsUser = irodsUser;
	}

	/**
	 * @return the irodsPassword
	 */
	public String getIrodsPassword() {
		return irodsPassword;
	}

	/**
	 * @param irodsPassword
	 *            the irodsPassword to set
	 */
	public void setIrodsPassword(String irodsPassword) {
		this.irodsPassword = irodsPassword;
	}

	/**
	 * @return the irodsAuthType
	 */
	public String getIrodsAuthType() {
		return irodsAuthType;
	}

	/**
	 * @param irodsAuthType
	 *            the irodsAuthType to set
	 */
	public void setIrodsAuthType(String irodsAuthType) {
		this.irodsAuthType = irodsAuthType;
	}

}
