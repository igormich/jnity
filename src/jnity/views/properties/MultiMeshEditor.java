package jnity.views.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import base.Object3d;
import io.ResourceController;
import jnity.Utils;
import jnity.views.SceneEditor;
import jnity.views.editor.SceneController;
import properties.Mesh;
import properties.MultiMesh;
import properties.Property3d;

public class MultiMeshEditor extends PropertyEditor {

	private Composite materials;
	private Combo meshSelector;
	private MultiMesh multiMesh;
	private List<Combo> materialSelectors = new ArrayList<>();

	public MultiMeshEditor(Composite parent, int style, SceneEditor sceneEditor, Object3d owner) {
		super(parent, style, sceneEditor, owner);
		Composite header = new Composite(group, style);
		header.setLayoutData(Utils.fillGridHorizontal());
		GridLayout headerLayout = new GridLayout(2, false);
		header.setLayout(headerLayout);
		new Label(header, SWT.NONE).setText("Mesh file:");
		meshSelector = new Combo(header, SWT.NONE);
		meshSelector.setLayoutData(Utils.fillGridHorizontal());
		SceneController sceneController = sceneEditor.getSceneController();
		try {
			for (IResource file : sceneController.getModelFolder().members()) {
				meshSelector.add(file.getName());
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		meshSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo combo = (Combo) e.getSource();
				if (multiMesh != null) {
					if ((combo.getText() != null) && (!multiMesh.getFileName().equals(combo.getText()))) {
						ResourceController.getOrCreate().getOrLoadMesh(multiMesh, combo.getText());
						sceneEditor.makeDirty("Mesh model change");
						renewMaterials(multiMesh, editable);
					}
				}
			}
		});
		materials = new Composite(group, SWT.NONE);
		materials.setLayoutData(Utils.fillGridHorizontal());
		GridLayout materialsLayout = new GridLayout(2, false);
		materials.setLayout(materialsLayout);

	}

	@Override
	public void renew(Property3d property3d, boolean editable) {
		super.renew(property3d, editable);
		MultiMesh multiMesh = (MultiMesh) property3d;
		boolean objectChange = (this.multiMesh != multiMesh)
				|| (this.multiMesh.getFileName() != multiMesh.getFileName());
		this.multiMesh = null;
		Utils.setIfChangeString(meshSelector, multiMesh.getFileName());
		meshSelector.setEnabled(editable);
		if (objectChange) 
			rebuildMaterials(multiMesh);
		renewMaterials(multiMesh, editable);
		this.multiMesh = multiMesh;

	}

	private void rebuildMaterials(MultiMesh multiMesh) {
		Utils.clear(materials);
		materialSelectors.clear();
		SceneController sceneController = sceneEditor.getSceneController();
		int num = 1;
		for (Mesh mesh : multiMesh.getMeshes()) {
			new Label(materials, SWT.NONE).setText("Material " + (num++) + ":");
			Combo materialSelector = new Combo(materials, SWT.NONE);
			materialSelectors.add(materialSelector);
			for (String material : sceneController.getScene().getMaterialLibrary().getMaterialNames()) {
				materialSelector.add(material);
			}
			materialSelector.setLayoutData(Utils.fillGridHorizontal());
			materialSelector.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					Combo combo = (Combo) e.getSource();
					if ((combo.getText() != null) && (!mesh.getMaterialName().equals(combo.getText()))) {
						mesh.setMaterialName(combo.getText());
						sceneEditor.makeDirty("Mesh material change");
					}
				}
			});
		}
	}

	private void renewMaterials(MultiMesh multiMesh, boolean editable) {
		int num = 0;
		for (Mesh mesh : multiMesh.getMeshes()) {
			Combo materialSelector = materialSelectors.get(num++);
					Utils.setIfChangeString(materialSelector, mesh.getMaterialName());
			materialSelector.setText(mesh.getMaterialName());
			materialSelector.setEnabled(editable);
		}
			

	}

}
