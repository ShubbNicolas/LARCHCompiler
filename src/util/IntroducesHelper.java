package util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IntroducesHelper {
	int Maxsize = 10;
	char[] first = new char[Maxsize];   //变量1
	char[] second = new char[Maxsize];  //变量2
	char[] third = new char[Maxsize]; 	//变量3
	int next = 0;
	Map<String,String> _prove = new HashMap<String, String>();
	
	IntroducesHelper() {
	}
	
	//单变量构造函数
	IntroducesHelper(char[] first) {
		this.first = first;
	}
	
	//双变量构造函数
	public IntroducesHelper(char[] first, char[] second) {
		this.first = first;
		this.second = second;
	}
	
	//三变量构造函数
	IntroducesHelper(char[] first, char[] second, char[] third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	//未涉及变量的函数使用  单算子单值的定理
	@SuppressWarnings("unlikely-arg-type")
	public Boolean Lclass_operate0_1(String operatorType, String element) {
		_prove.put("/smer", "~xx");
		_prove.put("/esmer", "xx");
		Iterator<String> it = _prove.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			System.out.println("key值：" + key + " value值：" + _prove.get(key));
		if(operatorType.equals(key) && element.equals(_prove.get(key))) {
			System.out.print("算式正确-------");
			return true;
		}
		if(operatorType.equals(key) && ("~" + element).equals(_prove.get(key))) {
			System.out.print("算式错误-------");
			return false;
		}
		}
		return false;
	}
	
	//未涉及变量的函数使用  多算子多值返回结果的定理
	public String Lclass_operate0_2(String operatorType, String element) {
		_prove.put("/smer", "~xx");
		return _prove.put("/esmer", "xx");
	}
	
	//涉及单变量的函数使用--查找
	public char Lclass_opreate1(char fir){
		int j;
		for(j = 0;j<10;j++) {
			System.out.println(first[j] + " " + fir);
			if(first[j] == fir) {
				return second[j];
			}
		}
		return fir;
	}
	
	//涉及双变量的函数使用--赋值
	/*public void Lclass_opreate2(char fir, char sec){
		int j;
		for(j = 0; j<this.next ;j++) {
			if(first[j] == fir) {
				System.out.println("sec: " + first[j] + " " + sec);
				second[j] = sec;
				break;
			}
		}
	}*/
	
	//涉及三变量的函数使用
	public boolean Lclass_opreate3(char[] first, char[] second, char[] third){
		return false;
	}

	public void Lclass_operate2(char fir, char sec) {
		// TODO Auto-generated method stub
		int j;
		for(j = 0; j<this.next ;j++) {
			if(first[j] == fir) {
				System.out.println("sec: " + first[j] + " " + sec);
				second[j] = sec;
				break;
			}
		}
	}

}
