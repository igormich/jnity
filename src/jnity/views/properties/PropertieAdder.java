package jnity.views.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import base.Object3d;
import jnity.views.SceneEditor;
import physics.AbstractCollisionBody;
import properties.ConvexCollisionBody;
import properties.NotConvexCollisionBody;

public class PropertieAdder implements SelectionListener {


	private Object3d object3d;
	private MenuItem addDynamicPhysicBody;
	private MenuItem addStaticPhysicBody;
	private SceneEditor sceneEditor;

	public PropertieAdder(Control control, Object3d object3d,SceneEditor sceneEditor) {
		this.object3d = object3d;
		this.sceneEditor = sceneEditor;
		Menu menu = new Menu(control);
		addStaticPhysicBody = new MenuItem(menu, SWT.NONE);
		addStaticPhysicBody.setText("add Static Physic Body");
		addStaticPhysicBody.addSelectionListener(this);
		
		addDynamicPhysicBody = new MenuItem(menu, SWT.NONE);
		addDynamicPhysicBody.setText("add Dynamic Physic Body");
		addDynamicPhysicBody.addSelectionListener(this);
		
		Rectangle bounds = control.getBounds();
		Point point = control.getParent().toDisplay(bounds.x, bounds.y + bounds.height);
		menu.setLocation(point);

		menu.setVisible(true);
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		MenuItem menuItem = (MenuItem) e.widget;
		if (menuItem == addDynamicPhysicBody) {
			AbstractCollisionBody body = new ConvexCollisionBody();
			body.setMass(1);
			object3d.add(body);
		}
		if (menuItem == addStaticPhysicBody) {
			AbstractCollisionBody body = new NotConvexCollisionBody();
			body.setFriction(10);
			object3d.add(body);
		}
		sceneEditor.makeDirty(menuItem.getText());
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// TODO Auto-generated method stub
		
	}

}
