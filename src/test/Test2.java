package test;

import compiler.ExcuteProgram;
import compiler.OpenFile;
import util.AssertsHelper;
import util.ReaderHelper;

public class Test2 {
	public static void main(String[] args) {
	String results = null;
	ReaderHelper rhelper=new ReaderHelper();
    //OpenFile�������Ҫ���Ǵ��ļ��Ի���ѡ���ļ��������ļ�·����
    OpenFile op=new OpenFile();
    //getPath�������Ƿ����ļ�·��
    String path=op.getPath();
    rhelper.setPath(path);
    rhelper.Assign();
	
    /*for(int i=0;i<rhelper.assertContent.length;i++)
    {
    	System.out.println(rhelper.assertContent[i]);
    } */ 
    
	//assert����
    AssertsHelper ahelper = new AssertsHelper();
    AssertsHelper.asserts = rhelper.assertContent;	
	
	ExcuteProgram ep=new ExcuteProgram();
	System.out.println("==================" + AssertsHelper.readAssert());
	ep.Funname = AssertsHelper.readAssert();
	System.out.println("==================" + ep.Funname[5]);
	String path1="C:/Users/Janki/Desktop/Larch/Run.txt";
	ep.setPath(path1);
	
	ep.Run();
	System.out.print("Ind: ");
	for(int i=0;i<5;i++)
	{
		
	  System.out.print(ep.Ind[i]+" ");
	}
	System.out.println("");
	System.out.print("Val: ");
	for(int i=0;i<5;i++)
	{
	  System.out.print(ep.Val[i]+" ");
	}
	
	
}
}

