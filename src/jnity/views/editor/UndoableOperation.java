package jnity.views.editor;

import java.io.InputStream;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.UndoContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class UndoableOperation extends AbstractOperation {

	private InputStream previousContentStream;
	private InputStream contentStream;
	private SceneController sceneController;

	public UndoableOperation(String info, SceneController sceneController, UndoContext undoContext) {
		super(info);
		this.sceneController = sceneController;
		previousContentStream = sceneController.getPreviousContentStream();
		contentStream = sceneController.openContentStream();
		addContext(undoContext);
		
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		sceneController.loadSceneWithoutCamera(contentStream);
		return Status.OK_STATUS;
		
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		sceneController.loadSceneWithoutCamera(previousContentStream);
		return Status.OK_STATUS;
	}

	

}
