package jnity.views.tree;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;

import base.Object3d;

public class TreeDragListener  extends DragSourceAdapter  {

	
	private final ISelectionProvider selectionProvider;
	public TreeDragListener(TreeViewer viewer) {
		this.selectionProvider = viewer;
	}


	@Override
	public void dragSetData(DragSourceEvent event) {
		TreeSelection selection = (TreeSelection) selectionProvider.getSelection();

		TreeTransfer transfer = TreeTransfer.getInstance();
		if (transfer.isSupportedType(event.dataType)) {
			transfer.setObject((Object3d) selection.getFirstElement());
		}

	}


}
