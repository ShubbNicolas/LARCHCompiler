package exceptions;

import java.io.IOException;

@SuppressWarnings("serial")
public class UnavilableFileException extends IOException{

	public UnavilableFileException() {
		super("��ȡ�����ļ������ã�");
		
	}

	public UnavilableFileException(String message) {
		super(message);
		
	}

	public UnavilableFileException(Throwable cause) {
		super(cause);
		
	}
	

}
