package jnity.views.tree;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Policy;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;

import base.Object3d;

public class TreeTransfer extends ByteArrayTransfer {

	 private static final String TYPE_NAME = "tree-transfer"; //$NON-NLS-1$;

	    private static final int TYPEID = registerType(TYPE_NAME);

	    private static final TreeTransfer INSTANCE = new TreeTransfer();

	    private Object3d selection;

	    private long selectionSetTime;


	    protected TreeTransfer() {
	    	// do nothing
	    }

		public static TreeTransfer getInstance() {
			return INSTANCE;
		}

	    public Object3d getObject() {
	        return selection;
	    }

	    private boolean isInvalidNativeType(Object result) {
	        return !(result instanceof byte[])
	                || !TYPE_NAME.equals(new String((byte[]) result));
	    }

	    @Override
		protected int[] getTypeIds() {
	        return new int[] { TYPEID };
	    }

	    @Override
		protected String[] getTypeNames() {
	        return new String[] { TYPE_NAME };
	    }

	    @Override
		public void javaToNative(Object object, TransferData transferData) {
	        byte[] check = TYPE_NAME.getBytes();
	        super.javaToNative(check, transferData);
	    }

	    @Override
		public Object nativeToJava(TransferData transferData) {
	        Object result = super.nativeToJava(transferData);
	        if (isInvalidNativeType(result)) {
	        	Policy.getLog().log(new Status(
	                            IStatus.ERROR,
	                            Policy.JFACE,
	                            IStatus.ERROR,
	                            JFaceResources.getString("LocalSelectionTransfer.errorMessage"), null)); //$NON-NLS-1$
	        }
	        return selection;
	    }

	    public void setObject(Object3d s) {
	        selection = s;
	    }


}
