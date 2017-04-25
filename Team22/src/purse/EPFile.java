package purse;


import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;

public class EPFile {
	private KeyFile keyfile;
	
	//�ڲ�����Ԫ
	private byte[] EP_balance;         //����Ǯ�����
	private byte[] EP_offline;         //����Ǯ�������������
	private byte[] EP_online;          //����Ǯ���ѻ��������
	
	byte keyID;        //��Կ�汾��
	byte algID;        //�㷨��ʶ��
	
	//��ȫϵͳ���
	private Randgenerator RandData;          //���������
	private PenCipher EnCipher;              //���ݼӽ��ܷ�ʽʵ��
/**
 * ���������Ǽ���ʱ��Ҫ�õ�����ʱ��������	
 */
	//��ʱ��������
	//4���ֽڵ���ʱ��������
	private byte[] pTemp41;           
	private byte[] pTemp42;
	
	//8���ֽڵ���ʱ��������
	private byte[] pTemp81;
	private byte[] pTemp82;
	
	//32���ֽڵ���ʱ��������
	private byte[] pTemp16;
	private byte[] pTemp32;
	
	public EPFile(KeyFile keyfile){
		EP_balance = new byte[4];
		Util.arrayFillNonAtomic(EP_balance, (short)0, (short)4, (byte)0x00);
		
		EP_offline = new byte[2];
		Util.arrayFillNonAtomic(EP_offline, (short)0, (short)2, (byte)0x00);
		
		EP_online = new byte[2];
		Util.arrayFillNonAtomic(EP_online, (short)0, (short)2, (byte)0x00);
		
		this.keyfile = keyfile;
		
		pTemp41 = JCSystem.makeTransientByteArray((short)4, JCSystem.CLEAR_ON_DESELECT);
		pTemp42 = JCSystem.makeTransientByteArray((short)4, JCSystem.CLEAR_ON_DESELECT);
		
		pTemp81 = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_DESELECT);
		pTemp82 = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_DESELECT);
		
		pTemp16 = JCSystem.makeTransientByteArray((short)32, JCSystem.CLEAR_ON_DESELECT);
		pTemp32 = JCSystem.makeTransientByteArray((short)32, JCSystem.CLEAR_ON_DESELECT);
		
		RandData = new Randgenerator();
		EnCipher = new PenCipher();
	}
	
	/*
	 * ���ܣ�����Ǯ����������
	 * ������data �����ӵĽ��  
	 * 	   flag �Ƿ��������ӵ���Ǯ�����
	 * ���أ�Ȧ�������Ƿ񳬹�����޶�
	 */
	public final short increase(byte[] data, boolean flag){
		short i, t1, t2, ads;
		
		ads = (short)0;
		for(i = 3; i >= 0; i --){
			t1 = (short)(EP_balance[(short)i] & 0xFF); 	//ȡ��һ���ֽ�
			t2 = (short)(data[i] & 0xFF);				//ȡ��һ���ֽ�
			
			t1 = (short)(t1 + t2 + ads);				//��һ���ֽڽ��мӷ�����
			if(flag)									//��������£���abs���������ֱ����Ĵ�С
				EP_balance[(short)i] = (byte)(t1 % 256);//����
			ads = (short)(t1 / 256);					//��ȡ��λ
		}
		return ads;
	}
	
	/*
	 * ���ܣ�����Ǯ��������
	 * ������data ���ѵĽ� flag �Ƿ������ۼ�����Ǯ�����
	 * ���أ� �����Ƿ񳬶�
	 */
	public final short decrease(byte[] data, boolean flag){
		short i, t1, t2, dec;
		
		dec = (short)0;
		for(i = 3; i >= 0; i --){
			t1 = (short)(EP_balance[(short)i] & 0xFF); 	//ȡ��һ���ֽ�
			t2 = (short)(data[i] & 0xFF);				//ȡ��һ���ֽ�
			
			t1 = (short)(t1 - t2 - dec);				//��һ���ֽڽ��мӷ�����
			if(flag)
				EP_balance[(short)i] = (byte)((t1+256) % 256);//����
			dec = (short) (t1<0?1:0);					//��λ��ǣ��˴�����������������
		}
		return dec;
	}
	
	/*
	 * ���ܣ�Ȧ���ʼ���������
	 * ������num ��Կ��¼�ţ� data ������е����ݶ�
	 * ���أ�0�� Ȧ���ʼ������ִ�гɹ�     2��Ȧ�泬������Ǯ������޶�
	 */
	public final short init4load(short num, byte[] data){
		short length,rc;
		
		//pTemp42������Ž��׽��
		//pTemp81��������ն˻����
		Util.arrayCopyNonAtomic(data, (short)1, pTemp42, (short)0, (short)4);  //���׽��
		Util.arrayCopyNonAtomic(data, (short)5, pTemp81, (short)0, (short)6);  //�ն˻����
		
		//�ж��Ƿ񳬶�Ȧ��
		rc = increase(pTemp42, false);
		if(rc != (short)0)
			return (short)2;
		
		/*
		 * ��Կ��ȡ
		 * keyfile����ʾ��ȡ������Կ����pTemp32�У�����α�����ҵ���Ȧ����Կ�ĳ���
		 * pTemp32�ṹ��5���ֽڵ���Կͷ+16���ֽڵ���Կֵ
		 * ����ṹΪǰ3��byteδ֪����Կ�汾�� 1byte���㷨��ʶ  1byte�������ҵ���Ȧ����Կ16bytes
		 * keyID��Կ�汾��
		 * algID�㷨��ʶ��
		 * pTemp16�����ҵ���Ȧ����Կ
		 */
		length = keyfile.readkey(num, pTemp32);
		keyID = pTemp32[3];
		algID = pTemp32[4];
		Util.arrayCopyNonAtomic(pTemp32, (short)5, pTemp16, (short)0, length);
		
		/*
		 * ���������
		 * RandData.GenerateSecureRnd()�������ܣ�����һ��4bytes�������
		 * RandData.getRndValue(pTemp32, (short)0)���ܣ��������д��pTemp32[0:3]
		 */
		RandData.GenerateSecureRnd();
		RandData.getRndValue(pTemp32, (short)0);
		
		/*
		 * ����������Կ������������Ϊα�����4bytes||����Ǯ�������������2bytes||8000
		 * ��������ֽ���֮����������ȫ���Ѿ�д��pTemp32��
		 */
		Util.arrayCopyNonAtomic(EP_online, (short)0, pTemp32, (short)4, (short)2);
		pTemp32[6] = (byte)0x80;
		pTemp32[7] = (byte)0x00;
		
		/*
		 * IC����������������������ҵ�����Կ����������Կ��
		 * ������Կ�����ɷ�ʽ���ҽ���֮�����Կ�����н���˵����
		 * �������������Ϊα�����||����Ǯ�������������||8000
		 * ����ԿΪ�����ҵ���Ȧ����Կ
		 * gen_SESPK������key ��Կ�� data ��Ҫ���ܵ����ݣ� dOff �����ܵ�����ƫ������ dLen �����ܵ����ݳ��ȣ� r ���ܺ�����ݣ� rOff ���ܺ�����ݴ洢ƫ����
		 * ������Կ4bytes�Ѿ�����pTemp16�У���������8bytes�Ѿ�����pTemp32��
		 * ���ܺ�Ľ���ǹ�����Կ 8bytes������pTemp82��
		 */
		EnCipher.gen_SESPK(pTemp16, pTemp32, (short)0, (short)8, pTemp82, (short)0); 
		//ISOException.throwIt(pTemp82[0]);
		/*
		 * IC�����������ɵĹ�����Կ����MAC1��
		 * ��MAC1�����ɷ�ʽ����Ҳ����֮�����Կ�����н���˵����
		 * �������������Ϊ����Ǯ��������ǰ��||���׽��||�������ͱ�ʶ||�ն˻����
		 * ����ԿΪ������Կ��
		 * 
		 * EP_balance��4bytes������Ǯ�������pTemp32[0:3]
		 * data[1:4], ���׽��, ���pTemp32[4:7]
		 * ��������0x02(p2)��1byte��������pTemp32[8]
		 * data[5:10]���ն˻��ͱ�ţ�6byte�����pTemp32[9:14]
		 * 
		 */
		Util.arrayCopyNonAtomic(EP_balance, (short)0, pTemp32, (short)0, (short)4);   //����Ǯ�����
		Util.arrayCopyNonAtomic(data, (short)1, pTemp32, (short)4, (short)4);         //���׽��
		pTemp32[8] = (byte)0x02;                                                      //�������ͱ�ʶ
		Util.arrayCopyNonAtomic(data, (short)5, pTemp32, (short)9, (short)6);         //�ն˻����
		
		/*
		 * ����ʱ�����ڵ�����д��data
		 * ����������data 15bytes��ʹ�ù�����ԿpTemp82 8bytes����
		 * �е㵣��gmac4��ʱ�򣬱�����= =
		 * ���ܺ��macֵ4bytes�����pTemp41
		 */
		//������һ�ж����ˣ�����Դ�������д���������
		Util.arrayCopyNonAtomic(pTemp32, (short)0, data, (short)0x00, (short)0x0F);
		//gmac4������key ��Կ; data ��Ҫ���ܵ�����; dl ��Ҫ���ܵ����ݳ��ȣ� mac ������õ���MAC��TAC��
		EnCipher.gmac4(pTemp82, pTemp32, (short)0x0F, pTemp41);
		
		/*
		 * ��Ӧ���ݽṹ����
		 * ���4bytes | �����������к�2bytes | ��Կ�汾��1byte | �㷨��ʶ1byte | α�����4bytes | mac14bytes
		 * ����д��data
		 */
		Util.arrayCopyNonAtomic(EP_balance, (short)0, data, (short)0, (short)4);      //����Ǯ�����
		Util.arrayCopyNonAtomic(EP_online, (short)0, data,  (short)4, (short)2);      //����Ǯ�������������
		data[6] = keyID;                                                              //��Կ�汾��
		data[7] = algID;                                                              //�㷨��ʶ
		RandData.getRndValue(data, (short)8);                                         //�����
		Util.arrayCopyNonAtomic(pTemp41, (short)0, data, (short)12, (short)4);        //mac1
		
		return 0;
	}
	
	/*
	 * ���ܣ�Ȧ�湦�ܵ����
	 * ������data ������е����ݶ�
	 * ���أ�0 Ȧ������ִ�гɹ���1 MAC2У�����  2 Ȧ�泬������޶�; 3 ��Կδ�ҵ�
	 */
	public final short load(byte[] data){
		short rc;
		//ISOException.throwIt((short) 0x1234);
		/*
		 * IC���յ�Ȧ����������ù�����Կ����MAC2��
		 * ����������Ϊ���׽��||�������ͱ�ʶ||�ն˻����||�������ڣ�������||����ʱ�䣨������
		 * ��ԿΪ������Կ
		 * ��Ȧ������͵�MAC2���бȽϣ������ͬ����MAC2��Ч
		 * 
		 * ��init4load�У���pTemp42 4bytes������Ž��׽�����д��pTemp32[0:3]
		 * ��ʶ��p2Ϊ0x02 1byte������д��pTemp32[4]
		 * ��init4load�У�pTemp81 6bytes��������ն˻���ţ�����д��pTemp32[5:10]
		 * �ն���IC���͵�У��ָ���У�data[0:6]Ϊ[���� | ʱ��]������д��pTemp32[11:17]
		 * ��init4load�У����ܺ�Ľ���ǹ�����Կ 8bytes������pTemp82��
		 * ������pTemp32[0:17]ʹ�ù�����ԿpTemp82[0:7]ʹ��gmac4���ܣ����д��pTemp41
		 */
		Util.arrayCopyNonAtomic(pTemp42, (short)0, pTemp32, (short)0, (short)4);       //���׽��
		pTemp32[4] = (byte)0x02;                                                       //���ױ�ʶ
		Util.arrayCopyNonAtomic(pTemp81, (short)0, pTemp32, (short)5, (short)6);       //�ն˻����
		Util.arrayCopyNonAtomic(data, (short)0, pTemp32, (short)11, (short)7);         //����������ʱ��
		EnCipher.gmac4(pTemp82, pTemp32, (short)0x12, pTemp41);
		
		/*
		 * ����MAC2
		 * data[7:10]ΪMAC2�������IC���Լ����������mac2����У��
		 * ���һ�£�IC�����ܹ�ȷ���ն˻���������
		 */
		if(Util.arrayCompare(data, (short)7, pTemp41, (short)0, (short)4) != (byte)0x00)
			return (short)1;
		
		//����Ǯ����Ŀ����
		rc = increase(pTemp42, true);
		if(rc != (short)0)
			return 2;
		
		/*
		 * IC������TAC�롣
		 * TAC������ɷ�ʽ��MAC������ɷ�ʽһ�¡�
		 * ���룺����Ǯ�������׺�||����Ǯ������������ţ���1ǰ��||���׽��||�������ͱ�ʶ||�ն˻����||�������ڣ�������||����ʱ�䣨��������
		 * ��ԿΪTAC��������8���ֽ���TAC��������8���ֽ����Ľ����
		 */
		Util.arrayCopyNonAtomic(EP_balance, (short)0, pTemp32, (short)0, (short)4);    //����Ǯ�����
		Util.arrayCopyNonAtomic(EP_online, (short)0, pTemp32, (short)4, (short)2);     //����Ǯ�������������
		Util.arrayCopyNonAtomic(pTemp42, (short)0, pTemp32, (short)6, (short)4);       //���׽��
		pTemp32[10] = (byte)0x02;                                                      //��������
		Util.arrayCopyNonAtomic(pTemp81, (short)0, pTemp32, (short)11, (short)6);      //�ն˻����
		Util.arrayCopyNonAtomic(data, (short)0, pTemp32, (short)17, (short)7);         //����������ʱ��
		
		//����������ż�1
		rc = Util.makeShort(EP_online[0], EP_online[1]);
		rc ++;
		if(rc > (short)256)
			rc = (short)1;
		Util.setShort(EP_online, (short)0, rc);
		
		/*
		 * TAC�ļ���
		 * ��ȡ��Կ16bytes�����pTemp16
		 * keyfile����ʾ��ȡ������Կ����pTemp16�У�����α�����ҵ���Ȧ����Կ�ĳ���
		 * pTemp16�ṹ��5���ֽڵ���Կͷ+16���ֽڵ���Կֵ
		 * ����ṹΪǰ3��byteδ֪����Կ�汾�� 1byte���㷨��ʶ  1byte�������ҵ���Ȧ����Կ16bytes
		 */
		short length, num;
		num = keyfile.findKeyByType((byte)0x34);//Ѱ��0x34����Ϊ��ӦTAC��Կ
		length = keyfile.readkey(num, pTemp16);
		
		if(length == 0)
			return (short)3;
		
		//Util.arrayCopyNonAtomic(pTemp16, (short)5, data, (short)0, (short)8);
		
		//ȡ��Կ��ǰ8λ��д��pTemp82[0:7]�����ǵ�������Կ
		Util.arrayCopyNonAtomic(pTemp16, (short)5, pTemp82, (short)0, (short)8);
		//key�ٸ�����飬����tac��д��data
		//byte[] temp = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_DESELECT);
		EnCipher.xorblock8(pTemp82, pTemp16, (short)13);
		EnCipher.gmac4(pTemp82, pTemp32, (short)0x18, data);
		
		
		return (short)0;
	}
		
	/*
	 * ���ܣ����ѳ�ʼ����������
	 * ������num ��Կ��¼�ţ� data ������е����ݶ�
	 * ���أ�0 ����ִ�гɹ���2 ���ѳ���
	 */
	public final short init4purchase(short num, byte[] data){
		short length,rc;
		
		//pTemp42������Ž��׽��
		//pTemp81��������ն˻����
		Util.arrayCopyNonAtomic(data, (short)1, pTemp42, (short)0, (short)4);  //���׽��
		Util.arrayCopyNonAtomic(data, (short)5, pTemp81, (short)0, (short)6);  //�ն˻����
		
		/*
		 * �ж��Ƿ񳬶�Ȧ��
		 * IC��������Ǯ������Ƿ���ڻ���ڽ��׽��
		 * ���С�ڽ��׽������״̬��2����ʾ�ʽ��㡣
		 */
		rc = decrease(pTemp42, false);
		if(rc != (short)0)
			return (short)2;
		
		/*
		 * ��Կ��ȡ
		 * 
		 * keyfile����ʾ��ȡ������Կ����pTemp32�У�����α�����ҵ���Ȧ����Կ�ĳ���
		 * pTemp32�ṹ��5���ֽڵ���Կͷ+16���ֽڵ���Կֵ
		 * ����ṹΪǰ3��byteδ֪����Կ�汾�� 1byte���㷨��ʶ  1byte�������ҵ���Ȧ����Կ16bytes
		 * keyID��Կ�汾��
		 * algID�㷨��ʶ��
		 * pTemp16�����ҵ���Ȧ����Կ
		 */
		length = keyfile.readkey(num, pTemp32);
		keyID = pTemp32[3];
		algID = pTemp32[4];
		Util.arrayCopyNonAtomic(pTemp32, (short)5, pTemp16, (short)0, length);
		
		/*
		 * ���������
		 * RandData.GenerateSecureRnd()�������ܣ�����һ��4bytes�������
		 * RandData.getRndValue(pTemp32, (short)0)���ܣ��������д��pTemp32[0:3]
		 */
		RandData.GenerateSecureRnd();
		RandData.getRndValue(pTemp32, (short)0);
		
		/*
		 * IC������������󣬽������´���
		 * �ڽ�����Щ������IC����������Ӧ������
		 * ���4bytes | �ѻ��������2bytes | ͸֧�޶�3bytes | ��Կ�汾 1byte | �㷨��ʶ1byte | α�����4bytes
		 */
		byte[] overdraft = {0x00, 0x00, 0x00};
		Util.arrayCopyNonAtomic(EP_balance, (short)0, data, (short)0, (short)4);
		Util.arrayCopyNonAtomic(EP_offline, (short)0, data, (short)4, (short)2);
		Util.arrayCopyNonAtomic(overdraft, (short)0, data, (short)6, (short)3);
		data[9] = keyID;
		data[10] = algID;
		Util.arrayCopyNonAtomic(pTemp32, (short)0, data, (short)11, (short)4);
		
		return 0;
		
	}
	/*
	 * ���ܣ����������ʵ��
	 * ������data ������е����ݶ�
	 * ���أ�0 ����ִ�гɹ��� 1 MACУ����� 2 ���ѳ�� 3 ��Կδ�ҵ�
	 */
	public final short purchase(byte[] data){
		short rc;
		/*
		 * IC���յ�Ȧ��������������ҵ�����Կ����������Կ
		 * ����������Ϊα�����||����Ǯ���ѻ��������||�ն˽�����ŵ����������ֽ�
		 * ��ԿΪ�����ҵ���������Կ
		 * 
		 * ��init4load�У���pTemp32 4bytes��������漴����������д��pTemp32[0:3]
		 * ����Ǯ���ѻ��������2bytes��EP_offline�У�����д��pTemp32[4:5]
		 * �ն˽��������data�У�ȡ������
		 * 
		 * ������pTemp32[0:7]ʹ��������ԿpTemp16[0:4]�õ�������Կ��д��pTemp82
		 */

		//Util.arrayCopyNonAtomic(pTemp32, (short)0, pTemp32, (short)0, (short)4);
		Util.arrayCopyNonAtomic(EP_offline, (short)0, pTemp32, (short)4, (short)2);
		Util.arrayCopyNonAtomic(data, (short)2, pTemp32, (short)6, (short)2);

		//���ɹ�����Կ
		EnCipher.gen_SESPK(pTemp16, pTemp32, (short)0, (short)8, pTemp82, (short)0); 

		/*
		 * IC�����ù�����Կ����MAC1��
		 * �������ݣ����׽��||�������ͱ�ʶ(0x06)||�ն˻����||�������ڣ�������||����ʱ�䣨������
		 * ����������͵�MAC1���бȽϣ������ͬ����MAC1��Ч,�����һ�¾ͷ���0x01
		 */
		Util.arrayCopyNonAtomic(pTemp42, (short)0, pTemp32, (short)0, (short)4);
		pTemp32[4] = (byte)0x06;
		Util.arrayCopyNonAtomic(pTemp81, (short)0, pTemp32, (short)5, (short)6);
		Util.arrayCopyNonAtomic(data, (short)4, pTemp32, (short)11, (short)7);
		EnCipher.gmac4(pTemp82, pTemp32, (short)0x12, pTemp41);
		
		if(Util.arrayCompare(data, (short)11, pTemp41, (short)0, (short)4) != (byte)0x00)  
            return (short)0x01;    //����ͬ�򷵻�1  

		 // IC��������Ǯ���ѻ�������ż�1
		rc = Util.makeShort(EP_offline[0], EP_offline[1]);
		rc ++;
		if(rc > (short)256)
			rc = (short)1;
		Util.setShort(EP_offline, (short)0, rc);
		
		//���Ұѵ���Ǯ��������ȥ���׽��
		rc = decrease(pTemp42, true);
		if(rc != (short)0)
			return 2;

		/*
		 * �ڽ�������������IC�����ù�����Կ����MAC2��
		 * ����������Ϊ���׽�����Ϊ������Կ��
		 */
		//byte[] temp = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_DESELECT);  
        //Util.arrayCopyNonAtomic(pTemp16, (short)13, temp, (short)0, (short)8);//pTtem16[13]��ʼ������Կ  
		//pTemp32 = JCSystem.makeTransientByteArray((short)32, JCSystem.CLEAR_ON_DESELECT);  
		Util.arrayCopyNonAtomic(pTemp42, (short)0, pTemp32, (short)0, (short)4);
		EnCipher.gmac4(pTemp82, pTemp32, (short)0x4, pTemp41); //pTemp32����չ
		
		Util.arrayCopyNonAtomic(pTemp41, (short)0, data, (short)4, (short)4); //����mac2

		/*
		 * 	IC������TAC�롣
		 * TAC������ɷ�ʽ��MAC������ɷ�ʽһ�¡�
		 * ���룺���׽��||�������ͱ�ʶ||�ն˻����||�ն˽������||�������ڣ�������||����ʱ�䣨��������
		 * ��ԿΪTAC��������8���ֽ���TAC��������8���ֽ����Ľ����
		 */
		//byte[] pTemp32 = JCSystem.makeTransientByteArray((short)32, JCSystem.CLEAR_ON_DESELECT);  
		Util.arrayCopyNonAtomic(pTemp42, (short)0, pTemp32, (short)0, (short)4);
		pTemp32[4] = 0x06;
		Util.arrayCopyNonAtomic(pTemp81, (short)0, pTemp32, (short)5, (short)6);
		Util.arrayCopyNonAtomic(data, (short)0, pTemp32, (short)11, (short)4);
		Util.arrayCopyNonAtomic(data, (short)4, pTemp32, (short)15, (short)7);

		short length, num;
		num = keyfile.findKeyByType((byte)0x34);//Ϊʲôֻ��0x34����������
		length = keyfile.readkey(num, pTemp16);
		
		if(length == 0)
			return (short)3;
		
		//ȡ��Կ��ǰ8λ��д��pTemp82[0:7]�����ǵ�������Կ
		Util.arrayCopyNonAtomic(pTemp16, (short)5, pTemp82, (short)0, (short)8);//ȥ��ǰ��λ��Կͷ��
		//key�ٸ�����飬����tac��д��data
		EnCipher.xorblock8(pTemp82, pTemp16, (short)13);//��Կ��8λ����8λ���õ�����Կ
		

        //�õ�tacͬʱ����tac���ն�  
        byte[] temp = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_DESELECT);  
        Util.arrayCopyNonAtomic(pTemp16, (short)13, temp, (short)0, (short)8);//pTtem16[13]��ʼ������Կ
		
		//EnCipher.gmac4(pTemp82, pTemp32, (short)0x22, pTemp41);//�õ�tac������ط�������Խ��
        EnCipher.gmac4(temp, pTemp32, (short)22, data);//�õ�tacֱ�Ӹ��Ƹ�data������
		//Util.arrayCopyNonAtomic(pTemp41, (short)0, data, (short)0, (short)4);
		//ISOException.throwIt((short) 0x1234);
		return 0;
	}
	/*
	 * ���ܣ�����Ǯ������ȡ
	 * ������data ����Ǯ�����Ļ�����
	 * ���أ� 0
	 */
	public final short get_balance(byte[] data){
		Util.arrayCopyNonAtomic(EP_balance, (short)0, data, (short)0, (short)4);
		return 0;
	}
}