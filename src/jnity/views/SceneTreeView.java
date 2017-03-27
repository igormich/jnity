package jnity.views;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

import base.Object3d;
import jnity.properties.SelectionOverlay;
import jnity.views.editor.ClipboardUtils;
import jnity.views.editor.SceneController;
import jnity.views.tree.SceneContentProvider;
import jnity.views.tree.TreeDragListener;
import jnity.views.tree.TreeDropListener;
import jnity.views.tree.TreeTransfer;

public class SceneTreeView extends ViewPart {

	public static final String ID = "jnity.views.SceneTreeView";
	private static WeakReference<SceneTreeView> instance = new WeakReference<SceneTreeView>(null);
	private SceneEditor sceneEditor;
	private Set<Integer> keys = new HashSet<>();
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
		viewer.addDragSupport(operations, types, new TreeDragListener(viewer));
		viewer.addDropSupport(operations, types, new TreeDropListener(viewer));
		viewer.setContentProvider(new SceneContentProvider());
		viewer.addSelectionChangedListener(treeSelectionListener);
		parent.addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {
				TreeSelection treeSelection = (TreeSelection) viewer.getSelection();
				Object3d selected = (Object3d) treeSelection.getFirstElement();
				SceneController sceneController = sceneEditor.getSceneController();
				
				if ((e.keyCode == (int) 'x') && keys.contains(Utils.KEY_CTRL) && selected != null) {
					ClipboardUtils.setClipboardContents(selected.fastClone());
					sceneController.getScene().remove(sceneController.getSelectedObject());
					sceneController.setSelectedObject(null);
					sceneEditor.makeDirty("Cut Object");
				}
				if ((e.keyCode == (int) 'c') && keys.contains(Utils.KEY_CTRL) && selected != null) {
					ClipboardUtils.setClipboardContents(selected.fastClone());
				}
				if ((e.keyCode == (int) 'v') && keys.contains(Utils.KEY_CTRL)) {
					Object3d object3d = ClipboardUtils.getClipboardContents();
					if (object3d != null) {
						sceneController.getScene().add(object3d);
						sceneController.setSelectedObject(object3d);// ?
						sceneEditor.makeDirty("Insert Object");
					}
				}
				if ((e.keyCode == Utils.KEY_DELETE) && (sceneController.getSelectedObject() != null)) {
					sceneController.getScene().remove(sceneController.getSelectedObject());
					sceneController.setSelectedObject(null);
					sceneEditor.makeDirty("delete");
				}
				keys.clear();
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if ((e.stateMask & SWT.MODIFIER_MASK) == 0)
					keys.add(e.keyCode);
			}
		});
		parent.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				keys.clear();
				
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		Menu menu = new Menu(viewer.getControl());
		MenuItem addStaticPhysicBody = new MenuItem(menu, SWT.NONE);
		addStaticPhysicBody.setText("add Static Physic Body");
		
		MenuItem addDynamicPhysicBody = new MenuItem(menu, SWT.NONE);
		addDynamicPhysicBody.setText("add Dynamic Physic Body");
		viewer.getControl().setMenu(menu);
	}

	@Override
	public void setFocus() {

	}
	public void setSceneEditor(SceneEditor sceneEditor) {
		this.sceneEditor = sceneEditor;
		viewer.setInput(sceneEditor.getSceneController().getScene().getRoot());
		viewer.refresh();
		viewer.expandAll();
	}
}
