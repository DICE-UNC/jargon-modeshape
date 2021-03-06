package org.irods.jargon.modeshape.connector;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;

public class PathUtilitiesTest {

	@Test
	public void testGetNodeTypeForId() {
		InclusionExclusionFilenameFilter filter = Mockito
				.mock(InclusionExclusionFilenameFilter.class);
		String dir = "/a/dir";
		IrodsWriteableConnector connector = Mockito
				.mock(IrodsWriteableConnector.class);
		PathUtilities utilities = new PathUtilities(dir, filter, connector);
		String testId = "/col1/file613.txt/jcr:content";
		IrodsNodeTypes actual = utilities.getNodeTypeForId(testId);
		Assert.assertEquals(IrodsNodeTypes.CONTENT_NODE, actual);
	}

}
