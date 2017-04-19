package jnity.views;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.UndoContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ResourceTransfer;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.vector.Vector3f;

import base.Camera;
import base.Object3d;
import io.ResourceController;
import jnity.SampleHandler;
import jnity.Utils;
import jnity.views.editor.ClipboardUtils;
import jnity.views.editor.ProjectResourseListener;
import jnity.views.editor.SceneDropTarget;
import jnity.views.editor.SceneEditorListener;
import jnity.views.editor.UndoableOperation;
import physics.PhysicController;

public class SceneEditor extends EditorPart {


	GLCanvas canvas;
	String filePath;

	private Set<Integer> keys = new HashSet<>();

	IProject project;
	private FileEditorInput fileEditorInput;
	private SceneController sceneController;
	private ProjectResourseListener sceneResourseController;
	private boolean dirty;
	private UndoContext undoContext;
	private IOperationHistory operationHistory;

	@Override
	public void doSave(IProgressMonitor monitor) {
		IEditorInput input = getEditorInput();
		if (input instanceof FileEditorInput) {
			FileEditorInput fileEditorInput = (FileEditorInput) input;
			IFile file = fileEditorInput.getFile();
			try {
				InputStream stream = sceneController.openContentStream();
				if (file.exists()) {
					file.setContents(stream, true, true, monitor);
				} else {
					file.create(stream, true, monitor);
				}
				stream.close();
			} catch (Exception e) {
			}
		}
		monitor.worked(1);
		dirty = false;
		setPartName(fileEditorInput.getName());
		firePropertyChange(PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		undoContext = new UndoContext();
		UndoActionHandler undoAction = new UndoActionHandler(this.getSite(), undoContext);
		RedoActionHandler redoAction = new RedoActionHandler(this.getSite(), undoContext);
		IActionBars actionBars = getEditorSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
		actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
		actionBars.updateActionBars();
		IWorkbench workbench = getSite().getWorkbenchWindow().getWorkbench();
		operationHistory = workbench.getOperationSupport().getOperationHistory();
		fileEditorInput = (FileEditorInput) input;
		try {
			setPartName(fileEditorInput.getName());
			IFile file = fileEditorInput.getFile();
			project = file.getProject();
			
			if (!project.isOpen())
				project.open(null);
			SampleHandler.printProjectInfo(project);
			sceneResourseController = new ProjectResourseListener();
			project.getWorkspace().addResourceChangeListener(sceneResourseController);
			sceneController = new SceneController(sceneResourseController);
			sceneController.initDirs(project);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public void makeDirty(String info) {
		if (sceneController.isPlaying())
			return;// any changes during playing is ignored
		try {
			operationHistory.execute(new UndoableOperation(info, sceneController, undoContext), null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		setPartName("*" + fileEditorInput.getName());
		dirty = true;
		firePropertyChange(PROP_DIRTY);
		SceneTreeView sceneTreeView = SceneTreeView.getInstance();
		if (sceneTreeView != null) {
			sceneTreeView.setSceneEditor(this);
		}
		ObjectPropertiesView objectPropertiesView = ObjectPropertiesView.getInstance();
		if (objectPropertiesView != null) {
			objectPropertiesView.setObject(this, sceneController.getSelectedObject(), !sceneController.isPlaying());
		}
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		ResourceController.getOrCreate().emptyCache();
		GLData data = new GLData();
		data.doubleBuffer = true;
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		initControls(parent);

		canvas = new GLCanvas(parent, SWT.NONE, data);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		canvas.setLayoutData(gridData);
		initDropTarget();
		canvas.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				keys.clear();
				
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		canvas.setCurrent();
		try {
			GLContext.useContext(canvas);
		} catch (LWJGLException e) {
			e.printStackTrace();
			setPartName(e.getMessage());
		}
		Utils.initGL();
		FileEditorInput fileEditorInput = (FileEditorInput) getEditorInput();
		IFile file = fileEditorInput.getFile();
		try {
			sceneController.loadScene(file.getContents());
		} catch (CoreException e1) {
			e1.printStackTrace();
		}

		canvas.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				Rectangle bounds = canvas.getBounds();
				canvas.setCurrent();
				try {
					GLContext.useContext(canvas);
					sceneController.getCamera().width = bounds.width;
					sceneController.getCamera().height = bounds.height;
				} catch (LWJGLException e) {
					setPartName(e.getMessage());
				}
			}
		});
		SceneEditorListener sceneEditorListener = new SceneEditorListener(this, sceneController);
		canvas.addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseScrolled(MouseEvent e) {
				if (canvas.isFocusControl()) {
					Camera camera = sceneController.getCamera();
					float speed = 0.5f;
					int wheelCount = e.count;
					if (wheelCount > 0)
						camera.getPosition().move((Vector3f) camera.getPosition().getFrontVector().negate().scale(speed));
					else
						camera.getPosition().move((Vector3f) camera.getPosition().getFrontVector().scale(speed));
				}
			}
		});
		canvas.addListener(SWT.MouseMove, sceneEditorListener);
		canvas.addListener(SWT.MouseDown, sceneEditorListener);
		canvas.addListener(SWT.MouseUp, sceneEditorListener);
		canvas.addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {
				Object3d selected = sceneController.getSelectedObject();
				if ((e.keyCode == (int) 'x') && keys.contains(Utils.KEY_CTRL) && selected != null) {
					ClipboardUtils.setClipboardContents(selected.fastClone());
					sceneController.getScene().remove(sceneController.getSelectedObject());
					sceneController.setSelectedObject(null);
					makeDirty("Cut Object");
				}
				if ((e.keyCode == (int) 'c') && keys.contains(Utils.KEY_CTRL) && selected != null) {
					ClipboardUtils.setClipboardContents(selected.fastClone());
				}
				if ((e.keyCode == (int) 'v') && keys.contains(Utils.KEY_CTRL)) {
					Object3d object3d = ClipboardUtils.getClipboardContents();
					if (object3d != null) {
						sceneController.getScene().add(object3d);
						sceneController.setSelectedObject(object3d);// ?
						makeDirty("Insert Object");
					}
				}
				if ((e.keyCode == Utils.KEY_DELETE) && (sceneController.getSelectedObject() != null)) {
					sceneController.getScene().remove(sceneController.getSelectedObject());
					sceneController.setSelectedObject(null);
					makeDirty("delete");
				}
				//keys.remove(e.keyCode);
				keys.clear();
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if ((e.stateMask & SWT.MODIFIER_MASK) == 0)
					keys.add(e.keyCode);
			}
		});

		SceneTreeView sceneTreeView = SceneTreeView.getInstance();
		if (sceneTreeView != null) {
			sceneTreeView.setSceneEditor(this);
		}

		Display.getCurrent().timerExec(10, initRunnable());

	}

	private void initControls(Composite parent) {

		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.heightHint = 32;
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayoutData(gridData);
		control.setLayout(new RowLayout());
		Button play = new Button(control, SWT.CENTER);
		play.setText("Play");
		
		Button pause = new Button(control, SWT.CENTER);
		pause.setText("Pause");
		
		Button stop = new Button(control, SWT.CENTER);
		stop.setText("Stop");
		
		Button rebase = new Button(control, SWT.CENTER);
		rebase.setText("Rebase");
		
		SelectionListener listener = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Button sourse = (Button) e.getSource();
				switch (sourse.getText()) {
				case "Play":
					sceneController.play();
					play.setEnabled(false);
					pause.setEnabled(true);
					stop.setEnabled(true);
					rebase.setEnabled(true);
					break;
				case "Resume":
				case "Pause":
					sceneController.pause();
					pause.setText(sceneController.isPlaying() ? "Pause" : "Resume");
					break;
				case "Stop":
					sceneController.stop();
					play.setEnabled(true);
					pause.setEnabled(false);
					pause.setText("Pause");
					stop.setEnabled(false);
					rebase.setEnabled(false);
					break;
				case "Rebase":
					sceneController.rebase();
					play.setEnabled(true);
					pause.setEnabled(false);
					pause.setText("Pause");
					stop.setEnabled(false);
					rebase.setEnabled(false);
					break;
				default:
					break;
				}
				setObjectToEdit();
			}
		};
		
		play.addSelectionListener(listener);
		pause.addSelectionListener(listener);
		pause.setEnabled(false);
		stop.addSelectionListener(listener);
		stop.setEnabled(false);
		rebase.addSelectionListener(listener);
		rebase.setEnabled(false);
	}

	private void initDropTarget() {
		int operations = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] types = new Transfer[] { FileTransfer.getInstance(), ResourceTransfer.getInstance() };
		DropTarget dropTarget = new DropTarget(canvas, operations);
		dropTarget.setTransfer(types);
		dropTarget.addDropListener(new SceneDropTarget(this, sceneController));
	}

	Runnable initRunnable() {
		return new Runnable() {
			long time = System.currentTimeMillis();

			public void run() {
				if (canvas.isDisposed())
					return;
				if (canvas.isVisible()) {
					float delta = (System.currentTimeMillis() - time) / 1000f;
					canvas.setCurrent();
					try {
						GLContext.useContext(canvas);
						if (sceneController.isPlaying()) {
							PhysicController.getDefault().step(delta, 100);
							sceneController.getScene().tick(delta);
							setObjectToEdit();
						}
						Camera camera = sceneController.getCamera();
						float speed = 0.5f;
						if (keys.contains((int) 'w'))
							camera.getPosition()
									.move((Vector3f) camera.getPosition().getFrontVector().negate().scale(speed));
						if (keys.contains((int) 's'))
							camera.getPosition().move((Vector3f) camera.getPosition().getFrontVector().scale(speed));
						if (keys.contains((int) 'a'))
							camera.getPosition().move((Vector3f) camera.getPosition().getLeftVector().scale(speed));
						if (keys.contains((int) 'd'))
							camera.getPosition()
									.move((Vector3f) camera.getPosition().getLeftVector().negate().scale(speed));
						if (keys.contains((int) 'e'))
							camera.getPosition().move((Vector3f) camera.getPosition().getUpVector().scale(speed));
						if (keys.contains((int) 'q'))
							camera.getPosition()
									.move((Vector3f) camera.getPosition().getUpVector().negate().scale(speed));
						sceneController.render();

					} catch (LWJGLException e) {
						e.printStackTrace();
					}
					time = System.currentTimeMillis();
					canvas.swapBuffers();
				}
				Display.getCurrent().timerExec(0, this);
			}
		};
	}

	public void setObjectToEdit() {
		ObjectPropertiesView objectPropertiesView = ObjectPropertiesView.getInstance();
		if (objectPropertiesView != null) {
			objectPropertiesView.setObject(this, sceneController.getSelectedObject(), !sceneController.isPlaying());
		}
	}

	@Override
	public void setFocus() {
		SceneTreeView sceneTreeView = SceneTreeView.getInstance();
		if (sceneTreeView != null) {
			sceneTreeView.setSceneEditor(this);
		}
		canvas.setFocus();
		setObjectToEdit();
	}

	public Object3d getObjectFromScreenPoint(int x, int y) {
		Point point = new Point(x, y);
		point = canvas.toControl(point);
		return sceneController.getObject(point.x, point.y);
	}

	public Object3d getObject(int x, int y) {
		try {
			GLContext.useContext(canvas);
			return sceneController.getObject(x, y);
		} catch (LWJGLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public SceneController getSceneController() {
		return sceneController;
	}

}
