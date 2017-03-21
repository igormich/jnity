package jnity.views.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import base.Object3d;
import jnity.views.SceneEditor;

public class SceneEditorListener implements Listener {

	private SceneController sceneController;
	private SceneEditor sceneEditor;
	private boolean look;
	private int startY;
	private int startX;

	public SceneEditorListener(SceneEditor sceneEditor, SceneController sceneController) {
		this.sceneEditor = sceneEditor;
		this.sceneController = sceneController;
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.type) {
		case SWT.MouseMove:
			mouseMove(event);
			break;
		case SWT.MouseDown:
			mouseDown(event);
			break;
		case SWT.MouseUp:
			mouseUp(event);
			break;
		default:
			break;
		}

	}

	private void mouseUp(Event event) {
		if (event.button == 3) {
			look = false;
		}
	}

	private void mouseDown(Event event) {
		if (event.button == 1) {
			Object3d object3d = sceneController.getObject(event.x, event.y);
			boolean selectedChanged = sceneController.getSelectedObject() != object3d;
			sceneController.setSelectedObject(object3d);
			if (selectedChanged) {
				sceneEditor.setObjectToEdit();
			}
		}
		if (event.button == 3) {
			sceneEditor.setFocus();
			startX = event.x;
			startY = event.y;
			look = true;
		}
	}

	private void mouseMove(Event event) {
		sceneController.setUnderCursorObject(sceneController.getObject(event.x, event.y));
		if (look) {
			sceneController.getCamera().getPosition().roll((startY - event.y) / 5).turn(-(startX - event.x) / 5);
			startX = event.x;
			startY = event.y;
		}
	}

}
