package util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IntroducesHelper {
	int Maxsize = 10;
	char[] first = new char[Maxsize];   //����1
	char[] second = new char[Maxsize];  //����2
	char[] third = new char[Maxsize]; 	//����3
	int next = 0;
	Map<String,String> _prove = new HashMap<String, String>();
	
	IntroducesHelper() {
	}
	
	//���������캯��
	IntroducesHelper(char[] first) {
		this.first = first;
	}
	
	//˫�������캯��
	public IntroducesHelper(char[] first, char[] second) {
		this.first = first;
		this.second = second;
	}
	
	//���������캯��
	IntroducesHelper(char[] first, char[] second, char[] third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	//δ�漰�����ĺ���ʹ��  �����ӵ�ֵ�Ķ���
	@SuppressWarnings("unlikely-arg-type")
	public Boolean Lclass_operate0_1(String operatorType, String element) {
		_prove.put("/smer", "~xx");
		_prove.put("/esmer", "xx");
		Iterator<String> it = _prove.keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			System.out.println("keyֵ��" + key + " valueֵ��" + _prove.get(key));
		if(operatorType.equals(key) && element.equals(_prove.get(key))) {
			System.out.print("��ʽ��ȷ-------");
			return true;
		}
		if(operatorType.equals(key) && ("~" + element).equals(_prove.get(key))) {
			System.out.print("��ʽ����-------");
			return false;
		}
		}
		return false;
	}
	
	//δ�漰�����ĺ���ʹ��  �����Ӷ�ֵ���ؽ���Ķ���
	public String Lclass_operate0_2(String operatorType, String element) {
		_prove.put("/smer", "~xx");
		return _prove.put("/esmer", "xx");
	}
	
	//�漰�������ĺ���ʹ��--����
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
	
	//�漰˫�����ĺ���ʹ��--��ֵ
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
	
	//�漰�������ĺ���ʹ��
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
