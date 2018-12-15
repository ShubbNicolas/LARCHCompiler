package test;


import compiler.OpenFile;
import exceptions.FileNotTxtException;
import util.ReaderHelper;

public class Test {
	public static void main(String[]args) throws FileNotTxtException{
		
        ReaderHelper rhelper=new ReaderHelper();
        //OpenFile�������Ҫ���Ǵ��ļ��Ի���ѡ���ļ��������ļ�·����
        OpenFile op=new OpenFile();
        //getPath�������Ƿ����ļ�·��
        String path=op.getPath();
        rhelper.setPath(path);
        rhelper.Assign();
        System.out.print("new: ");
        
        //�鿴newStack�����ֵ
        while(!rhelper.newStack.empty())
        {
       	 System.out.print(rhelper.newStack.peek()+" ");
       	 rhelper.newStack.pop();
        }
        System.out.println("");     
        
        //�鿴addStack�����ֵ
        System.out.print("add: ");
        while(!rhelper.addStack.empty())
        {
       	 System.out.print(rhelper.addStack.peek()+" ");
       	 rhelper.addStack.pop();
        } 
        System.out.println("");
        
        //�鿴belongStack�����ֵ
        System.out.print("/belong: ");
        while(!rhelper.belongStack.empty())
        {
       	 System.out.print(rhelper.belongStack.peek()+" ");
       	 rhelper.belongStack.pop();
        }
        System.out.println("");
        System.out.print("lookup: ");
        
        //�鿴lookupStack�����ֵ
        while(!rhelper.lookupStack.empty())
        {
       	 System.out.print(rhelper.lookupStack.peek()+" ");
       	 rhelper.lookupStack.pop();
        }
        System.out.println("");
        
        //�鿴asserts������
        for(int i=0;i<rhelper.assertContent.length;i++)
        {
       	 System.out.println(rhelper.assertContent[i]);
        }   
}
}
