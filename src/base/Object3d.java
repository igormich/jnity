package base;

import static org.lwjgl.opengl.GL11.glColor3f;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import properties.Property3d;

public class Object3d implements Externalizable, FastCloneable {

	private static final long serialVersionUID = 906802098539655519L;
	static final AtomicInteger idCounter = new AtomicInteger(1);
	private final int id = idCounter.getAndIncrement();

	private Position position = new Position(this);
	private List<Object3d> children = new ArrayList<Object3d>();
	private Object3d parent = null;
	private List<Property3d> properties = new ArrayList<Property3d>();
	private float hideDistance = 1000000;
	private boolean visible = true;
	private String name = "";

	@Override
	public String toString() {
		return name;
	}
	public Object3d() {
		setName("Object " + getID());
	}
	public Object3d(Property3d... properties) {
		setName("Object " + getID());
		for (Property3d property : properties)
			add(property);
	}

	public Position getPosition() {
		return position;
	}

	public Position resetPosition() {
		position = new Position(this);
		return position;
	}

	private void setParent(Object3d parent) {
		if (parent == null) {
			this.parent = null;
			return;
		}
		Object3d nextParent = parent;
		while (nextParent != null) {
			if (nextParent == this)
				throw new IllegalStateException("Recursive hierarchy");
			nextParent = nextParent.getParent();
		}
		position.rebuildRelativeTo(parent.getPosition());
		if (this.parent != null)
			this.parent.removeChild(this);
		this.parent = parent;
	}

	@Override
	public int hashCode() {
		return getID();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Object3d other = (Object3d) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public void removeChild(Object3d object3d) {
		object3d.setParent(null);
		if (!children.remove(object3d)) {
			children.forEach(obj -> obj.removeChild(object3d));
		}
	}

	public Object3d getParent() {
		return parent;
	}

	public List<Object3d> getChildren() {
		return Collections.unmodifiableList(children);
	}

	public Object3d add(Property3d property) {
		if (property.isUnique()) {
			Optional<Property3d> propertyForReplace = properties.stream()
					.filter(p -> p.getClass() == property.getClass()).findAny();
			if (propertyForReplace.isPresent()) {
				int index = properties.indexOf(propertyForReplace.get());
				Property3d oldProperty = properties.get(index);
				oldProperty.unRegister(this);
				property.register(this);
				properties.set(index, property);
			} else {
				property.register(this);
				properties.add(property);
			}
		} else {
			property.register(this);
			properties.add(property);
		}
		return this;
	}

	public <T extends Property3d> void removeAll(Class<T> type) {
		properties.stream().filter(property -> property.getClass().isInstance(type))
				.forEach(property -> property.unRegister(this));
		properties.removeIf(property -> property.getClass().equals(type));
	}
	public void remove(Property3d property) {
		property.unRegister(this);
		properties.remove(property);
	}

	public <T extends Property3d> T get(Class<T> type) {
		return get(type, null);
	}

	public <T extends Property3d> T get(Class<T> type, T defaultValue) {
		@SuppressWarnings("unchecked")
		Optional<T> result = (Optional<T>) properties.stream().filter(p -> p.getClass() == type).findAny();
		return result.orElse(defaultValue);
	}

	public List<Property3d> getProperties() {
		return Collections.unmodifiableList(properties);
	}

	public void tick(float deltaTime, float time) {
		position.apply();
		getChildren().forEach(child -> child.tick(deltaTime, time));
		properties.forEach(property -> property.tick(deltaTime, time, this));
		position.unApply();
	}

	public void render(RenderContex renderContex) {
		if (!visible)
			return;
		position.apply();
		getChildren().forEach(child -> child.render(renderContex));
		// if(renderContex.isVisible(getPosition().getAbsoluteTranslation())<hideDistance){
		if (renderContex.selectMode()) {
			int r = getID() & 0xff;
			int g = (getID() >> 8) & 0xff;
			int b = (getID() >> 16) & 0xff;
			glColor3f(r / 255f, g / 255f, b / 255f);
		}
		properties.forEach(property -> property.render(renderContex, this));
		// }
		position.unApply();
	}

	public Integer getID() {
		return id;
	}

	public Object3d getByID(int id) {
		if (id == getID())
			return this;
		for (Object3d o : getChildren()) {
			Object3d result = o.getByID(id);
			if (result != null)
				return result;
		}
		return null;
	}

	public Object3d setHideDistance(float hideDistance) {
		this.hideDistance = hideDistance;
		return this;
	}

	public float getHideDistance() {
		return hideDistance;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void renderSingle(RenderContex renderContex) {
		position.applyAbsolyte();
		properties.stream().forEach(property -> property.render(renderContex, this));
		position.unApply();
	}

	@Override
	public Object3d fastClone() {
		Object3d result = new Object3d();
		result.position = position.fastClone(result);
		result.visible = visible;
		properties.stream()
			.map(property -> property.fastClone())
			.filter(property -> property!=null)
			.forEach(property -> result.add(property));
		getChildren().forEach(child -> result.addChild(child.fastClone()));
		result.position.invalidate();
		return result;
	}

	public boolean contains(Object3d object) {
		if (object == this)
			return true;
		if (getChildren().contains(object))
			return true;
		for (Object3d child : children) {
			if (child.contains(object))
				return true;
		}
		return false;
	}

	public String getName() {
		if (name == null)
			name = "Object " + id;
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object3d addChild(Object3d object3d) {
		if(getChildren().contains(object3d))
			return this;
		object3d.setParent(this);
		children.add(object3d);
		return this;
	};
	
	public Object3d addChildBefore(Object3d object3d, Object3d target) {
		if(getChildren().contains(object3d))
			return this;
		object3d.setParent(this);
		int index = children.indexOf(target);
		children.add(index, object3d);
		return this;
	}

	public Object3d addChildAfter(Object3d object3d, Object3d target) {
		if(getChildren().contains(object3d))
			return this;
		object3d.setParent(this);
		int index = children.indexOf(target);
		children.add(index + 1, object3d);
		return this;
	}

	public InputStream saveSingle() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			Object3d toSave = fastClone();
			toSave.resetPosition();
			oos.writeObject(toSave);
			oos.flush();
			oos.close();
			oos.flush();
			oos.close();
			
			return new ByteArrayInputStream(baos.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ByteArrayInputStream(new byte[] {});
	}

	public void postLoad() {
		position.apply();
		getChildren().forEach(child -> child.postLoad());
		properties.forEach(property -> property.register(this));
		position.unApply();
		
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(name);
		out.writeObject(position);
		ArrayList<Property3d> propertiesToSave = new ArrayList<Property3d>();
		propertiesToSave.addAll(properties);
		propertiesToSave.removeIf(property -> property.isTransient());
		out.writeObject(propertiesToSave);
		out.writeObject(parent);
		out.writeObject(children);
		out.writeFloat(hideDistance);
		out.writeBoolean(visible);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		name = in.readUTF();
		position = (Position) in.readObject();
		properties = (List<Property3d>) in.readObject();
		parent = (Object3d) in.readObject();	
		children = (List<Object3d>) in.readObject();
		hideDistance = in.readFloat();
		visible = in.readBoolean();
		
	}
}
