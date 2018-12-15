package exceptions;

import java.io.IOException;

@SuppressWarnings("serial")
public class UnavilableFileException extends IOException{

	public UnavilableFileException() {
		super("读取错误：文件不可用！");
		
	}

	public UnavilableFileException(String message) {
		super(message);
		
	}

	public UnavilableFileException(Throwable cause) {
		super(cause);
		
	}
	

}
