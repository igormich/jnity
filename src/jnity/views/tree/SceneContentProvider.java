package jnity.views.tree;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;

import base.Object3d;

public class SceneContentProvider implements ITreeContentProvider {

	public SceneContentProvider() {
		
	}

	@Override
	public Object3d[] getElements(Object inputElement) {
		List<Object3d> children = ((Object3d) inputElement).getChildren();
		return children.toArray(new Object3d[]{});
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return ((Object3d) parentElement).getChildren().toArray();
	}

	@Override
	public Object getParent(Object element) {
		return ((Object3d) element).getParent();
	}

	@Override
	public boolean hasChildren(Object element) {
		return ((Object3d) element).getChildren().size()>0;
	}

}
