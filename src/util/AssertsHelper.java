package util;

public class AssertsHelper {
	
	public static String [] asserts = {"~(i¡Ênew)", 
			"i¡Êadd(t,i,v) == i=i1 i=e",
			"lookup"};
	
	public static void init() {
		for(int i = 0; i<asserts.length; i++) {
			asserts[i].trim();
		}
	}
	
	public static String[] readAssert() {
		String[] functioncase = new String[10];
		int addflag = 0;
		int lookupflag = 0;
	for(int i = 0; i<asserts.length; i++) {
		
		String[] assertsContent = asserts[i].split("==|=|V|,|:|;");
		for(int j=0; j<assertsContent.length; j++) {	
			System.out.println(j + " j: " + assertsContent[j]);
			if(assertsContent[j].indexOf("add")!= -1) {
				//System.out.println("addasserts: " + assertsContent[j].indexOf("add"));
				addflag++;
			}
			if(assertsContent[j].indexOf("new") != -1) {
				functioncase[1] = "new";
			}
			if(assertsContent[j].indexOf("lookup") != -1) {
				lookupflag++;
			}
			
			//ÅÐ¶Ïº¯ÊýÊÇ·ñ´æÔÚ
			if(addflag==1) {
				functioncase[6] = "add";
			}
			
			if(lookupflag==2) {
				functioncase[5] = "lookup";
			}
		}
	}
	
	
	for(int i=0;i<10;i++) {
		System.out.println(i + ": " + functioncase[i]);
	}
	return functioncase;
	}
}
