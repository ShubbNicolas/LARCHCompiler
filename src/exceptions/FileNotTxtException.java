package exceptions;

@SuppressWarnings("serial")
public class FileNotTxtException extends Exception{

	public FileNotTxtException() {
		super("�ϴ����Ͳ���Larch�ļ���");
		
	}
	
	public FileNotTxtException(String message) {
		super(message);
		
	}

	public FileNotTxtException(Throwable cause) {
		super(cause);
		
	}
	

}
