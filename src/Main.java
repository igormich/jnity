import java.lang.reflect.InvocationTargetException;

public class Main {
	
    public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
    	String value = WinRegistry.readString (
    		    WinRegistry.HKEY_LOCAL_MACHINE,                             //HKEY
    		   "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",           //Key
    		   "ProductName");                                              //ValueName
    		    System.out.println("Windows Distribution = " + value);
    }

}
