package jnity.views;

import java.lang.ref.WeakReference;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

import base.Object3d;
import jnity.views.tree.SceneContentProvider;
import jnity.views.tree.TreeDragListener;
import jnity.views.tree.TreeDropListener;
import jnity.views.tree.TreeTransfer;

public class SceneTreeView extends ViewPart {

	public static final String ID = "jnity.views.SceneTreeView";
	private static WeakReference<SceneTreeView> instance = new WeakReference<SceneTreeView>(null);
	private SceneEditor sceneEditor;
	private ISelectionChangedListener treeSelectionListener = new ISelectionChangedListener() {

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			TreeSelection treeSelection = (TreeSelection) event.getSelection();
			Object3d object = (Object3d) treeSelection.getFirstElement();
			sceneEditor.getSceneController().setSelectedObject(object);
			sceneEditor.setObjectToEdit();
		}	
	};
	private TreeViewer viewer;


	public static SceneTreeView getInstance() {
		return instance.get();
	}

	public SceneTreeView() {
		instance = new WeakReference<SceneTreeView>(this);
	}

	@Override
	public void dispose() {
		instance = new WeakReference<SceneTreeView>(null);
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		int operations = DND.DROP_COPY | DND.DROP_MOVE;
		//Transfer[] types = new Transfer[] { FileTransfer.getInstance(), ResourceTransfer.getInstance() };
		Transfer[] types = new Transfer[] { TreeTransfer.getInstance()};
		viewer.addDragSupport(operations, types, new TreeDragListener(
				viewer));
		viewer.addDropSupport(operations, types, new TreeDropListener(
				viewer));
		viewer.setContentProvider(new SceneContentProvider());
		viewer.addSelectionChangedListener(treeSelectionListener);
	}

	@Override
	public void setFocus() {

	}
	public void setSceneEditor(SceneEditor sceneEditor) {
		this.sceneEditor = sceneEditor;
		viewer.setInput(sceneEditor.getSceneController().getScene().getRoot());
		viewer.refresh();
	}
}
