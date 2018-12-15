package compiler;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import exceptions.FileNotTxtException;

public class OpenFile {
	public String getPath() throws FileNotTxtException
	{
		
		 JFileChooser fileChooser = new JFileChooser();
		  //设置当前目录
		  //文件对话框显示的是当前项目文件目录
		  fileChooser.setCurrentDirectory(new File("."));
		  fileChooser.setAcceptAllFileFilterUsed(false);
		  //设置文件对话框里面需要显示的文件
		  final String[][] fileENames = { 
				 
				  { ".larch", "larch(*.larch)" },
				  { ".txt", "文本文件(*.txt)" },
		           };
		  // 显示所有文件
		  fileChooser.addChoosableFileFilter(new FileFilter() {
		   public boolean accept(File file) {
			 String p= file.getPath();
			 System.out.println(p);
		    return true;
		   }
		   public String getDescription() {
		    return "所有文件(*.*)";
		   }
		  });
		  // 循环添加需要显示的文件
		  for (final String[] fileEName : fileENames) {
		   
		   fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
		 
				    public boolean accept(File file) { 
				 
				     if (file.getName().endsWith(fileEName[0]) || file.isDirectory()) {
				 
				      return true;
				     }
				 
				     return false;
				    }
		 
				    public String getDescription() {
				 
				     return fileEName[1];
				    }
		     });
		  } 
		  fileChooser.showDialog(null, null);
		  //通过filechooser获取文件的名字
		  File p=fileChooser.getSelectedFile();
		  //通过getAbsolutePath获取文件的绝对路径
		  String path = p.getAbsolutePath();
		  return path;
	}
}
