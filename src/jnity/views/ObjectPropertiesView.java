
package jnity.views;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.lwjgl.util.vector.Vector3f;

import base.Object3d;
import base.Position;
import jnity.Utils;
import jnity.properties.EditorProperty;
import jnity.views.editor.SceneController;
import jnity.views.properties.MultiMeshEditor;
import jnity.views.properties.PropertieAdder;
import jnity.views.properties.PropertyEditor;
import properties.MultiMesh;
import properties.Property3d;

public class ObjectPropertiesView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */

	private static WeakReference<ObjectPropertiesView> instance = new WeakReference<ObjectPropertiesView>(null);

	public static final String ID = "jnity.views.ObjectPropertiesView";

	private Map<Property3d, PropertyEditor> propertiesPanels = new HashMap<>();

	private Text inputTX;
	private Text inputTY;
	private Text inputTZ;
	private Text inputRX;
	private Text inputRY;
	private Text inputRZ;
	private Text inputSX;
	private Text inputSY;
	private Text inputSZ;

	private Text objectName;

	private Composite parent;

	public static ObjectPropertiesView getInstance() {
		return instance.get();
	}

	public ObjectPropertiesView() {
		instance = new WeakReference<ObjectPropertiesView>(this);
	}

	@Override
	public void dispose() {
		instance = new WeakReference<ObjectPropertiesView>(null);
		super.dispose();
	}

	private static VerifyListener floatInput = new VerifyListener() {
		@Override
		public void verifyText(VerifyEvent e) {
			Text text = (Text) e.getSource();
			final String oldS = text.getText();
			String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
			if (newS.length() == 0)
				return;
			if ("-".equals(newS))
				return;
			try {
				Float.parseFloat(newS);
			} catch (NumberFormatException ex) {
				e.doit = false;
			}

		}
	};

	private Object3d object3d;

	private ModifyListener positionListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent arg0) {
			Object3d object3d = ObjectPropertiesView.this.object3d;
			if (object3d != null) {
				float x = Utils.parseFloat(inputTX.getText());
				float y = Utils.parseFloat(inputTY.getText());
				float z = Utils.parseFloat(inputTZ.getText());
				object3d.getPosition().setTranslation(x, y, z);
				sceneEditor.makeDirty("move");
			}
		}
	};

	private ModifyListener rotationListener = new ModifyListener() {

		@Override
		public void modifyText(ModifyEvent arg0) {
			Object3d object3d = ObjectPropertiesView.this.object3d;
			if (object3d != null) {
				float x = Utils.parseFloat(inputRX.getText());
				float y = Utils.parseFloat(inputRY.getText());
				float z = Utils.parseFloat(inputRZ.getText());
				object3d.getPosition().setEulerAngles(x, y, z);
				sceneEditor.makeDirty("rotate");
			}
		}
	};
	private ModifyListener scaleListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent arg0) {
			Object3d object3d = ObjectPropertiesView.this.object3d;
			if (object3d != null) {
				float x = Utils.parseFloat(inputSX.getText());
				float y = Utils.parseFloat(inputSY.getText());
				float z = Utils.parseFloat(inputSZ.getText());
				object3d.getPosition().setScale(x, y, z);
				sceneEditor.makeDirty("scale");
			}
		}
	};

	private SceneEditor sceneEditor;

	private ModifyListener setNameListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent arg0) {
			Object3d object3d = ObjectPropertiesView.this.object3d;
			if (object3d != null) {
				object3d.setName(objectName.getText());
				sceneEditor.makeDirty("rename");
			}
		}
	};

	private ScrolledComposite scrolledComposite;

	private Button savePrefab;

	private Button addProperty;

	private Composite propertiesComposite;

	public void createPartControl(Composite owner) {
		scrolledComposite = new ScrolledComposite(owner, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		parent = new Composite(scrolledComposite, SWT.WRAP);
		scrolledComposite.setContent(parent);
	}

	private void clear() {
		Utils.clear(parent);
	}

	private void rebuild(Object3d object3d, boolean editable) {
		clear();
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		Group header = new Group(parent, SWT.NONE);
		header.setLayoutData(Utils.fillGridHorizontal());
		GridLayout headerLayout = new GridLayout(2, false);
		header.setLayout(headerLayout);
		new Label(header, SWT.NONE).setText("Object name:");
		objectName = new Text(header, SWT.BORDER);
		objectName.setText(object3d.getName());
		objectName.setLayoutData(Utils.fillGridHorizontal());
		objectName.setEnabled(editable);
		objectName.addModifyListener(setNameListener);
		rebuildPosition(object3d.getPosition(), editable);
		
		propertiesComposite = new Composite(parent,  SWT.NONE);
		propertiesComposite.setLayoutData(Utils.fillGridHorizontal());
		GridLayout propertiesLayout = new GridLayout(1, false);
		propertiesComposite.setLayout(propertiesLayout);
		rebuildProperties(object3d, editable);

		Group footer = new Group(parent, SWT.NONE);
		footer.setLayoutData(Utils.fillGridHorizontal());
		GridLayout footerLayout = new GridLayout(2, false);
		footer.setLayout(footerLayout);
		addProperty = new Button(footer, SWT.NONE);
		addProperty.setText("Add property");
		addProperty.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				new PropertieAdder(addProperty, object3d, sceneEditor);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		savePrefab = new Button(footer, SWT.NONE);
		savePrefab.setText("Save as prefab");
		savePrefab.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				SceneController sceneController = sceneEditor.getSceneController();
				IFolder prefabs = sceneController.getPrefabFolder();
				IFile file = prefabs.getFile(object3d.getName() + ".prefab");
				
				if (file.exists()){
					MessageDialog dialog = new MessageDialog(null, "Replace", null, "Replace existing prefab" + object3d.getName() + " ?",
							MessageDialog.QUESTION, new String[] { "Yes", "No" }, 0);
					int result = dialog.open();
					if (result == 1)
						return;
				}
				InputStream stream = object3d.saveSingle();
				try {
					if (file.exists()) {
						file.setContents(stream, true, true, null);
					} else {
						file.create(stream, true, null);
					}
				} catch (CoreException e1) {
					e1.printStackTrace();
				}

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
	}

	private void rebuildPosition(Position position, boolean editable) {
		Group positionGroup = new Group(parent, SWT.NONE);
		positionGroup.setLayoutData(Utils.fillGridHorizontal());
		positionGroup.setText("Position");
		GridLayout positionLayout = new GridLayout(7, false);
		positionGroup.setLayout(positionLayout);
		Vector3f translation = position.getTranslation();
		new Label(positionGroup, SWT.NONE).setText("Translation:");
		new Label(positionGroup, SWT.NONE).setText("X");
		inputTX = new Text(positionGroup, SWT.BORDER);
		inputTX.setText("" + translation.x);
		inputTX.setLayoutData(Utils.fillGridHorizontal());
		inputTX.addVerifyListener(floatInput);
		inputTX.addModifyListener(positionListener);
		new Label(positionGroup, SWT.NONE).setText("Y");
		inputTY = new Text(positionGroup, SWT.BORDER);
		inputTY.setText("" + translation.y);
		inputTY.setLayoutData(Utils.fillGridHorizontal());
		inputTY.addVerifyListener(floatInput);
		inputTY.addModifyListener(positionListener);
		new Label(positionGroup, SWT.NONE).setText("Z");
		inputTZ = new Text(positionGroup, SWT.BORDER);
		inputTZ.setText("" + translation.z);
		inputTZ.setLayoutData(Utils.fillGridHorizontal());
		inputTZ.addVerifyListener(floatInput);
		inputTZ.addModifyListener(positionListener);
		Vector3f angles = position.getEulerAngles();
		new Label(positionGroup, SWT.NONE).setText("Rotation:");
		new Label(positionGroup, SWT.NONE).setText("X");
		inputRX = new Text(positionGroup, SWT.BORDER);
		inputRX.setText("" + angles.x);
		inputRX.setLayoutData(Utils.fillGridHorizontal());
		inputRX.addVerifyListener(floatInput);
		inputRX.addModifyListener(rotationListener);
		new Label(positionGroup, SWT.NONE).setText("Y");
		inputRY = new Text(positionGroup, SWT.BORDER);
		inputRY.setText("" + angles.y);
		inputRY.setLayoutData(Utils.fillGridHorizontal());
		inputRY.addVerifyListener(floatInput);
		inputRY.addModifyListener(rotationListener);
		new Label(positionGroup, SWT.NONE).setText("Z");
		inputRZ = new Text(positionGroup, SWT.BORDER);
		inputRZ.setText("" + angles.z);
		inputRZ.setLayoutData(Utils.fillGridHorizontal());
		inputRZ.addVerifyListener(floatInput);
		inputRZ.addModifyListener(rotationListener);
		Vector3f scale = position.getScale();
		new Label(positionGroup, SWT.NONE).setText("Scale:");
		new Label(positionGroup, SWT.NONE).setText("X");
		inputSX = new Text(positionGroup, SWT.BORDER);
		inputSX.setText("" + scale.x);
		inputSX.setLayoutData(Utils.fillGridHorizontal());
		inputSX.addVerifyListener(floatInput);
		inputSX.addModifyListener(scaleListener);
		new Label(positionGroup, SWT.NONE).setText("Y");
		inputSY = new Text(positionGroup, SWT.BORDER);
		inputSY.setText("" + scale.y);
		inputSY.setLayoutData(Utils.fillGridHorizontal());
		inputSY.addVerifyListener(floatInput);
		inputSY.addModifyListener(scaleListener);
		new Label(positionGroup, SWT.NONE).setText("Z");
		inputSZ = new Text(positionGroup, SWT.BORDER);
		inputSZ.setText("" + scale.z);
		inputSZ.setLayoutData(Utils.fillGridHorizontal());
		inputSZ.addVerifyListener(floatInput);
		inputSZ.addModifyListener(scaleListener);
	}

	private void rebuildProperties(Object3d object3d, boolean editable) {
		propertiesPanels.clear();
		Utils.clear(propertiesComposite);
		for (Property3d property3d : object3d.getProperties()) {
			rebuildProperty(property3d, object3d, editable);
		}
		propertiesComposite.layout(true, true);
	}

	private PropertyEditor rebuildProperty(Property3d property3d, Object3d object3d, boolean editable) {
		if (property3d instanceof EditorProperty)
			return null;
		PropertyEditor group;
		if (property3d instanceof MultiMesh) {
			group = new MultiMeshEditor(propertiesComposite, SWT.NONE, sceneEditor, object3d);
		} else {
			group = new PropertyEditor(propertiesComposite, SWT.NONE, sceneEditor, object3d);
		}
		group.renew(property3d, editable);
		propertiesPanels.put(property3d, group);
		return group;
	}

	@Override
	public void setFocus() {

	}

	public void setObject(SceneEditor sceneEditor, Object3d object3d, boolean editable) {
		this.sceneEditor = sceneEditor;
		boolean objectChange = this.object3d != object3d;
		this.object3d = null;
		if (objectChange && object3d != null) {
			rebuild(object3d, editable);
			parent.setSize(parent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			scrolledComposite.layout(true, true);
		}
		if (object3d != null) {
			Utils.setIfChangeString(objectName, object3d.getName());

			Position position = object3d.getPosition();
			Vector3f translation = position.getTranslation();
			Utils.setIfChangeFloat(inputTX, translation.x);
			inputTX.setEnabled(editable);
			Utils.setIfChangeFloat(inputTY, translation.y);
			inputTY.setEnabled(editable);
			Utils.setIfChangeFloat(inputTZ, translation.z);
			inputTZ.setEnabled(editable);
			Vector3f angles = position.getEulerAngles();
			Utils.setIfChangeFloat(inputRX, angles.x);
			inputRX.setEnabled(editable);
			Utils.setIfChangeFloat(inputRY, angles.y);
			inputRY.setEnabled(editable);
			Utils.setIfChangeFloat(inputRZ, angles.z);
			inputRZ.setEnabled(editable);
			Vector3f scale = position.getScale();
			Utils.setIfChangeFloat(inputSX, scale.x);
			inputSX.setEnabled(editable);
			Utils.setIfChangeFloat(inputSY, scale.y);
			inputSY.setEnabled(editable);
			Utils.setIfChangeFloat(inputSZ, scale.z);
			inputSZ.setEnabled(editable);
			boolean propertiesChanged = false;
			for (Property3d property3d : object3d.getProperties()) {
				PropertyEditor propertyEditor = propertiesPanels.get(property3d);
				if (propertyEditor == null) {
					rebuildProperty(property3d, object3d, objectChange);
					propertiesComposite.layout(true, true);
				}
				if (propertyEditor != null)
					propertyEditor.renew(property3d, editable);					
			}
			if(propertiesChanged)
				propertiesComposite.layout(true, true);
			addProperty.setEnabled(editable);
			scrolledComposite.layout(true, true);
		} else {
			clear();
			Label label = new Label(parent, SWT.BORDER);
			label.setText("Select object or material first");
			label.setLayoutData(Utils.fillGridHorizontal());
			scrolledComposite.layout(true, true);
		}
		this.object3d = object3d;
	}

}
