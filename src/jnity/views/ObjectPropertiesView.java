package jnity.views;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.lwjgl.util.vector.Vector3f;

import base.Object3d;
import base.Position;
import io.ResourceController;
import jnity.views.editor.SceneController;
import properties.Mesh;
import properties.MultiMesh;
import properties.Property3d;
import properties.SelectionOverlay;

public class ObjectPropertiesView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */

	
	private static WeakReference<ObjectPropertiesView> instance = new WeakReference<ObjectPropertiesView>(null);

	public static final String ID = "jnity.views.ObjectPropertiesView";
	
	
	private Map<Property3d, Composite> propertiesPanels = new HashMap<>();
	
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
				float x = parseFloat(inputTX.getText());
				float y = parseFloat(inputTY.getText());
				float z = parseFloat(inputTZ.getText());
				object3d.getPosition().setTranslation(x, y, z);
				sceneEditor.makeDirty("move");
			}
		}
	};

	private static void setIfChangeFloat(Text input, float value) {
		if (parseFloat(input.getText()) != value) {
			input.setText("" + value);
		}
	}

	private static float parseFloat(String text) {
		try {
			return Float.parseFloat(text);
		} catch (Exception e) {
			return 0;
		}
	}

	private ModifyListener rotationListener = new ModifyListener() {

		@Override
		public void modifyText(ModifyEvent arg0) {
			Object3d object3d = ObjectPropertiesView.this.object3d;
			if (object3d != null) {
				float x = parseFloat(inputRX.getText());
				float y = parseFloat(inputRY.getText());
				float z = parseFloat(inputRZ.getText());
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
				float x = parseFloat(inputSX.getText());
				float y = parseFloat(inputSY.getText());
				float z = parseFloat(inputSZ.getText());
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

	public void createPartControl(Composite owner) {
		scrolledComposite = new ScrolledComposite(owner, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolledComposite.setExpandHorizontal(true);  
		parent = new Composite(scrolledComposite, SWT.WRAP);
		scrolledComposite.setContent(parent);
	}

	private void clear() {
		for (Control control : parent.getChildren()) {
			control.dispose();
		}
	}

	private void rebuild(Object3d object3d, boolean editable) {
		clear();
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		Group header = new Group(parent, SWT.NONE);
		header.setLayoutData(fillGridHorizontal());
		GridLayout headerLayout = new GridLayout(2, false);
		header.setLayout(headerLayout);
		new Label(header, SWT.NONE).setText("Object name:");
		objectName = new Text(header, SWT.BORDER);
		objectName.setText(object3d.getName());
		objectName.setLayoutData(fillGridHorizontal());
		objectName.setEnabled(editable);
		objectName.addModifyListener(setNameListener);
		rebuildPosition(object3d.getPosition(), editable);
		rebuildProperties(object3d, editable);
	}

	private void rebuildPosition(Position position, boolean editable) {
		Group positionGroup = new Group(parent, SWT.NONE);
		positionGroup.setLayoutData(fillGridHorizontal());
		positionGroup.setText("Position");
		GridLayout positionLayout = new GridLayout(7, false);
		positionGroup.setLayout(positionLayout);
		Vector3f translation = position.getTranslation();
		new Label(positionGroup, SWT.NONE).setText("Translation:");
		new Label(positionGroup, SWT.NONE).setText("X");
		inputTX = new Text(positionGroup, SWT.BORDER);
		inputTX.setText("" + translation.x);
		inputTX.setLayoutData(fillGridHorizontal());
		inputTX.addVerifyListener(floatInput);
		inputTX.addModifyListener(positionListener);
		new Label(positionGroup, SWT.NONE).setText("Y");
		inputTY = new Text(positionGroup, SWT.BORDER);
		inputTY.setText("" + translation.y);
		inputTY.setLayoutData(fillGridHorizontal());
		inputTY.addVerifyListener(floatInput);
		inputTY.addModifyListener(positionListener);
		new Label(positionGroup, SWT.NONE).setText("Z");
		inputTZ = new Text(positionGroup, SWT.BORDER);
		inputTZ.setText("" + translation.z);
		inputTZ.setLayoutData(fillGridHorizontal());
		inputTZ.addVerifyListener(floatInput);
		inputTZ.addModifyListener(positionListener);
		Vector3f angles = position.getEulerAngles();
		new Label(positionGroup, SWT.NONE).setText("Rotation:");
		new Label(positionGroup, SWT.NONE).setText("X");
		inputRX = new Text(positionGroup, SWT.BORDER);
		inputRX.setText("" + angles.x);
		inputRX.setLayoutData(fillGridHorizontal());
		inputRX.addVerifyListener(floatInput);
		inputRX.addModifyListener(rotationListener);
		new Label(positionGroup, SWT.NONE).setText("Y");
		inputRY = new Text(positionGroup, SWT.BORDER);
		inputRY.setText("" + angles.y);
		inputRY.setLayoutData(fillGridHorizontal());
		inputRY.addVerifyListener(floatInput);
		inputRY.addModifyListener(rotationListener);
		new Label(positionGroup, SWT.NONE).setText("Z");
		inputRZ = new Text(positionGroup, SWT.BORDER);
		inputRZ.setText("" + angles.z);
		inputRZ.setLayoutData(fillGridHorizontal());
		inputRZ.addVerifyListener(floatInput);
		inputRZ.addModifyListener(rotationListener);
		Vector3f scale = position.getScale();
		new Label(positionGroup, SWT.NONE).setText("Scale:");
		new Label(positionGroup, SWT.NONE).setText("X");
		inputSX = new Text(positionGroup, SWT.BORDER);
		inputSX.setText("" + scale.x);
		inputSX.setLayoutData(fillGridHorizontal());
		inputSX.addVerifyListener(floatInput);
		inputSX.addModifyListener(scaleListener);
		new Label(positionGroup, SWT.NONE).setText("Y");
		inputSY = new Text(positionGroup, SWT.BORDER);
		inputSY.setText("" + scale.y);
		inputSY.setLayoutData(fillGridHorizontal());
		inputSY.addVerifyListener(floatInput);
		inputSY.addModifyListener(scaleListener);
		inputSY.setEnabled(editable);
		new Label(positionGroup, SWT.NONE).setText("Z");
		inputSZ = new Text(positionGroup, SWT.BORDER);
		inputSZ.setText("" + scale.z);
		inputSZ.setLayoutData(fillGridHorizontal());
		inputSZ.addVerifyListener(floatInput);
		inputSZ.addModifyListener(scaleListener);
	}

	private void rebuildProperties(Object3d object3d, boolean editable) {
		propertiesPanels.clear();
		for (Property3d property3d : object3d.getProperties()) {
			if (property3d instanceof SelectionOverlay)
				continue;
			Group group = new Group(parent, SWT.NONE);
			group.setLayoutData(fillGridHorizontal());
			group.setText(property3d.getClass().getSimpleName());
			GridLayout positionLayout = new GridLayout(2, false);
			group.setLayout(positionLayout);
			if(property3d instanceof MultiMesh){
				try {
					MultiMesh multiMesh = (MultiMesh) property3d;
					new Label(group, SWT.NONE).setText("Mesh file:");
					Combo meshSelector = new Combo(group, SWT.NONE);
					SceneController sceneController = sceneEditor.getSceneController();
					for(IResource file:sceneController.getModelFolder().members()){
						meshSelector.add(file.getName());
					}
					meshSelector.setText(multiMesh.getFileName());
					meshSelector.addSelectionListener(new SelectionAdapter() {							
						@Override
						public void widgetSelected(SelectionEvent e) {	
							Combo combo = (Combo) e.getSource();
							if((combo.getText()!=null)&&(!multiMesh.getFileName().equals(combo.getText()))){
								ResourceController.getOrCreate().getOrLoadMesh(multiMesh, combo.getText());
								sceneEditor.makeDirty("Mesh model change");
							}
						}
					});
					int num = 1;
					for(Mesh mesh:multiMesh.getMeshes()){
						new Label(group, SWT.NONE).setText("Material "+ (num++)+ ":");
						Combo materialSelector = new Combo(group, SWT.NONE);
						for(String material:sceneController.getScene().getMaterialLibrary().getMaterialNames()){
							materialSelector.add(material);
						}
						materialSelector.setText(mesh.getMaterialName());
						materialSelector.addSelectionListener(new SelectionAdapter() {							
							@Override
							public void widgetSelected(SelectionEvent e) {	
								Combo combo = (Combo) e.getSource();
								
								if((combo.getText()!=null)&&(!mesh.getMaterialName().equals(combo.getText()))){
									mesh.setMaterialName(combo.getText());
									sceneEditor.makeDirty("Mesh material change");
								}
							}
						});
					}
					
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private GridData fillGridHorizontal() {
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		return gridData;
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
			
			objectName.setText(object3d.getName());
			
			Position position = object3d.getPosition();
			Vector3f translation = position.getTranslation();
			setIfChangeFloat(inputTX, translation.x);
			inputTX.setEnabled(editable);
			setIfChangeFloat(inputTY, translation.y);
			inputTY.setEnabled(editable);
			setIfChangeFloat(inputTZ, translation.z);
			inputTZ.setEnabled(editable);
			Vector3f angles = position.getEulerAngles();
			setIfChangeFloat(inputRX, angles.x);
			inputRX.setEnabled(editable);
			setIfChangeFloat(inputRY, angles.y);
			inputRY.setEnabled(editable);
			setIfChangeFloat(inputRZ, angles.z);
			inputRZ.setEnabled(editable);
			Vector3f scale = position.getScale();
			setIfChangeFloat(inputSX, scale.x);
			inputSX.setEnabled(editable);
			setIfChangeFloat(inputSY, scale.y);
			inputSY.setEnabled(editable);
			setIfChangeFloat(inputSZ, scale.z);
			inputSZ.setEnabled(editable);
			
			for(Property3d property3d:object3d.getProperties()){
				Composite composite = propertiesPanels.get(property3d);
				if(composite!=null) {
					
				}
			}
			//String properties = .stream().map(p -> p.getClass().getSimpleName())
			//		.collect(Collectors.joining(","));
			//objectProperties.setText(properties);
		} else {
			clear();
			Label label = new Label(parent, SWT.BORDER);
			label.setText("Select object or material first");
			label.setLayoutData(fillGridHorizontal());
			scrolledComposite.layout(true, true);
		}
		this.object3d = object3d;
	}

}
