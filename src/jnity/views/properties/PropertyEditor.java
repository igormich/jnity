package jnity.views.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import base.Object3d;
import jnity.views.SceneEditor;
import jnity.views.Utils;
import properties.Property3d;

public class PropertyEditor {

	
	protected SceneEditor sceneEditor;
	protected Group group;
	protected boolean editable;
	private Property3d property3d;
	private Button removeProperty;
	public PropertyEditor(Composite parent, int style, SceneEditor sceneEditor, Object3d owner) {
		this.sceneEditor = sceneEditor;
		group = new Group(parent, style);
		group.setLayoutData(Utils.fillGridHorizontal());
		GridLayout positionLayout = new GridLayout(1, false);
		group.setLayout(positionLayout);
		
		Composite header = new Composite(group, style);
		header.setLayoutData(Utils.fillGridHorizontal());
		GridLayout headerLayout = new GridLayout(2, false);
		header.setLayout(headerLayout);
		removeProperty = new Button(header, SWT.NONE);
		removeProperty.setText("delete");
		removeProperty.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				group.dispose();
				owner.remove(property3d);
				sceneEditor.makeDirty("Property removed");
			}
		});

	}
	public void renew(Property3d property3d, boolean editable) {
		this.property3d = property3d;
		this.editable = editable;
		removeProperty.setEnabled(editable);
		Utils.setIfChangeString(group, property3d.getClass().getSimpleName());
	}

}
