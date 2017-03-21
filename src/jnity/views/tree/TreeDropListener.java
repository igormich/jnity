package jnity.views.tree;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;

import base.Object3d;

public class TreeDropListener  extends ViewerDropAdapter  {

	private final Viewer viewer;

	public TreeDropListener(Viewer viewer) {
		super(viewer);
		this.viewer = viewer;
	}

	@Override
	public boolean validateDrop(Object target, int operation,
			TransferData transferType) {
		return TreeTransfer.getInstance().isSupportedType(transferType);
	}

	@Override
	public boolean performDrop(Object data) {
		Object3d object3d = (Object3d) data;
		Object3d target = (Object3d) getCurrentTarget();
		if (getCurrentLocation()== LOCATION_ON) {
			target.addChild(object3d);
		}
		if (getCurrentLocation() == LOCATION_BEFORE) {	
			target.getParent().addChildBefore(object3d,target);
		}
		if (getCurrentLocation() == LOCATION_AFTER) {
			target.getParent().addChildAfter(object3d,target);
		}
		viewer.refresh();
		return true;
	}

}
