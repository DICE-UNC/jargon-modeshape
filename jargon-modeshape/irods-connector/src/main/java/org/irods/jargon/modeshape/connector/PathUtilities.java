/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.jcr.NamespaceRegistry;

import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSFileSystemSingletonWrapper;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.modeshape.connector.nodetypes.NodeTypeAndId;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.StringUtil;
import org.modeshape.connector.filesystem.FileSystemConnector;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.spi.federation.ConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for understanding and formatting document ids between ModeShape and
 * IRODS
 * 
 * @author Mike Conway - DICE
 * 
 */
public class PathUtilities {

	public static final String DELIMITER = "/";
	public static final String MIX_MIME_TYPE = "mix:mimeType";
	public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
	public static final String JCR_DATA = "jcr:data";
	public static final String JCR_MIME_TYPE = "jcr:mimeType";
	public static final String JCR_ENCODING = "jcr:encoding";
	public static final String JCR_CREATED = "jcr:created";
	public static final String JCR_CREATED_BY = "jcr:createdBy";
	public static final String JCR_LAST_MODIFIED = "jcr:lastModified";
	public static final String JCR_LAST_MODIFIED_BY = "jcr:lastModified";
	public static final String NT_FOLDER = "nt:folder";
	public static final String NT_FILE = "nt:file";
	public static final String NT_RESOURCE = "nt:resource";
	public static final String JCR_CONTENT = "jcr:content";
	public static final String JCR_CONTENT_SUFFIX = DELIMITER + JCR_CONTENT;
	public static final String JCR_IRODS_IRODSOBJECT = "irods:irodsobject";
	public static final String JCR_IRODS_AVU_PROP = "irods:avu";
	public static final int DELIMITER_LENGTH = DELIMITER.length();
	public static final String JCR_IRODS_AVU = "irods:avu";
	public static final String JCR_AVU_SUFFIX = DELIMITER + JCR_IRODS_AVU;
	public static final int JCR_AVU_SUFFIX_LENGTH = JCR_AVU_SUFFIX.length();
	public static final int JCR_CONTENT_SUFFIX_LENGTH = JCR_CONTENT_SUFFIX
			.length();
	private static final String AVU_ID = "/avuId";

	/**
	 * The string path for a {@link File} object that represents the top-level
	 * directory accessed by this connector. This is set via reflection and is
	 * required for this connector.
	 */
	private final String directoryPath;

	/**
	 * A string that is created in the
	 * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method that
	 * represents the absolute path to the {@link #directory}. This path is
	 * removed from an absolute path of a file to obtain the ID of the node.
	 */
	private final String directoryPathWithTrailingSlash;

	/**
	 * length of the absolute path of the directory - the trailing slash
	 */
	private final int directoryAbsolutePathLength;

	private final InclusionExclusionFilenameFilter inclusionExclusionFilenameFilter;

	/**
	 * Connector associated with these path utilities
	 */
	private final IrodsWriteableConnector irodsWriteableConnector;

	public static final Logger log = LoggerFactory
			.getLogger(PathUtilities.class);

	/**
	 * Constructor takes the absolute path to the iRODS directory in the json
	 * config for the iRODS projection
	 * 
	 * @param directoryPath
	 *            <code>String</code> with the directory path
	 * @param inclusionExclusionFilenameFilter
	 *            {@link InclusinoExclusionFilenameFilter}
	 * @param IrodsWriteableConnector
	 *            connector {@link IrodsWriteableConnector}
	 */
	public PathUtilities(
			final String directoryPath,
			final InclusionExclusionFilenameFilter inclusionExclusionFilenameFilter,
			final IrodsWriteableConnector irodsWriteableConnector) {
		if (directoryPath == null || directoryPath.isEmpty()) {
			throw new IllegalArgumentException("null or empty directoryPath");
		}

		if (inclusionExclusionFilenameFilter == null) {
			throw new IllegalArgumentException(
					"null or empty inclusionExclusionFilenameFilter");
		}

		if (irodsWriteableConnector == null) {
			throw new IllegalArgumentException("null irodsWriteableConnector");
		}

		this.directoryPath = directoryPath;
		this.inclusionExclusionFilenameFilter = inclusionExclusionFilenameFilter;

		this.directoryPathWithTrailingSlash = directoryPath + "/";
		this.directoryAbsolutePathLength = directoryPathWithTrailingSlash
				.length() - DELIMITER.length();
		this.irodsWriteableConnector = irodsWriteableConnector;
	}

	/**
	 * Given an id, determine the JCR node type
	 * 
	 * @param id
	 *            <code>String</code> with the id
	 * @return {@link IrodsNodeTypes} enum value
	 */
	public IrodsNodeTypes getNodeTypeForId(final String id) {
		if (id == null) {
			throw new IllegalArgumentException("unknown node type");
		}

		return IrodsNodeTypes.determineNodeTypeFromId(id);
	}

	/**
	 * Given an id, make sure it starts with a delimiter, and if it ends with
	 * the delimiter, strip it
	 * 
	 * @param id
	 *            <code>String</code> with a jcr id for a node
	 * @return <code>String</code> with a delim at the start and any trailing
	 *         delim stripped out
	 */
	public static String ensureIdStartsWithDelimiterAndStripOutTrailingDelimeter(
			final String id) {

		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}

		if (!id.startsWith(DELIMITER)) {
			throw new IllegalArgumentException("id must start with delimiter");
		}
		if (id.endsWith(DELIMITER)) {
			return id.substring(0, id.length() - DELIMITER.length());
		} else {
			return id;
		}
	}

	/**
	 * @return the directoryPath
	 */
	public String getDirectoryPath() {
		return directoryPath;
	}

	/**
	 * @return the directoryPathWithTrailingSlash
	 */
	public String getDirectoryPathWithTrailingSlash() {
		return directoryPathWithTrailingSlash;
	}

	/**
	 * @return the directoryAbsolutePathLength
	 */
	public int getDirectoryAbsolutePathLength() {
		return directoryAbsolutePathLength;
	}

	/**
	 * @return the inclusionExclusionFilenameFilter
	 */
	public InclusionExclusionFilenameFilter getInclusionExclusionFilenameFilter() {
		return inclusionExclusionFilenameFilter;
	}

	/**
	 * See if the file does not exist or is excluded by any filter. Return
	 * <code>true</code> if this file should be excluded
	 * 
	 * @param file
	 *            {@link IRODSFile} representing the underlying iRODS data
	 * @return <code>boolean</code> if this file should be excluded
	 */
	public boolean isExcluded(final IRODSFile file) {

		if (!file.exists()) {
			return true;
		}

		if (this.getInclusionExclusionFilenameFilter().accept(
				file.getParentFile(), file.getName())) {
			return false;
		} else {

			return true;
		}
	}

	/**
	 * Given an id for a node as given by Modeshape, strip a trailing delim if
	 * present, otherwise return as is
	 * 
	 * @param id
	 *            <code>String</code> with an id that may contain a trailing
	 *            delimiter
	 * @return <code>String</code> with the id stripped of any trailing
	 *         delimiter
	 */
	public static String stripTrailingDelimFromIdIfPresent(final String id) {
		if (id == null) {
			throw new IllegalArgumentException("Null id");
		}

		if (id.endsWith(DELIMITER)) {
			return id.substring(0, id.length() - DELIMITER.length());
		} else {
			return id;
		}
	}

	/**
	 * From an id for a file, return the id that has the appropriate
	 * JCR_CONTENT_SUFFIX for a file
	 * 
	 * @param id
	 *            <code>String</code> with the modeshape id
	 * @return <code>String</code> with the id formatted with the
	 *         JCR_CONTENT_SUFFIX
	 */
	public static String formatChildIdForDocument(final String id) {
		if (id == null) {
			throw new IllegalArgumentException("null id");
		}

		return isRoot(id) ? JCR_CONTENT_SUFFIX : id + JCR_CONTENT_SUFFIX;
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
	public static boolean isRoot(final String id) {
		if (id == null) {
			throw new IllegalArgumentException("null id");
		}
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
	public String idFor(final IRODSFile file) {
		String path = file.getAbsolutePath();
		if (!path.startsWith(getDirectoryPathWithTrailingSlash())) {
			if (getDirectoryPath().equals(path)) {
				// This is the root
				return PathUtilities.DELIMITER;
			}

			throw new DocumentStoreException(path,
					"given path is not within the scope of this connector");
		}
		String id = path.substring(getDirectoryAbsolutePathLength());
		return id;
	}

	/**
	 * Get an object that has the node type and parsed id (without suffixes)
	 * 
	 * @param id
	 *            <code>String</code> ModeShape id
	 * @return {@link NodeTypeAndId}
	 */
	public NodeTypeAndId stripSuffixFromId(final String id) {
		log.info("stripSuffixFromId()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}

		String myId = id;

		if (myId.endsWith(DELIMITER)) {
			myId = id.substring(0, id.length() - DELIMITER.length());
		}

		IrodsNodeTypes nodeType = this.getNodeTypeForId(myId);

		switch (nodeType) {
		case CONTENT_NODE:
			myId = myId.substring(0, myId.length() - JCR_CONTENT_SUFFIX_LENGTH);
			break;
		case AVU_NODE:
			int idxOfAttr = myId.indexOf(AVU_ID);
			myId = myId.substring(0, idxOfAttr);
			break;
		default:
			// myId is already set
		}

		NodeTypeAndId nodeTypeAndId = new NodeTypeAndId();
		nodeTypeAndId.setIrodsNodeType(nodeType);
		nodeTypeAndId.setId(myId);
		log.info("nodeTypeAndId determined:{}", nodeTypeAndId);
		return nodeTypeAndId;
	}

	/**
	 * Computes the SHA1 for the given file. By default, this method will look
	 * at the {@link FileSystemConnector#contentBasedSha1()} flag and either
	 * take the URL of the file (using @see java.util.File#toURI().toURL() and
	 * return the SHA1 of the URL string or return the SHA1 of the entire file
	 * content.
	 * 
	 * @param file
	 *            a {@link IRODSFile} instance; never null
	 * @return the SHA1 of the file.
	 */
	public String sha1(final IRODSFile file) {
		try {
			if (irodsWriteableConnector.isContentBasedSha1()) {
				log.info("content based SHA1, computing for {} ...",
						file.getAbsolutePath());

				DataObjectAO dataObjectAO = IRODSFileSystemSingletonWrapper
						.instance()
						.getIRODSAccessObjectFactory()
						.getDataObjectAO(
								irodsWriteableConnector.getIrodsAccount());

				byte[] hash = dataObjectAO
						.computeSHA1ChecksumOfIrodsFileByReadingDataFromStream(file
								.getAbsolutePath());
				return StringUtil.getHexString(hash);
			} else {
				log.info("file path based SHA1, computing for {} ...",
						file.getAbsolutePath());
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
	public URL createUrlForFile(final IRODSFile file) throws IOException {
		return file.toFileBasedURL();
	}

	/**
	 * @return the delimiter
	 */
	public static String getDelimiter() {
		return DELIMITER;
	}

	/**
	 * @return the mixMimeType
	 */
	public static String getMixMimeType() {
		return MIX_MIME_TYPE;
	}

	/**
	 * @return the jcrPrimaryType
	 */
	public static String getJcrPrimaryType() {
		return JCR_PRIMARY_TYPE;
	}

	/**
	 * @return the jcrData
	 */
	public static String getJcrData() {
		return JCR_DATA;
	}

	/**
	 * @return the jcrMimeType
	 */
	public static String getJcrMimeType() {
		return JCR_MIME_TYPE;
	}

	/**
	 * @return the jcrEncoding
	 */
	public static String getJcrEncoding() {
		return JCR_ENCODING;
	}

	/**
	 * @return the jcrCreated
	 */
	public static String getJcrCreated() {
		return JCR_CREATED;
	}

	/**
	 * @return the jcrCreatedBy
	 */
	public static String getJcrCreatedBy() {
		return JCR_CREATED_BY;
	}

	/**
	 * @return the jcrLastModified
	 */
	public static String getJcrLastModified() {
		return JCR_LAST_MODIFIED;
	}

	/**
	 * @return the jcrLastModifiedBy
	 */
	public static String getJcrLastModifiedBy() {
		return JCR_LAST_MODIFIED_BY;
	}

	/**
	 * @return the ntFolder
	 */
	public static String getNtFolder() {
		return NT_FOLDER;
	}

	/**
	 * @return the ntFile
	 */
	public static String getNtFile() {
		return NT_FILE;
	}

	/**
	 * @return the ntResource
	 */
	public static String getNtResource() {
		return NT_RESOURCE;
	}

	/**
	 * @return the jcrContent
	 */
	public static String getJcrContent() {
		return JCR_CONTENT;
	}

	/**
	 * @return the jcrContentSuffix
	 */
	public static String getJcrContentSuffix() {
		return JCR_CONTENT_SUFFIX;
	}

	/**
	 * @return the jcrIrodsIrodsobject
	 */
	public static String getJcrIrodsIrodsobject() {
		return JCR_IRODS_IRODSOBJECT;
	}

	/**
	 * @return the jcrIrodsAvuProp
	 */
	public static String getJcrIrodsAvuProp() {
		return JCR_IRODS_AVU_PROP;
	}

	/**
	 * @return the delimiterLength
	 */
	public static int getDelimiterLength() {
		return DELIMITER_LENGTH;
	}

	/**
	 * @return the jcrIrodsAvu
	 */
	public static String getJcrIrodsAvu() {
		return JCR_IRODS_AVU;
	}

	/**
	 * @return the jcrAvuSuffix
	 */
	public static String getJcrAvuSuffix() {
		return JCR_AVU_SUFFIX;
	}

	/**
	 * @return the jcrAvuSuffixLength
	 */
	public static int getJcrAvuSuffixLength() {
		return JCR_AVU_SUFFIX_LENGTH;
	}

	/**
	 * @return the jcrContentSuffixLength
	 */
	public static int getJcrContentSuffixLength() {
		return JCR_CONTENT_SUFFIX_LENGTH;
	}

	/**
	 * @return the avuId
	 */
	public static String getAvuId() {
		return AVU_ID;
	}

	/**
	 * @return the log
	 */
	public static Logger getLog() {
		return log;
	}

}
