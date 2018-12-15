package test;


import compiler.OpenFile;
import exceptions.FileNotTxtException;
import util.ReaderHelper;

public class Test {
	public static void main(String[]args) throws FileNotTxtException{
		
        ReaderHelper rhelper=new ReaderHelper();
        //OpenFile这个类主要就是打开文件对话框，选择文件，返回文件路径。
        OpenFile op=new OpenFile();
        //getPath函数就是返回文件路径
        String path=op.getPath();
        rhelper.setPath(path);
        rhelper.Assign();
        System.out.print("new: ");
        
        //查看newStack里面的值
        while(!rhelper.newStack.empty())
        {
       	 System.out.print(rhelper.newStack.peek()+" ");
       	 rhelper.newStack.pop();
        }
        System.out.println("");     
        
        //查看addStack里面的值
        System.out.print("add: ");
        while(!rhelper.addStack.empty())
        {
       	 System.out.print(rhelper.addStack.peek()+" ");
       	 rhelper.addStack.pop();
        } 
        System.out.println("");
        
        //查看belongStack里面的值
        System.out.print("/belong: ");
        while(!rhelper.belongStack.empty())
        {
       	 System.out.print(rhelper.belongStack.peek()+" ");
       	 rhelper.belongStack.pop();
        }
        System.out.println("");
        System.out.print("lookup: ");
        
        //查看lookupStack里面的值
        while(!rhelper.lookupStack.empty())
        {
       	 System.out.print(rhelper.lookupStack.peek()+" ");
       	 rhelper.lookupStack.pop();
        }
        System.out.println("");
        
        //查看asserts的内容
        for(int i=0;i<rhelper.assertContent.length;i++)
        {
       	 System.out.println(rhelper.assertContent[i]);
        }   
}
}
