/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.util.Collection;
import java.util.Map;

import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.DocumentChanges;
import org.modeshape.jcr.federation.spi.ExtraPropertiesStore;
import org.modeshape.jcr.federation.spi.PageKey;
import org.modeshape.jcr.federation.spi.Pageable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * ModeShape SPI connector
 * 
 * @author Mike Conway - DICE (www.irods.org)
 * 
 */
public class IRODSWriteableConnector extends Connector implements
		ExtraPropertiesStore, Pageable {

	/**
	 * 
	 */
	public IRODSWriteableConnector() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#getDocumentById(java.lang.
	 * String)
	 */
	@Override
	public Document getDocumentById(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#getDocumentId(java.lang.String
	 * )
	 */
	@Override
	public String getDocumentId(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#getDocumentPathsById(java.
	 * lang.String)
	 */
	@Override
	public Collection<String> getDocumentPathsById(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#hasDocument(java.lang.String)
	 */
	@Override
	public boolean hasDocument(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.modeshape.jcr.federation.spi.Connector#isReadonly()
	 */
	@Override
	public boolean isReadonly() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#newDocumentId(java.lang.String
	 * , org.modeshape.jcr.value.Name, org.modeshape.jcr.value.Name)
	 */
	@Override
	public String newDocumentId(String arg0, Name arg1, Name arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#removeDocument(java.lang.String
	 * )
	 */
	@Override
	public boolean removeDocument(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#storeDocument(org.infinispan
	 * .schematic.document.Document)
	 */
	@Override
	public void storeDocument(Document arg0) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.modeshape.jcr.federation.spi.Connector#updateDocument(org.modeshape
	 * .jcr.federation.spi.DocumentChanges)
	 */
	@Override
	public void updateDocument(DocumentChanges arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<Name, Property> getProperties(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeProperties(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void storeProperties(String arg0, Map<Name, Property> arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateProperties(String arg0, Map<Name, Property> arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public Document getChildren(PageKey arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
