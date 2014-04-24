/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.FileNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.JargonRuntimeException;
import org.irods.jargon.core.exception.NoResourceDefinedException;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.CollectionAndDataObjectListAndSearchAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSFileSystemSingletonWrapper;
import org.irods.jargon.core.pub.domain.ObjStat;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.MetaDataAndDomainData;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.StringUtil;
import org.modeshape.connector.filesystem.FileSystemConnector;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.federation.spi.ConnectorException;
import org.modeshape.jcr.federation.spi.DocumentChanges;
import org.modeshape.jcr.federation.spi.DocumentReader;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.federation.spi.PageKey;
import org.modeshape.jcr.federation.spi.Pageable;
import org.modeshape.jcr.federation.spi.WritableConnector;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.basic.BasicName;
import org.modeshape.jcr.value.basic.BasicSingleValueProperty;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.modeshape.jcr.value.binary.UrlBinaryValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ModeShape SPI connector for iRODS, based on the ModeShape file connector.
 * <p/>
 * Currently this connector processes collections and data objects, as well as
 * AVUs in read-only mode
 * 
 * @author Mike Conway - DICE (www.irods.org)
 * 
 */
public class IRODSWriteableConnector extends WritableConnector implements
		Pageable {

	public static final String AVU_UNIT_PROP = "avuUnit";
	public static final String AVU_VALUE_PROP = "avuValue";
	public static final String AVU_ATTRIBUTE_PROP = "avuAttribute";
	private static final String AVU_ID = "/avuId";
	// private static final String AVU_VALUE = "##[[[avuValue]]]##";
	// public static final String AVU_ATTRIBUTE = "##[[[avuAttribute]]]##";
	public static final String JCR_IRODS_IRODSOBJECT = "irods:irodsobject";
	public static final String JCR_IRODS_AVU_PROP = "irods:avu";

	public static final Logger log = LoggerFactory
			.getLogger(IRODSWriteableConnector.class);

	public static final String FILE_SEPARATOR = "/";
	public static final String DELIMITER = "/";
	public static final String NT_FOLDER = "nt:folder";
	public static final String NT_FILE = "nt:file";
	public static final String NT_RESOURCE = "nt:resource";
	public static final String MIX_MIME_TYPE = "mix:mimeType";
	public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
	public static final String JCR_DATA = "jcr:data";
	public static final String JCR_MIME_TYPE = "jcr:mimeType";
	public static final String JCR_ENCODING = "jcr:encoding";
	public static final String JCR_CREATED = "jcr:created";
	public static final String JCR_CREATED_BY = "jcr:createdBy";
	public static final String JCR_LAST_MODIFIED = "jcr:lastModified";
	public static final String JCR_LAST_MODIFIED_BY = "jcr:lastModified";
	public static final String JCR_CONTENT = "jcr:content";
	public static final String JCR_IRODS_AVU = "irods:avu";
	public static final String JCR_CONTENT_SUFFIX = DELIMITER + JCR_CONTENT;
	public static final int JCR_CONTENT_SUFFIX_LENGTH = JCR_CONTENT_SUFFIX
			.length();
	public static final String JCR_AVU_SUFFIX = DELIMITER + JCR_IRODS_AVU;
	public static final int JCR_AVU_SUFFIX_LENGTH = JCR_AVU_SUFFIX.length();

	/**
	 * The string path for a {@link File} object that represents the top-level
	 * directory accessed by this connector. This is set via reflection and is
	 * required for this connector.
	 */
	private String directoryPath;

	/**
	 * A string that is created in the
	 * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method that
	 * represents the absolute path to the {@link #directory}. This path is
	 * removed from an absolute path of a file to obtain the ID of the node.
	 */
	private String directoryPathWithTrailingSlash;
	private int directoryAbsolutePathLength;

	/**
	 * A boolean flag that specifies whether this connector should add the
	 * 'mix:mimeType' mixin to the 'nt:resource' nodes to include the
	 * 'jcr:mimeType' property. If set to <code>true</code>, the MIME type is
	 * computed immediately when the 'nt:resource' node is accessed, which might
	 * be expensive for larger files. This is <code>false</code> by default.
	 */
	private final boolean addMimeTypeMixin = false;

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
	 * The maximum number of children a folder will expose at any given time.
	 */
	private final int pageSize = 5000;

	/**
	 * The {@link FilenameFilter} implementation that is instantiated in the
	 * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method.
	 */
	private InclusionExclusionFilenameFilter filenameFilter;

	/**
	 * A boolean which determines whether for external binary values (i.e.
	 * {@link UrlBinaryValue}) the SHA1 is computed based on the content of the
	 * file itself or whether it's computed based on the URL string. This is
	 * {@code true} by default, but if the connector needs to deal with very
	 * large values it might be worth turning off.
	 */
	private final boolean contentBasedSha1 = true;

	private NamespaceRegistry registry;

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

		checkFieldNotNull(directoryPath, "directoryPath");

		directoryPathWithTrailingSlash = directoryPath + "/";
		directoryAbsolutePathLength = directoryPathWithTrailingSlash.length()
				- FILE_SEPARATOR.length(); // does NOT include the separtor

		// Initialize the filename filter ...
		filenameFilter = new InclusionExclusionFilenameFilter();
		if (exclusionPattern != null)
			filenameFilter.setExclusionPattern(exclusionPattern);
		if (inclusionPattern != null)
			filenameFilter.setInclusionPattern(inclusionPattern);

		log.info("getting documentMapper");
		log.info("initialized");

	}

	/**
	 * Get the namespace registry.
	 * 
	 * @return the namespace registry; never null
	 */
	NamespaceRegistry registry() {
		return registry;
	}

	/**
	 * Utility method for determining if the supplied identifier is for the
	 * "jcr:content" child node of a file. * Subclasses may override this method
	 * to change the format of the identifiers, but in that case should also
	 * override the {@link #fileFor(String)}, {@link #isRoot(String)}, and
	 * {@link #idFor(File)} methods.
	 * 
	 * @param id
	 *            the identifier; may not be null
	 * @return true if the identifier signals the "jcr:content" child node of a
	 *         file, or false otherwise
	 * @see #isRoot(String)
	 * @see #fileFor(String)
	 * @see #idFor(File)
	 */
	protected boolean isContentNode(final String id) {
		return id.endsWith(JCR_CONTENT_SUFFIX);
	}

	protected boolean isAvuNode(final String id) {
		return id.endsWith(JCR_AVU_SUFFIX);
	}

	/**
	 * Utility method for obtaining the {@link File} object that corresponds to
	 * the supplied identifier. Subclasses may override this method to change
	 * the format of the identifiers, but in that case should also override the
	 * {@link #isRoot(String)}, {@link #isContentNode(String)}, and
	 * {@link #idFor(File)} methods.
	 * 
	 * @param id
	 *            the identifier; may not be null
	 * @return the File object for the given identifier
	 * @param closeInFinally
	 *            <code>boolean</code> that indicates whether the irods
	 *            connection should be closed in the finally. Call with true
	 *            when fileFor is invoked in the public API for now, don't when
	 *            another method will handle this. Prevents unnecessary twerking
	 *            of the connection.
	 * @see #isRoot(String)
	 * @see #isContentNode(String)
	 * @see #idFor(File)
	 */
	protected File fileFor(String id, final boolean closeInFinally) {
		log.info("fileFor()");
		log.info("id:{}", id);

		assert id.startsWith(DELIMITER);
		if (id.endsWith(DELIMITER)) {
			id = id.substring(0, id.length() - DELIMITER.length());
		}

		if (isContentNode(id)) {
			id = id.substring(0, id.length() - JCR_CONTENT_SUFFIX_LENGTH);
		} else if (isAvuNode(id)) {

			// is an avu, strip out the appended id
			int idxOfAttr = id.indexOf(AVU_ID);

			id = id.substring(0, idxOfAttr);
		}

		try {
			return (File) IRODSFileSystemSingletonWrapper.instance()
					.getIRODSAccessObjectFactory()
					.getIRODSFileFactory(getIrodsAccount())
					.instanceIRODSFile(directoryPathWithTrailingSlash, id);
		} catch (JargonException e) {
			log.error("error getting irods file", e);
			throw new ConnectorException(e);
		} finally {
			if (closeInFinally) {
				try {
					IRODSFileSystemSingletonWrapper.instance()
							.getIRODSAccessObjectFactory()
							.closeSessionAndEatExceptions();
				} catch (JargonException e) {
					// ignored

				}
			}
		}

	}

	/**
	 * Utility method for determining if the node identifier is the identifier
	 * of the root node in this external source. Subclasses may override this
	 * method to change the format of the identifiers, but in that case should
	 * also override the {@link #fileFor(String)},
	 * {@link #isContentNode(String)}, and {@link #idFor(File)} methods.
	 * 
	 * @param id
	 *            the identifier; may not be null
	 * @return true if the identifier is for the root of this source, or false
	 *         otherwise
	 * @see #isContentNode(String)
	 * @see #fileFor(String)
	 * @see #idFor(File)
	 */
	protected boolean isRoot(final String id) {
		return DELIMITER.equals(id);
	}

	/**
	 * Utility method for determining the node identifier for the supplied file.
	 * Subclasses may override this method to change the format of the
	 * identifiers, but in that case should also override the
	 * {@link #fileFor(String)}, {@link #isContentNode(String)}, and
	 * {@link #isRoot(String)} methods.
	 * 
	 * @param file
	 *            the file; may not be null
	 * @return the node identifier; never null
	 * @see #isRoot(String)
	 * @see #isContentNode(String)
	 * @see #fileFor(String)
	 */
	protected String idFor(final File file) {
		String path = file.getAbsolutePath();
		if (!path.startsWith(directoryPathWithTrailingSlash)) {
			if (this.directoryPath.equals(path)) {
				// This is the root
				return DELIMITER;
			}
			String msg = JcrI18n.fileConnectorNodeIdentifierIsNotWithinScopeOfConnector
					.text(getSourceName(), directoryPath, path);
			throw new DocumentStoreException(path, msg);
		}
		String id = path.substring(directoryAbsolutePathLength);
		id = id.replaceAll(Pattern.quote(FILE_SEPARATOR), DELIMITER);
		assert id.startsWith(DELIMITER);

		log.info("idFor is:{}", id);

		return id;
	}

	/**
	 * Utility method for creating a {@link BinaryValue} for the given
	 * {@link File} object. Subclasses should rarely override this method.
	 * 
	 * @param file
	 *            the file; may not be null
	 * @return the BinaryValue; never null
	 */
	protected ExternalBinaryValue binaryFor(final File file) {

		log.info("binaryFor()");
		assert file != null;
		log.info("file:{}", file);

		try {
			return createBinaryValue(file);
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Utility method to create a {@link BinaryValue} object for the given file.
	 * Subclasses should rarely override this method, since the
	 * {@link UrlBinaryValue} will be applicable in most situations.
	 * 
	 * @param file
	 *            the file for which the {@link BinaryValue} is to be created;
	 *            never null
	 * @return the binary value; never null
	 * @throws IOException
	 *             if there is an error creating the value
	 */
	protected ExternalBinaryValue createBinaryValue(final File file)
			throws IOException {

		log.info("createBinaryFile()");
		assert file != null;
		log.info("file:{}", file);

		URL content = createUrlForFile(file);
		try {
			return new IRODSBinaryValue(sha1(file), getSourceName(), content,
					file.length(), file.getName(), getMimeTypeDetector(),
					IRODSFileSystemSingletonWrapper.instance()
							.getIRODSAccessObjectFactory(),
					this.getIrodsAccount());
		} catch (JargonException e) {
			throw new ConnectorException(e);
		}
	}

	/**
	 * Computes the SHA1 for the given file. By default, this method will look
	 * at the {@link FileSystemConnector#contentBasedSha1()} flag and either
	 * take the URL of the file (using @see java.util.File#toURI().toURL() and
	 * return the SHA1 of the URL string or return the SHA1 of the entire file
	 * content.
	 * 
	 * @param file
	 *            a {@link File} instance; never null
	 * @return the SHA1 of the file.
	 */
	protected String sha1(final File file) {
		try {
			if (contentBasedSha1()) {
				log.info("content based SHA1, computing for {} ...",
						file.getAbsolutePath());

				DataObjectAO dataObjectAO = IRODSFileSystemSingletonWrapper
						.instance().getIRODSAccessObjectFactory()
						.getDataObjectAO(getIrodsAccount());

				byte[] hash = dataObjectAO
						.computeSHA1ChecksumOfIrodsFileByReadingDataFromStream(file
								.getAbsolutePath());
				return StringUtil.getHexString(hash);
			} else {
				return SecureHash.sha1(createUrlForFile(file).toString());
			}
		} catch (Exception e) {
			throw new ConnectorException(e);
		}
	}

	/**
	 * Construct a {@link URL} object for the given file, to be used within the
	 * {@link Binary} value representing the "jcr:data" property of a
	 * 'nt:resource' node.
	 * <p>
	 * Subclasses can override this method to transform the URL into something
	 * different. For example, if the files are being served by a web server,
	 * the overridden method might transform the file-based URL into the
	 * corresponding HTTP-based URL.
	 * </p>
	 * 
	 * @param file
	 *            the file for which the URL is to be created; never null
	 * @return the URL for the file; never null
	 * @throws IOException
	 *             if there is an error creating the URL
	 */
	protected URL createUrlForFile(final File file) throws IOException {
		return ((IRODSFile) file).toFileBasedURL();
	}

	protected File createFileForUrl(final URL url) throws URISyntaxException {

		log.info("createFileForUrl()");

		if (url == null) {
			throw new IllegalArgumentException("Null url");
		}

		log.info("url:{}", url);

		try {

			IRODSFileFactory irodsFileFactory = IRODSFileSystemSingletonWrapper
					.instance().getIRODSAccessObjectFactory()
					.getIRODSFileFactory(getIrodsAccount());

			String stringFormOfUrl = url.getPath();

			log.info("string form:{}", stringFormOfUrl);

			IRODSFile irodsFile = irodsFileFactory
					.instanceIRODSFile(stringFormOfUrl);
			return (File) irodsFile;

		} catch (JargonException e) {
			throw new ConnectorException(e);
		} finally {
			try {
				IRODSFileSystemSingletonWrapper.instance()
						.getIRODSAccessObjectFactory()
						.closeSessionAndEatExceptions();
			} catch (JargonException e) {
				// ignore here
			}
		}

	}

	protected boolean contentBasedSha1() {
		return contentBasedSha1;
	}

	/**
	 * Utility method to determine if the file is excluded by the
	 * inclusion/exclusion filter.
	 * 
	 * @param file
	 *            the file
	 * @return true if the file is excluded, or false if it is to be included
	 */
	protected boolean isExcluded(final File file) {
		return !filenameFilter.accept(file.getParentFile(), file.getName());
	}

	/**
	 * Utility method to ensure that the file is writable by this connector.
	 * 
	 * @param id
	 *            the identifier of the node
	 * @param file
	 *            the file
	 * @throws DocumentStoreException
	 *             if the file is expected to be writable but is not or is
	 *             excluded, or if the connector is readonly
	 */
	protected void checkFileNotExcluded(final String id, final File file) {
		if (isExcluded(file)) {
			String msg = JcrI18n.fileConnectorCannotStoreFileThatIsExcluded
					.text(getSourceName(), id, file.getAbsolutePath());
			throw new DocumentStoreException(id, msg);
		}
	}

	@Override
	public boolean hasDocument(final String id) {
		return fileFor(id, true).exists();
	}

	@Override
	public Document getDocumentById(final String id) {

		log.info("getDocumentById()");

		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}

		log.info("id:{}", id);

		/*
		 * This patches an apparent bug in the webdav servlet that tends to
		 * double-include // chars in folder links
		 */

		String correctedId = id.replaceAll("//", "/");
		log.info("correctedId:{}", correctedId);

		try {

			boolean isRoot = isRoot(correctedId);
			boolean isResource = isContentNode(correctedId);
			boolean isAvu = isAvuNode(correctedId);
			DocumentWriter writer = null;

			File file = fileFor(correctedId, false);
			if (isExcluded(file) || !file.exists()) {
				return null;
			}
			File parentFile = file.getParentFile();

			/*
			 * Note that I won't be asking for an avu unless the parent file or
			 * folder was 'included'
			 */
			if (isAvu) {
				log.info("return a doc for an avu");
				writer = getDocumentForAvu(id);
			} else if (isResource) {
				log.info("treat as resource...");
				writer = newDocument(correctedId);
				BinaryValue binaryValue = binaryFor(file);
				writer.setPrimaryType(NT_RESOURCE);
				writer.addProperty(JCR_DATA, binaryValue);
				if (addMimeTypeMixin) {
					String mimeType = null;
					String encoding = null; // We don't really know this
					try {
						mimeType = binaryValue.getMimeType();
					} catch (Throwable e) {
						getLogger().error(e, JcrI18n.couldNotGetMimeType,
								getSourceName(), correctedId, e.getMessage());
					}
					writer.addProperty(JCR_ENCODING, encoding);
					writer.addProperty(JCR_MIME_TYPE, mimeType);
				}
				writer.addProperty(JCR_LAST_MODIFIED, factories()
						.getDateFactory().create(file.lastModified()));
				writer.addProperty(JCR_LAST_MODIFIED_BY, null); // ignored

				// make these binary not queryable. If we really want to query
				// them,
				// we need to switch to external binaries
				writer.setNotQueryable();
				parentFile = file;

			} else if (file.isFile()) {
				log.info("treat as file");
				writer = newDocument(correctedId);
				writer.setPrimaryType(NT_FILE);
				writer.addMixinType(JCR_IRODS_IRODSOBJECT);

				writer.addProperty(JCR_CREATED, factories().getDateFactory()
						.create(file.lastModified()));
				writer.addProperty(JCR_CREATED_BY, null); // ignored
				String childId = isRoot ? JCR_CONTENT_SUFFIX : correctedId
						+ JCR_CONTENT_SUFFIX;
				writer.addChild(childId, JCR_CONTENT);

				log.info("get avus and add as props to the file");
				addAvuChildrenForDataObject(file.getAbsolutePath(), id, writer);

			} else {
				log.info("treat as folder..");
				writer = newFolderWriter(correctedId, file, 0);
			}

			if (!isRoot) {
				// Set the reference to the parent ...
				String parentId = idFor(parentFile);
				writer.setParents(parentId);
			}

			// Add the 'mix:mixinType' mixin; if other mixins are stored in the
			// extra properties, this will append ...
			if (addMimeTypeMixin) {
				writer.addMixinType(MIX_MIME_TYPE);
			}

			// Return the document ...
			return writer.document();

		} finally {
			try {
				IRODSFileSystemSingletonWrapper.instance()
						.getIRODSAccessObjectFactory()
						.closeSessionAndEatExceptions();
			} catch (JargonException e) {
				// ignored
			}
		}

	}

	/**
	 * For a given id, which is a stringified file path along with the delimited
	 * attrib and value, return the JCR document that holds that AVU with the
	 * AVU values as properties of the document of type irods:avu.
	 * 
	 * @param id
	 *            <code>String</code> with the encoded path,attrib,value data
	 * @return {@link DocumentWriter} that represents the child AVU
	 */
	private DocumentWriter getDocumentForAvu(final String id) {

		log.info("getDocumentForAvu()");
		assert id != null && !id.isEmpty();
		DocumentWriter writer = newDocument(id);

		log.info("id:{}", id);

		String myId = id.substring(0, id.length() - JCR_AVU_SUFFIX_LENGTH);
		// break up id into attr and value and path using delims
		int idxAttr = myId.indexOf(AVU_ID);

		if (idxAttr == -1) {
			throw new DocumentStoreException(
					"avu does not have expected id  delim");
		}

		String filePath = myId.substring(0, idxAttr);
		log.info("path for avu file:{}", filePath);

		int attribId;
		try {
			attribId = Integer.parseInt(myId.substring(idxAttr
					+ AVU_ID.length()));
		} catch (NumberFormatException e) {
			log.error("attrib id is not a number", e);
			throw new DocumentStoreException(
					"attribute id is missing from avu id", e);
		}

		StringBuilder sb = new StringBuilder();
		sb.append(this.directoryPathWithTrailingSlash);
		sb.append(filePath.substring(1));

		String myFilePath = sb.toString();

		/*
		 * this is sort of stupid, but find the AVU. Decide whether to have a
		 * query by attrib/value in Jargon
		 */

		try {
			MetaDataAndDomainData metadata;
			log.info("getting objstat for this path");
			CollectionAndDataObjectListAndSearchAO collectionAndDataObjectListAndSearchAO = IRODSFileSystemSingletonWrapper
					.instance()
					.getIRODSAccessObjectFactory()
					.getCollectionAndDataObjectListAndSearchAO(
							getIrodsAccount());
			ObjStat objStat = collectionAndDataObjectListAndSearchAO
					.retrieveObjectStatForPath(myFilePath);

			if (objStat.isSomeTypeOfCollection()) {
				CollectionAO collectionAO = IRODSFileSystemSingletonWrapper
						.instance().getIRODSAccessObjectFactory()
						.getCollectionAO(getIrodsAccount());

				metadata = collectionAO.findMetadataValueForCollectionById(
						objStat, attribId);

			} else {
				DataObjectAO dataObjectAO = IRODSFileSystemSingletonWrapper
						.instance().getIRODSAccessObjectFactory()
						.getDataObjectAO(getIrodsAccount());
				metadata = dataObjectAO.findMetadataValueForDataObjectById(
						objStat, attribId);
			}

			addAvuMetadataAsProperty(writer, metadata);

			log.info("added AVU as properties!");
			return writer;

		} catch (JargonException e) {
			log.error("exception getting collection metadata", e);
			throw new DocumentStoreException("error getting AVU metadata", e);
		}
	}

	/**
	 * Given a child document to contain the AVU, add the AVU values as
	 * properties in this child object
	 * 
	 * @param writer
	 *            {@link DocumentWriter} that is creating the irods:avu document
	 * @param metadataValue
	 *            {@link MetaDataAndDomainData} that represents the iRODS AVU
	 */
	private void addAvuMetadataAsProperty(DocumentWriter writer,
			MetaDataAndDomainData metadataValue) {
		Map<Name, Property> properties = new HashMap<Name, Property>();

		writer.setPrimaryType(JCR_IRODS_AVU);

		Property property = new BasicSingleValueProperty(new BasicName(
				"http://www.irods.org/jcr/irods/1.0", AVU_ATTRIBUTE_PROP),
				metadataValue.getAvuAttribute());

		properties.put(property.getName(), property);

		property = new BasicSingleValueProperty(new BasicName(
				"http://www.irods.org/jcr/irods/1.0", AVU_VALUE_PROP),
				metadataValue.getAvuValue());
		properties.put(property.getName(), property);

		property = new BasicSingleValueProperty(new BasicName(
				"http://www.irods.org/jcr/irods/1.0", AVU_UNIT_PROP),
				metadataValue.getAvuUnit());
		properties.put(property.getName(), property);

		writer.addProperties(properties);
	}

	/**
	 * Create a document writer for a folder, including it's file and avu
	 * children
	 * 
	 * @param id
	 * @param file
	 * @param offset
	 * @return
	 */
	private DocumentWriter newFolderWriter(final String id, final File file,
			final int offset) {

		log.info("newFolderWriter()");

		assert id != null;
		assert !id.isEmpty();
		assert file != null;

		log.info("id:{}", id);
		log.info("file:{}", file.getAbsolutePath());

		boolean root = isRoot(id);
		DocumentWriter writer = newDocument(id);
		writer.setPrimaryType(NT_FOLDER);
		writer.addMixinType(JCR_IRODS_IRODSOBJECT);
		writer.addProperty(JCR_CREATED,
				factories().getDateFactory().create(file.lastModified()));
		writer.addProperty(JCR_CREATED_BY, null); // ignored

		log.info("adding AVU children for the collection");
		addAvuChildrenForCollection(file.getAbsolutePath(), id, writer);
		log.info("AVU children added");

		File[] children = file.listFiles(filenameFilter);
		long totalChildren = 0;
		int nextOffset = 0;
		log.info("parent is:{}", file.getAbsolutePath());
		for (int i = 0; i < children.length; i++) {
			File child = children[i];
			// Only include as a child if we can access and read the file.
			// Permissions might prevent us from
			// reading the file, and the file might not exist if it is a broken
			// symlink (see MODE-1768 for details).
			if (child.exists() && child.canRead()
					&& (child.isFile() || child.isDirectory())) {
				// we need to count the total accessible children
				totalChildren++;
				// only add a child if it's in the current page
				if (i >= offset && i < offset + pageSize) {
					// We use identifiers that contain the file/directory name
					// ...
					String childName = child.getName();
					String childId = root ? DELIMITER + childName : id
							+ DELIMITER + childName;

					writer.addChild(childId, childName);

					log.info("added child directory with name:{}", childName);

				}
				nextOffset = i + 1;
			}
		}

		// if there are still accessible children add the next page
		if (nextOffset < totalChildren) {
			writer.addPage(id, nextOffset, pageSize, totalChildren);
		}

		return writer;
	}

	@Override
	public String getDocumentId(final String path) {
		String id = path; // this connector treats the ID as the path

		try {
			File file = fileFor(id, false);
			return file.exists() ? id : null;

		} finally {
			try {
				IRODSFileSystemSingletonWrapper.instance()
						.getIRODSAccessObjectFactory()
						.closeSessionAndEatExceptions();
			} catch (JargonException e) {
				// ignored
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#getDocumentPathsById(java.
	 * lang.String)
	 */
	@Override
	public Collection<String> getDocumentPathsById(final String id) {
		// this connector treats the ID as the path
		return Collections.singletonList(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#getBinaryValue(java.lang.String
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
			File f = createFileForUrl(new URL(id));
			return binaryFor(f);
		} catch (IOException e) {
			throw new DocumentStoreException(id, e);
		} catch (URISyntaxException e) {
			throw new DocumentStoreException(id, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#removeDocument(java.lang.String
	 * )
	 */
	@Override
	public boolean removeDocument(final String id) {

		try {

			File file = fileFor(id, false);
			checkFileNotExcluded(id, file);
			// Remove the extra properties at the old location ...
			extraPropertiesStore().removeProperties(id);
			// Now remove the file (if it is there) ...
			if (!file.exists()) {
				return false;
			}

			IRODSFile irodsFile = (IRODSFile) file;
			irodsFile.delete();

			return true;
		} finally {
			try {
				IRODSFileSystemSingletonWrapper.instance()
						.getIRODSAccessObjectFactory()
						.closeSessionAndEatExceptions();
			} catch (JargonException e) {
				// ignored
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#storeDocument(org.infinispan
	 * .schematic.document.Document)
	 */
	@Override
	public void storeDocument(final Document document) {
		// Create a new directory or file described by the document ...
		try {
			DocumentReader reader = readDocument(document);
			String id = reader.getDocumentId();
			File file = fileFor(id, false);
			checkFileNotExcluded(id, file);
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			if (!parent.canWrite()) {
				String parentPath = parent.getAbsolutePath();
				String msg = JcrI18n.fileConnectorCannotWriteToDirectory.text(
						getSourceName(), getClass(), parentPath);
				throw new DocumentStoreException(id, msg);
			}
			String primaryType = reader.getPrimaryTypeName();

			Map<Name, Property> properties = reader.getProperties();
			ExtraProperties extraProperties = extraPropertiesFor(id, false);
			extraProperties.addAll(properties).except(JCR_PRIMARY_TYPE,
					JCR_CREATED, JCR_LAST_MODIFIED, JCR_DATA);
			try {
				if (NT_FILE.equals(primaryType)) {
					file.createNewFile();
				} else if (NT_FOLDER.equals(primaryType)) {
					file.mkdirs();
				} else if (isContentNode(id)) {
					Property content = properties.get(JcrLexicon.DATA);
					BinaryValue binary = factories().getBinaryFactory().create(
							content.getFirstValue());
					IRODSFile irodsFile = (IRODSFile) file;

					OutputStream ostream = new BufferedOutputStream(
							IRODSFileSystemSingletonWrapper
									.instance()
									.getIRODSAccessObjectFactory()
									.getIRODSFileFactory(getIrodsAccount())
									.instanceSessionClosingIRODSFileOutputStream(
											irodsFile));
					IoUtil.write(binary.getStream(), ostream);
					if (!NT_RESOURCE.equals(primaryType)) {
						// This is the "jcr:content" child, but the primary type
						// is
						// non-standard so record it as an extra property
						extraProperties.add(properties
								.get(JcrLexicon.PRIMARY_TYPE));
					}
				}
				extraProperties.save();
			} catch (RepositoryException e) {
				throw new DocumentStoreException(id, e);
			} catch (IOException e) {
				throw new DocumentStoreException(id, e);
			} catch (NoResourceDefinedException e) {
				throw new DocumentStoreException(id, e);
			} catch (JargonException e) {
				throw new DocumentStoreException(id, e);
			}
		} finally {
			try {
				IRODSFileSystemSingletonWrapper.instance()
						.getIRODSAccessObjectFactory()
						.closeSessionAndEatExceptions();
			} catch (JargonException e) {
				// ignore
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#newDocumentId(java.lang.String
	 * , org.modeshape.jcr.value.Name, org.modeshape.jcr.value.Name)
	 */
	@Override
	public String newDocumentId(final String parentId,
			final Name newDocumentName, final Name newDocumentPrimaryType) {
		StringBuilder id = new StringBuilder(parentId);
		if (!parentId.endsWith(DELIMITER)) {
			id.append(DELIMITER);
		}

		// We're only using the name to check, which can be a bit dangerous if
		// users don't follow the JCR conventions.
		// However, it matches what "isContentNode(...)" does.
		String childNameStr = getContext().getValueFactories()
				.getStringFactory().create(newDocumentName);
		if (JCR_CONTENT.equals(childNameStr)) {
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
			if (!StringUtil.isBlank(newDocumentName.getNamespaceUri())) {
				// the FS connector does not support namespaces in names
				String ns = newDocumentName.getNamespaceUri();
				getLogger().warn(JcrI18n.fileConnectorNamespaceIgnored,
						getSourceName(), ns, id, childNameStr, parentId);
			}
		}
		return id.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#updateDocument(org.modeshape
	 * .jcr.federation.spi.DocumentChanges)
	 */
	@Override
	public void updateDocument(final DocumentChanges documentChanges) {

		log.info("updateDocument()");

		assert documentChanges != null;

		log.info("documentChanges:{}", documentChanges);

		try {

			String id = documentChanges.getDocumentId();
			log.info("id for doc changes:{}", id);
			Document document = documentChanges.getDocument();
			DocumentReader reader = readDocument(document);
			File file = fileFor(id, false);
			log.info("file for id:{}", file);
			String idOrig = id;

			// if we're dealing with the root of the connector, we can't process
			// any
			// moves/removes because that would go "outside" the
			// connector scope
			if (!isRoot(id)) {

				log.info("not root....");

				String parentId = reader.getParentIds().get(0);
				log.info("parent id:{}", parentId);
				File parent = file.getParentFile();
				log.info("parent:{}", parent);
				String newParentId = idFor(parent);
				log.info("new parentId:{}", newParentId);

				if (!parentId.equals(newParentId)) {
					// The node has a new parent (via the 'update' method),
					// meaning
					// it was moved ...

					log.info("node was moved...");

					File newParent = fileFor(parentId, false);
					log.info("file for new parent:{}", newParent);

					File newFile;
					try {
						newFile = (File) IRODSFileSystemSingletonWrapper
								.instance().getIRODSAccessObjectFactory()
								.getIRODSFileFactory(getIrodsAccount())
								.instanceIRODSFile(newParent, file.getName());

						log.info("new file:{}", newFile);

					} catch (JargonException e) {
						throw new DocumentStoreException(id, e);

					}

					log.info("renaming....");
					((IRODSFile) file).renameTo((IRODSFile) newFile);
					log.info("rename done to :{}", newFile);

					if (!parent.exists()) {
						parent.mkdirs(); // in case they were removed since we
											// created them ...
					}
					if (!parent.canWrite()) {
						String parentPath = newParent.getAbsolutePath();
						String msg = JcrI18n.fileConnectorCannotWriteToDirectory
								.text(getSourceName(), getClass(), parentPath);
						throw new DocumentStoreException(id, msg);
					}
					parent = newParent;
					// Remove the extra properties at the old location ...
					extraPropertiesStore().removeProperties(id);
					// Set the id to the new location ...
					id = idFor(newFile);
				} else {
					// It is the same parent as before ...
					if (!parent.exists()) {
						parent.mkdirs(); // in case they were removed since we
											// created them ...
					}
					if (!parent.canWrite()) {
						String parentPath = parent.getAbsolutePath();
						String msg = JcrI18n.fileConnectorCannotWriteToDirectory
								.text(getSourceName(), getClass(), parentPath);
						throw new DocumentStoreException(id, msg);
					}
				}
			}

			log.info("processing children renames...");

			// children renames have to be processed in the parent
			DocumentChanges.ChildrenChanges childrenChanges = documentChanges
					.getChildrenChanges();
			Map<String, Name> renamedChildren = childrenChanges.getRenamed();
			for (String renamedChildId : renamedChildren.keySet()) {

				renameChildrenFiles(id, file, renamedChildren, renamedChildId);
			}

			String primaryType = reader.getPrimaryTypeName();
			Map<Name, Property> properties = reader.getProperties();
			id = idOrig;
			ExtraProperties extraProperties = extraPropertiesFor(id, true);
			extraProperties.addAll(properties).except(JCR_PRIMARY_TYPE,
					JCR_CREATED, JCR_LAST_MODIFIED, JCR_DATA);
			try {
				if (NT_FILE.equals(primaryType)) {
					file.createNewFile();
				} else if (NT_FOLDER.equals(primaryType)) {
					file.mkdir();
				} else if (isContentNode(id)) {
					Property content = reader.getProperty(JCR_DATA);
					BinaryValue binary = factories().getBinaryFactory().create(
							content.getFirstValue());
					IRODSFile irodsFile = (IRODSFile) file;

					OutputStream ostream;
					try {
						ostream = new BufferedOutputStream(
								IRODSFileSystemSingletonWrapper
										.instance()
										.getIRODSAccessObjectFactory()
										.getIRODSFileFactory(getIrodsAccount())
										.instanceSessionClosingIRODSFileOutputStream(
												irodsFile));
					} catch (NoResourceDefinedException e) {
						throw new DocumentStoreException(id, e);
					} catch (JargonException e) {
						throw new DocumentStoreException(id, e);
					}
					IoUtil.write(binary.getStream(), ostream);
					if (!NT_RESOURCE.equals(primaryType)) {
						// This is the "jcr:content" child, but the primary type
						// is
						// non-standard so record it as an extra property
						extraProperties.add(properties
								.get(JcrLexicon.PRIMARY_TYPE));
					}
				}
				extraProperties.save();
			} catch (RepositoryException e) {
				throw new DocumentStoreException(id, e);
			} catch (IOException e) {
				throw new DocumentStoreException(id, e);
			}
		} finally {
			try {
				IRODSFileSystemSingletonWrapper.instance()
						.getIRODSAccessObjectFactory()
						.closeSessionAndEatExceptions();
			} catch (JargonException e) {
				// ignored
			}
		}
	}

	private File renameChildrenFiles(String id, File file,
			Map<String, Name> renamedChildren, String renamedChildId) {
		log.info("renamed child id:{}", renamedChildId);

		File child = fileFor(renamedChildId, false);
		log.info("child:{}", child);
		Name newName = renamedChildren.get(renamedChildId);
		String newNameStr = getContext().getValueFactories().getStringFactory()
				.create(newName);
		File renamedChild;
		try {
			renamedChild = (File) IRODSFileSystemSingletonWrapper.instance()
					.getIRODSAccessObjectFactory()
					.getIRODSFileFactory(getIrodsAccount())
					.instanceIRODSFile(file, newNameStr);
			log.info("renamedChild:{}", renamedChild);
		} catch (JargonException e) {
			throw new DocumentStoreException(id, e);
		}

		if (!((IRODSFile) child).renameTo((IRODSFile) renamedChild)) {
			getLogger().debug("Cannot rename {0} to {1}", child, renamedChild);
		}
		return renamedChild;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Pageable#getChildren(org.modeshape.jcr
	 * .federation.spi.PageKey)
	 */
	@Override
	public Document getChildren(final PageKey pageKey) {
		try {

			String parentId = pageKey.getParentId();
			File folder = fileFor(parentId, false);
			assert folder.isDirectory();
			if (!folder.canRead()) {
				getLogger().debug("Cannot read the {0} folder",
						folder.getAbsolutePath());
				return null;
			}
			return newFolderWriter(parentId, folder, pageKey.getOffsetInt())
					.document();
		} finally {
			try {
				IRODSFileSystemSingletonWrapper.instance()
						.getIRODSAccessObjectFactory()
						.closeSessionAndEatExceptions();
			} catch (JargonException e) {
				// ignored
			}
		}
	}

	/**
	 * For a given collection, create a set of child documents of type irods:avu
	 * that represents the AVU metadata
	 * 
	 * @param path
	 *            <code>String</code> with the path to the iRODS collection
	 * @param writer
	 *            {@link DocumentWriter} for the parent collection
	 */
	private void addAvuChildrenForCollection(final String path,
			final String id, final DocumentWriter writer) {

		assert path != null && !path.isEmpty();
		List<MetaDataAndDomainData> metadatas;
		try {

			File fileForProps;

			fileForProps = (File) IRODSFileSystemSingletonWrapper.instance()
					.getIRODSAccessObjectFactory()
					.getIRODSFileFactory(this.getIrodsAccount())
					.instanceIRODSFile(path);

			log.info("file abs path to search for collection AVUs:{}",
					fileForProps.getAbsolutePath());

			CollectionAO collectionAO = IRODSFileSystemSingletonWrapper
					.instance().getIRODSAccessObjectFactory()
					.getCollectionAO(getIrodsAccount());

			metadatas = collectionAO.findMetadataValuesForCollection(
					fileForProps.getAbsolutePath(), 0);

			addChildrenForEachAvu(writer, metadatas, fileForProps, id);

		} catch (FileNotFoundException e) {
			log.error("fnf retrieving avus", e);
			throw new DocumentStoreException(
					"file not found for retrieving avus", e);
		} catch (JargonException e) {
			log.error("jargon exception retrieving avus", e);
			throw new DocumentStoreException(
					"jargon exception retrieving avus", e);
		} catch (JargonQueryException e) {
			log.error("jargon query exception retrieving avus", e);
			throw new DocumentStoreException(
					"jargon query exception retrieving avus", e);
		}
	}

	/**
	 * For a given data object, create a set of child documents of type
	 * irods:avu that represents the AVU metadata
	 * 
	 * @param path
	 *            <code>String</code> with the path to the iRODS collection
	 * @param writer
	 *            {@link DocumentWriter} for the parent collection
	 */
	private void addAvuChildrenForDataObject(final String path,
			final String id, final DocumentWriter writer) {

		assert path != null && !path.isEmpty();

		List<MetaDataAndDomainData> metadatas;
		try {

			File fileForProps;

			fileForProps = (File) IRODSFileSystemSingletonWrapper.instance()
					.getIRODSAccessObjectFactory()
					.getIRODSFileFactory(this.getIrodsAccount())
					.instanceIRODSFile(path);

			log.info("file abs path to search for collection AVUs:{}",
					fileForProps.getAbsolutePath());

			DataObjectAO dataObjectAO = IRODSFileSystemSingletonWrapper
					.instance().getIRODSAccessObjectFactory()
					.getDataObjectAO(getIrodsAccount());

			metadatas = dataObjectAO
					.findMetadataValuesForDataObject(fileForProps
							.getAbsolutePath());

			addChildrenForEachAvu(writer, metadatas, fileForProps, id);

		} catch (FileNotFoundException e) {
			log.error("fnf retrieving avus", e);
			throw new DocumentStoreException(
					"file not found for retrieving avus", e);
		} catch (JargonException e) {
			log.error("jargon exception retrieving avus", e);
			throw new DocumentStoreException(
					"jargon exception retrieving avus", e);
		}
	}

	/**
	 * For each AVU in the list, add as a child of the given document
	 * 
	 * @param writer
	 * @param metadatas
	 * @param fileForProps
	 */
	private void addChildrenForEachAvu(final DocumentWriter writer,
			List<MetaDataAndDomainData> metadatas, File fileForProps,
			final String id) {
		StringBuilder sb;
		for (MetaDataAndDomainData metadata : metadatas) {
			sb = new StringBuilder();
			sb.append(id);
			sb.append(AVU_ID);
			sb.append(metadata.getAvuId());
			String childName = sb.toString();
			sb.append(JCR_AVU_SUFFIX);
			String childId = sb.toString();
			log.info("adding avu child with childName:{}", childName);
			log.info("avu childId:{}", childId);
			writer.addChild(childId, childName);
		}
	}

	/*
	 * FIXME: shim for account, figure out auth and account handling
	 */
	private IRODSAccount getIrodsAccount() {
		IRODSAccount irodsAccount;
		try {
			irodsAccount = IRODSAccount.instance("fedZone1", 1247, "test1",
					"test", "", "fedZone1", "");
			// irodsAccount = IRODSAccount.instance("localhost", 1247,
			// "test1", "test", "", "test1", "");
		} catch (JargonException e) {
			throw new JargonRuntimeException("unable to create irodsAccount", e);
		}

		return irodsAccount;
	}

}
