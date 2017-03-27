package jnity.views;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Toolkit;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public class Utils {

	public static final int KEY_DELETE = 0x7f;
	public static final int KEY_CTRL = 0x40000;
	public static void initDisplay(int w,int h,boolean fullscreen) {
		try {
			Dimension screenSize = new Dimension(w, h);
			if(w==0)
			screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			DisplayMode targetDisplayMode=null;
			for(DisplayMode mode:Display.getAvailableDisplayModes()){ 			
				if((mode.getWidth()==screenSize.width)&&(mode.getHeight()==screenSize.height))
					if ((targetDisplayMode == null) || (mode.getFrequency() >= targetDisplayMode.getFrequency())) 
                        if ((targetDisplayMode == null) || (mode.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel())) 
                            targetDisplayMode = mode;
			}
			System.out.println(targetDisplayMode);
			if(targetDisplayMode==null)
				targetDisplayMode=new DisplayMode(w, h);
			Display.setDisplayMode(targetDisplayMode);
			Display.setVSyncEnabled(false);
			if(fullscreen)
				Display.setFullscreen(true);
			Display.create();
		} catch (LWJGLException e) {
			Display.destroy();
			e.printStackTrace();
			System.exit(0);
		}
		initGL();
	}
	public static void initGL() {
		//glViewport (0, 0, Display.getWidth(), Display.getHeight());	// Reset The Current Viewport							// Select The Modelview Matrix
		glLoadIdentity ();											// Reset The Modelview Matrix		
		glClearColor (1.0f, 1.0f, 1.0f, 0.5f);						// Black Background
		glClearDepth (1.0f);										// Depth Buffer Setup
		glClearStencil(0); 
		glDepthFunc (GL_LEQUAL);									// The Type Of Depth Testing (Less Or Equal)
		glBlendFunc(GL_DST_COLOR, GL_SRC_COLOR);
		glEnable (GL_DEPTH_TEST);									// Enable Depth Testing
		glShadeModel (GL_SMOOTH);									// Select Smooth Shading
		glHint (GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);			// Set Perspective Calculations To Most Accurate
		glEnable ( GL_COLOR_MATERIAL ) ;
		glEnable(GL_CULL_FACE);
		glEnable(GL_DOUBLEBUFFER);
		glCullFace(GL_BACK);
	}
	public static void initDisplay(int w, int h, Canvas canvas) {
		try {
			Dimension screenSize = new Dimension(w, h);
			if(w==0)
			screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			DisplayMode targetDisplayMode=null;
			for(DisplayMode mode:Display.getAvailableDisplayModes()){ 			
				if((mode.getWidth()==screenSize.width)&&(mode.getHeight()==screenSize.height))
					if ((targetDisplayMode == null) || (mode.getFrequency() >= targetDisplayMode.getFrequency())) 
                        if ((targetDisplayMode == null) || (mode.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel())) 
                            targetDisplayMode = mode;
			}
			System.out.println(targetDisplayMode);
			if(targetDisplayMode==null)
				targetDisplayMode=new DisplayMode(w, h);
			Display.setDisplayMode(targetDisplayMode);
			Display.setVSyncEnabled(false);
			Display.setParent(canvas);
			Display.create();
			
		} catch (Exception e) {
			Display.destroy();
			e.printStackTrace();
			System.exit(0);
		}
		initGL();
		
	}
	public static GridData fillGridHorizontal() {
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		return gridData;
	}
	public static void clear(Composite parent) {
		for (Control control : parent.getChildren()) {
			control.dispose();
		}
	}
	public static void setIfChangeFloat(Text input, float value) {
		if (parseFloat(input.getText()) != value) {
			input.setText("" + value);
		}
	}

	public static float parseFloat(String text) {
		try {
			return Float.parseFloat(text);
		} catch (Exception e) {
			return 0;
		}
	}
	public static void setIfChangeString(Text input, String value) {
		if (!input.getText().equals(value)) {
			input.setText(value);
		}
		
	}
	public static void setIfChangeString(Group input, String value) {
		if (!input.getText().equals(value)) {
			input.setText(value);
		}
	}
	public static void setIfChangeString(Combo input, String value) {
		if (!input.getText().equals(value)) {
			input.setText(value);
		}	
	}
}
