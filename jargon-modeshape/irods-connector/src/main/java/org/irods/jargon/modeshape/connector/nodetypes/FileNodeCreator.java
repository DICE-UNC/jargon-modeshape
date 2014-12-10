/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.FileNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystemSingletonWrapper;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.MetaDataAndDomainData;
import org.irods.jargon.modeshape.connector.IrodsWriteableConnector;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.irods.jargon.modeshape.connector.exceptions.IrodsConnectorRuntimeException;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.spi.federation.Connector.ExtraProperties;
import org.modeshape.jcr.spi.federation.DocumentReader;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a file node (iRODS file) based in its id
 * 
 * @author Mike Conway - DICE
 * 
 */
public class FileNodeCreator extends AbstractNodeTypeCreator {

	public static final Logger log = LoggerFactory
			.getLogger(FileNodeCreator.class);

	public static final int JCR_CONTENT_SUFFIX_LENGTH = PathUtilities.JCR_CONTENT_SUFFIX
			.length();

	private final FileFromIdConverter fileFromIdConverter;

	public FileNodeCreator(IRODSAccessObjectFactory irodsAccessObjectFactory,
			IRODSAccount irodsAccount,
			IrodsWriteableConnector irodsWriteableConnector) {
		super(irodsAccessObjectFactory, irodsAccount, irodsWriteableConnector);

		this.fileFromIdConverter = new FileFromIdConverterImpl(
				irodsAccessObjectFactory, irodsAccount,
				irodsWriteableConnector.getPathUtilities());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.irods.jargon.modeshape.connector.nodetypes.AbstractNodeTypeCreator
	 * #instanceForId(java.lang.String, int)
	 */
	@Override
	public Document instanceForId(final String id, final int offset)
			throws RepositoryException {
		log.info("instanceForId()");
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("null or empty id");
		}
		log.info("id:{}", id);
		log.info("offset:{}", offset);

		IRODSFile file;
		try {
			file = fileFromIdConverter.fileFor(id);
		} catch (JargonException e) {
			log.error("error getting file from id", e);
			throw new RepositoryException("error getting file", e);
		}
		if (this.getPathUtilities().isExcluded(file)) {
			log.info("file is excluded by filter or does not exist..return null");
			return null;
		}
		log.info("see if this is a data object or collection and act appropriately");

		if (file.isFile()) {
			return instanceForIdAsFile(id, file);
		} else {
			return instanceForIdAsCollection(id, file, offset);
		}

	}

	private Document instanceForIdAsCollection(final String id,
			final IRODSFile file, final int offset) {
		log.info("instanceForIdAsCollection()");
		DocumentWriter writer = newDocument(id);
		writer.setPrimaryType(PathUtilities.NT_FOLDER);
		writer.addMixinType(PathUtilities.JCR_IRODS_IRODSOBJECT);
		writer.addProperty(PathUtilities.JCR_CREATED, factories()
				.getDateFactory().create(file.lastModified()));
		writer.addProperty(PathUtilities.JCR_CREATED_BY, null); // ignored

		log.info("adding AVU children for the collection");

		addAvuChildrenForCollection(file.getAbsolutePath(), id, writer);
		log.info("AVU children added");

		String[] children = file.list(this.getPathUtilities()
				.getInclusionExclusionFilenameFilter());
		long totalChildren = 0;
		int nextOffset = 0;
		log.info("parent is:{}", file.getAbsolutePath());
		for (int i = 0; i < children.length; i++) {
			String child = children[i];

			// we need to count the total accessible children
			totalChildren++;
			// only add a child if it's in the current page
			if (i >= offset && i < offset + IrodsWriteableConnector.PAGE_SIZE) {
				// We use identifiers that contain the file/directory name
				// ...
				// String childName = child.getName();
				String childId = PathUtilities.isRoot(id) ? PathUtilities.DELIMITER
						+ child
						: id + PathUtilities.DELIMITER + child;

				writer.addChild(childId, child);

				log.info("added child directory with name:{}", child);

			}
			nextOffset = i + 1;

		}

		// if there are still accessible children add the next page
		if (nextOffset < totalChildren) {
			writer.addPage(id, nextOffset, IrodsWriteableConnector.PAGE_SIZE,
					totalChildren);
		}

		Document myDoc = writer.document();
		log.debug("myDoc from create for file:{}", myDoc);
		return myDoc;
	}

	private Document instanceForIdAsFile(String id, IRODSFile file) {
		log.info("instanceFrIdAsFile()");
		DocumentWriter writer = this.newDocument(id);
		writer.setPrimaryType(PathUtilities.NT_FILE);
		writer.addMixinType(PathUtilities.JCR_IRODS_IRODSOBJECT);

		writer.addProperty(PathUtilities.JCR_CREATED, factories()
				.getDateFactory().create(file.lastModified()));
		writer.addProperty(PathUtilities.JCR_CREATED_BY, null); // ignored

		String childId = PathUtilities.formatChildIdForDocument(id);
		writer.addChild(childId, PathUtilities.JCR_CONTENT);
		if (!PathUtilities.isRoot(id)) {
			log.info("not root, set reference to parent");
			// Set the reference to the parent ...
			String parentId = this.getPathUtilities().idFor(
					(IRODSFile) file.getParentFile());
			writer.setParents(parentId);
		}

		// Add the 'mix:mixinType' mixin; if other mixins are stored in the
		// extra properties, this will append ...
		if (this.isAddMimeTypeMixin()) {
			writer.addMixinType(MIX_MIME_TYPE);
		}

		log.info("see if avus added...");
		addAvuChildrenForDataObject(file.getAbsolutePath(), id, writer);
		log.info("done!");
		// Return the document ...
		return writer.document();
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

		log.info("addAvuChildrenForDataObject()");
		if (!this.isIncludeAvus()) {
			log.info("avus not included");
			return;
		}

		log.info("avus included, do query");
		List<MetaDataAndDomainData> metadatas;
		try {

			File fileForProps;

			fileForProps = (File) getIrodsAccessObjectFactory()
					.getIRODSFileFactory(this.getIrodsAccount())
					.instanceIRODSFile(path);

			log.info("file abs path to search for collection AVUs:{}",
					fileForProps.getAbsolutePath());

			DataObjectAO dataObjectAO = getIrodsAccessObjectFactory()
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

		log.info("addAvuChildrenForCollection()");

		if (!this.isIncludeAvus()) {
			log.info("avus not included");
			return;
		}

		List<MetaDataAndDomainData> metadatas;
		try {

			File fileForProps = (File) getIrodsAccessObjectFactory()
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
			sb.append(PathUtilities.JCR_AVU_SUFFIX);
			String childId = sb.toString();
			log.info("adding avu child with childName:{}", childName);
			log.info("avu childId:{}", childId);
			writer.addChild(childId, childName);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.irods.jargon.modeshape.connector.nodetypes.AbstractNodeTypeCreator
	 * #store(org.modeshape.jcr.spi.federation.DocumentReader)
	 */
	@Override
	public void store(DocumentReader documentReader) {
		log.info("store()");
		if (documentReader == null) {
			throw new IllegalArgumentException("null documentReader");
		}

		String primaryType = documentReader.getPrimaryTypeName();
		log.info("primaryType:{}", primaryType);
		if (PathUtilities.NT_FILE.equals(primaryType)) {
			log.info("its a file");
			storeFile(documentReader);
		} else if (PathUtilities.NT_FOLDER.equals(primaryType)) {
			log.info("its a folder");
			storeFolder(documentReader);
		} else {
			log.error("invalid node type for this operation:{}", primaryType);
			throw new IrodsConnectorRuntimeException(
					"primaryType not supported by this NodeCreator");
		}

	}

	private void storeFolder(DocumentReader documentReader) {
		log.info("storeFolder");
		String id = documentReader.getDocumentId();
		File file = fileFor(id, false);
		if (this.isExcluded(file)) {
			throw new DocumentStoreException(id, "file is excluded");
		}
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

	}

	private void storeFile(DocumentReader documentReader) {
		// TODO Auto-generated method stub

	}
}
