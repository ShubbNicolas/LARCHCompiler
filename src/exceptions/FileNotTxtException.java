package exceptions;

@SuppressWarnings("serial")
public class FileNotTxtException extends Exception{

	public FileNotTxtException() {
		super("上传类型不是Larch文件！");
		
	}
	
	public FileNotTxtException(String message) {
		super(message);
		
	}

	public FileNotTxtException(Throwable cause) {
		super(cause);
		
	}
	

}
