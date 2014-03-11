package org.irods.jargon.modeshape.connector;

import java.io.File;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.irods.jargon.core.exception.JargonRuntimeException;
import org.irods.jargon.testutils.TestingUtilsException;
import org.irods.jargon.testutils.filemanip.FileGenerator;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.api.Workspace;
import org.modeshape.jcr.api.federation.FederationManager;

class Projection {
	protected final File directory;
	private final String name;

	public static final String TEXT_CONTENT = "Some text content";

	public Projection(final String name, final String directoryPath) {
		this.name = name;
		directory = new File(directoryPath);
	}

	public String getName() {
		return name;
	}

	public void create(final Node parentNode, final String childName)
			throws RepositoryException {
		Session session = parentNode.getSession();
		Workspace workspace = (Workspace) session.getWorkspace();
		FederationManager fedMgr = workspace.getFederationManager();
		fedMgr.createProjection(parentNode.getPath(), getName(), "/", childName);
	}

	public void initialize() throws IOException {
		if (directory.exists()) {
			FileUtil.delete(directory);
		}
		directory.mkdirs();
		// Make some content ...
		new File(directory, "dir1").mkdir();
		new File(directory, "dir2").mkdir();
		new File(directory, "dir3").mkdir();

		try {
			String localFileName = FileGenerator
					.generateFileOfFixedLengthGivenName(
							directory.getAbsolutePath(), "simple1.txt", 3);
		} catch (TestingUtilsException e) {
			throw new JargonRuntimeException(e);
		}

	}

	public void delete() {
		if (directory.exists()) {
			FileUtil.delete(directory);
		}
	}

	public File getTestFile(final String relativePath) {
		return new File(directory, relativePath);
	}

	@Override
	public String toString() {
		return "Projection: " + name + " (at '" + directory.getAbsolutePath()
				+ "')";
	}
}
