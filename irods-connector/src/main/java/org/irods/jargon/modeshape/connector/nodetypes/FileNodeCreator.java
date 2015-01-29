/**
 * 
 */
package org.irods.jargon.modeshape.connector.nodetypes;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.infinispan.schematic.document.Document;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.FileNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.CollectionPagerAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystemSingletonWrapper;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileImpl;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.MetaDataAndDomainData;
import org.irods.jargon.core.query.PagingAwareCollectionListing;
import org.irods.jargon.modeshape.connector.IrodsWriteableConnector;
import org.irods.jargon.modeshape.connector.PathUtilities;
import org.irods.jargon.modeshape.connector.exceptions.IrodsConnectorRuntimeException;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.spi.federation.Connector.ExtraProperties;
import org.modeshape.jcr.spi.federation.DocumentChanges;
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

	public FileNodeCreator(
			final IRODSAccessObjectFactory irodsAccessObjectFactory,
			final IRODSAccount irodsAccount,
			final IrodsWriteableConnector irodsWriteableConnector) {
		super(irodsAccessObjectFactory, irodsAccount, irodsWriteableConnector);

		fileFromIdConverter = new FileFromIdConverterImpl(
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
		if (getPathUtilities().isExcluded(file)) {
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

		// String[] children = file.list(this.getPathUtilities()
		// .getInclusionExclusionFilenameFilter());
		long totalChildren = 0;
		int nextOffset = 0;
		int i = 0;
		log.info("parent is:{}", file.getAbsolutePath());

		CollectionPagerAO collectionPagerAO = getIrodsAccessObjectFactory()
				.getCollectionPagerAO(getIrodsAccount());

		PagingAwareCollectionListing listing = collectionPagerAO
				.retrieveFirstPageUnderParent(file.getAbsolutePath());
		log.info("got first listing:{}", listing);

		for (CollectionAndDataObjectListingEntry entry : listing
				.getCollectionAndDataObjectListingEntries()) {
			totalChildren++;
			i++;
			if (i >= offset && i < offset + this.getConnector().getPageSize()) {
				// We use identifiers that contain the file/directory name
				// ...
				// String childName = child.getName();
				String childId = PathUtilities.isRoot(id) ? PathUtilities.DELIMITER
						+ entry.getNodeLabelDisplayValue()
						: id + PathUtilities.DELIMITER
								+ entry.getNodeLabelDisplayValue();

				writer.addChild(childId, entry.getNodeLabelDisplayValue());

				log.info("added child directory with name:{}",
						entry.getNodeLabelDisplayValue());

			}
			nextOffset = i + 1;

		}


		// if there are still accessible children add the next page
		if (nextOffset < totalChildren) {
			writer.addPage(id, nextOffset, IrodsWriteableConnector.PAGE_SIZE,
					totalChildren);
		}

		if (!PathUtilities.isRoot(id)) {
			// Set the reference to the parent ...
			String parentId = getPathUtilities().idFor(
					(IRODSFile) file.getParentFile());
			writer.setParents(parentId);
		}

		Document myDoc = writer.document();
		log.debug("myDoc from create for file:{}", myDoc);
		return myDoc;
	}

	private Document instanceForIdAsFile(final String id, final IRODSFile file) {
		log.info("instanceFrIdAsFile()");
		DocumentWriter writer = newDocument(id);
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
			String parentId = getPathUtilities().idFor(
					(IRODSFile) file.getParentFile());
			writer.setParents(parentId);
		}

		// Add the 'mix:mixinType' mixin; if other mixins are stored in the
		// extra properties, this will append ...
		if (isAddMimeTypeMixin()) {
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
		if (!isIncludeAvus()) {
			log.info("avus not included");
			return;
		}

		log.info("avus included, do query");
		List<MetaDataAndDomainData> metadatas;
		try {

			File fileForProps;

			fileForProps = (File) getIrodsAccessObjectFactory()
					.getIRODSFileFactory(getIrodsAccount()).instanceIRODSFile(
							path);

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

		if (!isIncludeAvus()) {
			log.info("avus not included");
			return;
		}

		List<MetaDataAndDomainData> metadatas;
		try {

			File fileForProps = (File) getIrodsAccessObjectFactory()
					.getIRODSFileFactory(getIrodsAccount()).instanceIRODSFile(
							path);

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
			final List<MetaDataAndDomainData> metadatas,
			final File fileForProps, final String id) {
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
	public void store(final Document document) {
		log.info("store()");
		if (document == null) {
			throw new IllegalArgumentException("null documentReader");
		}

		DocumentReader documentReader = getConnector()
				.produceDocumentReaderFromDocument(document);

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

	private void storeFolder(final DocumentReader documentReader) {
		log.info("storeFolder");
		String id = documentReader.getDocumentId();
		log.info("file to store:{}", id);
		FileFromIdConverter converter = new FileFromIdConverterImpl(
				getIrodsAccessObjectFactory(), getIrodsAccount(),
				getPathUtilities());
		IRODSFile file;
		try {
			file = converter.fileFor(id);
		} catch (JargonException e) {
			log.error("jargonException getting file for storing folder", e);
			throw new DocumentStoreException(id, "unable to get file for store");
		}
		if (isExcluded((File) file)) {
			throw new DocumentStoreException(id, "file is excluded");
		}
		File parent = file.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}
		if (!parent.canWrite()) {
			throw new DocumentStoreException(id,
					"unable to write to parent file");
		}

		file.mkdirs();

		Map<Name, Property> properties = documentReader.getProperties();
		ExtraProperties extraProperties = getConnector()
				.retrieveExtraPropertiesForId(id, false);
		extraProperties.addAll(properties).except(
				PathUtilities.JCR_PRIMARY_TYPE, PathUtilities.JCR_CREATED,
				PathUtilities.JCR_LAST_MODIFIED, PathUtilities.JCR_DATA);
		extraProperties.save();

	}

	private void storeFile(final DocumentReader documentReader) {
		log.info("storeFile");
		String id = documentReader.getDocumentId();
		log.info("file to store:{}", id);
		FileFromIdConverter converter = new FileFromIdConverterImpl(
				getIrodsAccessObjectFactory(), getIrodsAccount(),
				getPathUtilities());
		IRODSFile file;
		try {
			file = converter.fileFor(id);
		} catch (JargonException e) {
			log.error("jargonException getting file for storing folder", e);
			throw new DocumentStoreException(id, "unable to get file for store");
		}
		if (isExcluded((File) file)) {
			throw new DocumentStoreException(id, "file is excluded");
		}
		File parent = file.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}
		if (!parent.canWrite()) {
			throw new DocumentStoreException(id,
					"unable to write to parent file");
		}

		try {
			file.createNewFile();
		} catch (IOException e) {
			log.error("IOexception creating new file:{}", file, e);
			throw new DocumentStoreException(id, "exception creating new file",
					e);
		}
		Map<Name, Property> properties = documentReader.getProperties();
		ExtraProperties extraProperties = getConnector()
				.retrieveExtraPropertiesForId(id, false);
		extraProperties.addAll(properties).except(
				PathUtilities.JCR_PRIMARY_TYPE, PathUtilities.JCR_CREATED,
				PathUtilities.JCR_LAST_MODIFIED, PathUtilities.JCR_DATA);
		extraProperties.save();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.irods.jargon.modeshape.connector.nodetypes.AbstractNodeTypeCreator
	 * #update(org.modeshape.jcr.spi.federation.DocumentChanges)
	 */
	@Override
	public void update(final DocumentChanges documentChanges) {
		log.info("update()");
		if (documentChanges == null) {
			throw new IllegalArgumentException("null documentchanges");
		}

		String id = documentChanges.getDocumentId();
		log.info("id for doc changes:{}", id);
		Document document = documentChanges.getDocument();
		FileFromIdConverter converter = new FileFromIdConverterImpl(
				getIrodsAccessObjectFactory(), getIrodsAccount(),
				getPathUtilities());
		IRODSFile file;
		try {
			file = converter.fileFor(id);
		} catch (JargonException e) {
			log.error("jargonException getting file for storing folder", e);
			throw new DocumentStoreException(id, "unable to get file for store");
		}
		if (isExcluded((File) file)) {
			throw new DocumentStoreException(id, "file is excluded");
		}
		DocumentReader documentReader = getConnector()
				.produceDocumentReaderFromDocument(document);
		log.info("file for id:{}", file);
		String idOrig = id;

		// if we're dealing with the root of the connector, we can't process
		// any
		// moves/removes because that would go "outside" the
		// connector scope
		if (!PathUtilities.isRoot(id)) {

			log.info("not root....");

			String parentId = documentReader.getParentIds().get(0);
			log.info("parent id:{}", parentId);
			File parent = file.getParentFile();
			log.info("parent:{}", parent);
			String newParentId = getPathUtilities().idFor((IRODSFile) parent);
			log.info("new parentId:{}", newParentId);

			if (!parentId.equals(newParentId)) {
				// The node has a new parent (via the 'update' method),
				// meaning
				// it was moved ...

				log.info("node was moved...");

				IRODSFileImpl newParent;
				try {
					newParent = (IRODSFileImpl) converter.fileFor(parentId);
				} catch (JargonException e) {
					log.error(
							"jargon exception attempting to to create new parent file",
							e);
					throw new DocumentStoreException(id,
							"unable to create newParent");
				}
				log.info("file for new parent:{}", newParent);

				IRODSFile newFile;
				try {
					newFile = getIrodsAccessObjectFactory()
							.getIRODSFileFactory(getIrodsAccount())
							.instanceIRODSFile(newParent, file.getName());

					log.info("new file:{}", newFile);

				} catch (JargonException e) {
					log.error("jargon error getting newFile", e);
					throw new DocumentStoreException(id, e);

				}

				log.info("renaming....");
				file.renameTo(newFile);
				log.info("rename done to :{}", newFile);

				if (!parent.exists()) {
					parent.mkdirs(); // in case they were removed since we
										// created them ...
				}
				if (!parent.canWrite()) {
					log.error("parent does not allow write");
					throw new DocumentStoreException(id,
							"parent does not allow write");
				}
				parent = newParent;
				// Remove the extra properties at the old location ...
				getConnector().retrieveExtraPropertiesStore().removeProperties(
						id);
				// Set the id to the new location ...
				id = getPathUtilities().idFor(newFile);
			} else {
				// It is the same parent as before ...
				if (!parent.exists()) {
					parent.mkdirs(); // in case they were removed since we
										// created them ...
				}
				if (!parent.canWrite()) {
					log.error("cannot write this filef for parent:{}", parent);
					throw new DocumentStoreException(id,
							"unable to write file due to permissions");
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

		String primaryType = documentReader.getPrimaryTypeName();
		Map<Name, Property> properties = documentReader.getProperties();
		id = idOrig;
		ExtraProperties extraProperties = getConnector()
				.retrieveExtraPropertiesForId(id, true);
		extraProperties.addAll(properties).except(
				PathUtilities.JCR_PRIMARY_TYPE, PathUtilities.JCR_CREATED,
				PathUtilities.JCR_LAST_MODIFIED, PathUtilities.JCR_DATA);
		try {
			if (PathUtilities.NT_FILE.equals(primaryType)) {
				file.createNewFile();
			} else if (PathUtilities.NT_FOLDER.equals(primaryType)) {
				file.mkdir();
			} else {
			}
			extraProperties.save();

		} catch (IOException e) {
			throw new DocumentStoreException(id, e);
		}

	}

	private IRODSFile renameChildrenFiles(final String id,
			final IRODSFile file, final Map<String, Name> renamedChildren,
			final String renamedChildId) {
		log.info("renamed child id:{}", renamedChildId);

		FileFromIdConverter converter = new FileFromIdConverterImpl(
				getIrodsAccessObjectFactory(), getIrodsAccount(),
				getPathUtilities());
		IRODSFile child;
		try {
			child = converter.fileFor(renamedChildId);
		} catch (JargonException e) {
			log.error("jargonException getting file for storing folder", e);
			throw new DocumentStoreException(id, "unable to get file for store");
		}
		log.info("child:{}", child);
		Name newName = renamedChildren.get(renamedChildId);
		String newNameStr = getConnector().obtainHandleToFactories()
				.getStringFactory().create(newName);
		IRODSFile renamedChild;
		try {
			renamedChild = getIrodsAccessObjectFactory().getIRODSFileFactory(
					getIrodsAccount()).instanceIRODSFile((File) file,
					newNameStr);
			log.info("renamedChild:{}", renamedChild);
		} catch (JargonException e) {
			throw new DocumentStoreException(id, e);
		}

		if (!child.renameTo(renamedChild)) {
			log.error("cannot rename child:{}", renamedChild);
		}
		return renamedChild;
	}
}
