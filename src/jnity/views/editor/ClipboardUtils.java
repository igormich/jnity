package jnity.views.editor;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.Toolkit;
import java.io.*;

import base.Object3d;

public final class ClipboardUtils {
	private static final DataFlavor Object3dDataFlavor = new DataFlavor(Object3d.class, "Object3d");
	private static class Object3dSelection implements Transferable, ClipboardOwner {
 
		private Object3d data;

		public Object3dSelection(Object3d object3d) {
			data = object3d;
		}

		@Override
		public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException, IOException {
			if (isDataFlavorSupported(dataFlavor))
				return data;
			throw new UnsupportedFlavorException(dataFlavor);
		}

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[]{Object3dDataFlavor};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
			return Object3dDataFlavor.equals(dataFlavor);
		}

		@Override
		public void lostOwnership(Clipboard clipboard, Transferable contents) {
			System.out.println("lostOwnership");
		}
		
	}


  public static void setClipboardContents(Object3d object3d){
	Object3dSelection object3dSelection = new Object3dSelection(object3d);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(object3dSelection, object3dSelection);
  }

  public static Object3d getClipboardContents() {
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    Transferable contents = clipboard.getContents(null);
    if(contents.isDataFlavorSupported(Object3dDataFlavor)) {
    	try {
			Object3d result = (Object3d) contents.getTransferData(Object3dDataFlavor);
			return result.fastClone();
		} catch (UnsupportedFlavorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	return null;
   
  }
} 