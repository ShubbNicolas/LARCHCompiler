package test;

import compiler.ExcuteProgram;

public class testRun {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ExcuteProgram ep=new ExcuteProgram();
		
		String path="C:/Users/Janki/Desktop/Larch/Run.txt";
		ep.setPath(path);
		
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
