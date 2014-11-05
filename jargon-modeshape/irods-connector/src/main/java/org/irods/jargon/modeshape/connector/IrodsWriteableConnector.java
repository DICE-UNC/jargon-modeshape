/**
 * 
 */
package org.irods.jargon.modeshape.connector;

import java.util.Collection;

import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.spi.federation.DocumentChanges;
import org.modeshape.jcr.spi.federation.WritableConnector;
import org.modeshape.jcr.value.Name;

/**
 * @author Mike Conway - DICE
 *
 */
public class IrodsWriteableConnector extends WritableConnector {

	@Override
	public Document getDocumentById(String arg0) {
		return null;
	}

	@Override
	public String getDocumentId(String arg0) {
		return null;
	}

	@Override
	public Collection<String> getDocumentPathsById(String arg0) {
		return null;
	}

	@Override
	public boolean hasDocument(String arg0) {
		return false;
	}

	@Override
	public String newDocumentId(String arg0, Name arg1, Name arg2) {
		return null;
	}

	@Override
	public boolean removeDocument(String arg0) {
		return false;
	}

	@Override
	public void storeDocument(Document arg0) {

	}

	@Override
	public void updateDocument(DocumentChanges arg0) {
		// TODO Auto-generated method stub

	}

}
