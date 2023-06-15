package cn.wjdiankong.main;

import java.io.FileInputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import cn.wjdiankong.chunk.AttributeData;
import cn.wjdiankong.chunk.EndTagChunk;
import cn.wjdiankong.chunk.StartTagChunk;
import cn.wjdiankong.chunk.StringChunk;
import cn.wjdiankong.chunk.TagChunk;

public class XmlEditor {

	private String fileName;
	public XmlEditor(String fileName) {
		this.fileName = fileName;
	}

	public int tagStartChunkOffset = 0, tagEndChunkOffset;
	public int subAppTagChunkOffset = 0;
	public int subTagChunkOffsets = 0;
	
	public static String[] isNotAppTag = new String[]{
			"uses-permission", "uses-sdk", "compatible-screens", "instrumentation", "library",
			"original-package", "package-verifier", "permission", "permission-group", "permission-tree",
			"protected-broadcast", "resource-overlay", "supports-input", "supports-screens", "upgrade-key-set",
			"uses-configuration", "uses-feature"};
	
	public static String prefixStr = "http://schemas.android.com/apk/res/android";
	
	/**
	 * ɾ����ǩ����
	 * @param tagName
	 * @param name
	 */
	public void removeTag(String tagName, String name){
		try{
			ParserChunkUtilsHelper.get(fileName).parserXml();
			for(TagChunk tag : ParserChunkUtilsHelper.get(fileName).xmlStruct.tagChunkList){
				int tagNameIndex = Utils.byte2int(tag.startTagChunk.name);
				String tagNameTmp = ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.get(tagNameIndex);
				if(tagName.equals(tagNameTmp)){
					for(AttributeData attrData : tag.startTagChunk.attrList){
						String attrName = ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.get(attrData.name);
						if("name".equals(attrName)){
							String value = ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.get(attrData.valueString);
							if(name.equals(value)){
								//�ҵ�ָ����tag��ʼɾ��
								int size = Utils.byte2int(tag.endTagChunk.size);
								int delStart = tag.startTagChunk.offset;
								int delSize = (tag.endTagChunk.offset - tag.startTagChunk.offset) + size;
								ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.removeByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, delStart, delSize);

								modifyFileSize();
								return;
							}
						}
					}
				}
			}
		}catch (IOException e){
			System.out.println("parse xml err:"+e.toString());
		}
	}
	
	/**
	 * ��ӱ�ǩ���� 
	 */
	public void addTag(String insertXml){
		FileInputStream insertXmlFile = null;
		try {
			ParserChunkUtilsHelper.get(fileName).parserXml();
	        XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
	        XmlPullParser pullParser = pullParserFactory.newPullParser();
			insertXmlFile = new FileInputStream(insertXml);
	        pullParser.setInput(insertXmlFile, "UTF-8");
	        int event = pullParser.getEventType();  
	        // ��Ϊ������ĩβ  
	        while (event != XmlPullParser.END_DOCUMENT){ // �ĵ����� 
	            // �ڵ�����  
	            switch (event) {  
	            
	                case XmlPullParser.START_DOCUMENT: // �ĵ���ʼ  
	                    break;  
	                    
	                case XmlPullParser.START_TAG: // ��ǩ��ʼ 
	                	String tagName = pullParser.getName();
	                	int name = getStrIndex(tagName);
	                	int attCount = pullParser.getAttributeCount();
	                	byte[] attribute = new byte[20*attCount];
	                	for(int i=0;i<pullParser.getAttributeCount();i++){
	                		int attruri = getStrIndex(prefixStr);
	                		//������Ҫ��������������
	                		String attrName = pullParser.getAttributeName(i);
	                		String[] strAry = attrName.split(":");
	                		int[] type = getAttrType(pullParser.getAttributeValue(i));
	                		int attrname = getStrIndex(strAry[1]);
	                		int attrvalue = getStrIndex(pullParser.getAttributeValue(i));
	                		int attrtype = type[0];
	                		int attrdata = type[1];
	                		AttributeData data = AttributeData.createAttribute(attruri, attrname, attrvalue, attrtype, attrdata);
	                		attribute = Utils.byteConcat(attribute, data.getByte(), data.getLen()*i);
	                	}
	                	
	                	StartTagChunk startChunk = StartTagChunk.createChunk(name, attCount, -1, attribute);
	                	//����һ���µ�chunk֮�󣬿�ʼд��
	                	if(isNotAppTag(tagName)){
	            			ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, subTagChunkOffsets, startChunk.getChunkByte());
	            			subTagChunkOffsets += startChunk.getChunkByte().length;
	                	}else{
	                		ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, subAppTagChunkOffset, startChunk.getChunkByte());
	            			subAppTagChunkOffset += startChunk.getChunkByte().length;
	                	}
	                    break;  
	                    
	                case XmlPullParser.END_TAG: // ��ǩ����  
	                	tagName = pullParser.getName();
	                	name = getStrIndex(tagName);
	                	EndTagChunk endChunk = EndTagChunk.createChunk(name);
	                	if(isNotAppTag(tagName)){
	            			ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, subTagChunkOffsets, endChunk.getChunkByte());
	            			subTagChunkOffsets += endChunk.getChunkByte().length;
	                	}else{
	                		ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, subAppTagChunkOffset, endChunk.getChunkByte());
	            			subAppTagChunkOffset += endChunk.getChunkByte().length;
	                	}
	                    break;  
	                    
	            }
	            event = pullParser.next(); // ��һ����ǩ  
	        }
		} catch (XmlPullParserException e) {
			System.out.println("parse xml err:"+e.toString());
		} catch (IOException e){
			System.out.println("parse xml err:"+e.toString());
		} finally {
			if (insertXmlFile != null) {
				try {
					insertXmlFile.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		modifStringChunk();
		
		modifyFileSize();

	}
	
	/**
	 * ɾ������
	 * @param tag
	 * @param tagName
	 * @param attrName
	 */
	public void removeAttr(String tag, String tagName, String attrName){
		try{
			ParserChunkUtilsHelper.get(fileName).parserXml();
			for(StartTagChunk chunk : ParserChunkUtilsHelper.get(fileName).xmlStruct.startTagChunkList){
				int tagNameIndex = Utils.byte2int(chunk.name);
				String tagNameTmp = ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.get(tagNameIndex);

				if(tag.equals(tagNameTmp)){

					//�����application��manifest��ǩֱ�Ӵ���ͺ�
					if(tag.equals("application") || tag.equals("manifest")){
						for(AttributeData data : chunk.attrList){
							String attrNameTemp1 = ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.get(data.name);
							if(attrName.equals(attrNameTemp1)){
								//����ҵ���Ӧ�ı�ǩ������ֻ��һ������ֵ������ɾ���ɹ���ͬʱ���ð������ǩ��ɾ����
								if(chunk.attrList.size() == 1){
									removeTag(tag, tagName);
									return ;
								}
								//�����޸Ķ�Ӧ��tag chunk�����Ը������ʹ�С
								int countStart = chunk.offset + 28;
								byte[] modifyByte = Utils.int2Byte(chunk.attrList.size()-1);
								ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, modifyByte, countStart);

								//�޸�chunk�Ĵ�С
								int chunkSizeStart = chunk.offset + 4;
								int chunkSize = Utils.byte2int(chunk.size);
								byte[] modifyByteSize = Utils.int2Byte(chunkSize-20);//һ�����Կ���20���ֽ�
								ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

								//ɾ����������
								int delStart = data.offset;
								int delSize = data.getLen();
								ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.removeByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, delStart, delSize);

								modifyFileSize();
								return;
							}
						}
					}

					//������Ҫͨ��name�ҵ�ָ����tag
					for(AttributeData attrData : chunk.attrList){
						String attrNameTemp = ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.get(attrData.name);
						if("name".equals(attrNameTemp)){//�����ҵ�tag��Ӧ��Ψһ����
							String value = ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.get(attrData.valueString);
							if(tagName.equals(value)){
								for(AttributeData data : chunk.attrList){
									String attrNameTemp1 = ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.get(data.name);
									if(attrName.equals(attrNameTemp1)){

										//����ҵ���Ӧ�ı�ǩ������ֻ��һ������ֵ������ɾ���ɹ���ͬʱ���ð������ǩ��ɾ����
										if(chunk.attrList.size() == 1){
											removeTag(tag, tagName);
											return ;
										}

										//�����޸Ķ�Ӧ��tag chunk�����Ը������ʹ�С
										int countStart = chunk.offset + 28;
										byte[] modifyByte = Utils.int2Byte(chunk.attrList.size()-1);
										ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, modifyByte, countStart);

										//�޸�chunk�Ĵ�С
										int chunkSizeStart = chunk.offset + 4;
										int chunkSize = Utils.byte2int(chunk.size);
										byte[] modifyByteSize = Utils.int2Byte(chunkSize-20);
										ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

										//ɾ����������
										int delStart = data.offset;
										int delSize = data.getLen();
										ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.removeByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, delStart, delSize);

										modifyFileSize();
										return ;

									}
								}
							}
						}
					}
				}
			}
		}catch (IOException e){
			System.out.println("parse xml err:"+e.toString());
		}
	}
	
	/**
	 * ��������ֵ
	 * @param tag
	 * @param tagName
	 * @param attrName
	 * @param attrValue
	 */
	public void modifyAttr(String tag, String tagName, String attrName, String attrValue){
		try{
			ParserChunkUtilsHelper.get(fileName).parserXml();
			XmlEditorHelper.get(fileName).removeAttr(tag, tagName, attrName);
			ParserChunkUtilsHelper.get(fileName).parserXml();
			XmlEditorHelper.get(fileName).addAttr(tag, tagName, attrName, attrValue);

		}catch (IOException e){
			System.out.println("parse xml err:"+e.toString());
		}
	}
	
	/**
	 * �������ֵ
	 * @param tag
	 * @param tagName
	 * @param attrName
	 * @param attrValue
	 */
	public void addAttr(String tag, String tagName, String attrName, String attrValue){
		try{
			ParserChunkUtilsHelper.get(fileName).parserXml();
			//����һ�����Գ���
			int[] type = getAttrType(attrValue);
			int attrname = getStrIndex(attrName);
			int attrvalue = getStrIndex(attrValue);
			int attruri = getStrIndex(prefixStr);;
			int attrtype = type[0];//��������
			int attrdata = type[1];//����ֵ����int����

			AttributeData data = AttributeData.createAttribute(attruri, attrname, attrvalue, attrtype, attrdata);

			for(StartTagChunk chunk : ParserChunkUtilsHelper.get(fileName).xmlStruct.startTagChunkList){

				int tagNameIndex = Utils.byte2int(chunk.name);
				String tagNameTmp = ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.get(tagNameIndex);
				if(tag.equals(tagNameTmp)){

					//�����application��manifest��ǩֱ�Ӵ���ͺ�
					if(tag.equals("application") || tag.equals("manifest")){
						//�����޸Ķ�Ӧ��tag chunk�����Ը������ʹ�С
						int countStart = chunk.offset + 28;
						byte[] modifyByte = Utils.int2Byte(chunk.attrList.size()+1);
						ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, modifyByte, countStart);

						//�޸�chunk�Ĵ�С
						int chunkSizeStart = chunk.offset + 4;
						int chunkSize = Utils.byte2int(chunk.size);
						byte[] modifyByteSize = Utils.int2Byte(chunkSize+20);
						ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

						//����������ݵ�ԭ����chunk��
						ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, chunk.offset + chunkSize, data.getByte());

						modifStringChunk();

						modifyFileSize();

						return;
					}

					for(AttributeData attrData : chunk.attrList){
						String attrNameTemp = ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.get(attrData.name);
						if("name".equals(attrNameTemp)){//�����ҵ�tag��Ӧ��Ψһ����

							//�����޸Ķ�Ӧ��tag chunk�����Ը������ʹ�С
							int countStart = chunk.offset + 28;
							byte[] modifyByte = Utils.int2Byte(chunk.attrList.size()+1);
							ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, modifyByte, countStart);

							//�޸�chunk�Ĵ�С
							int chunkSizeStart = chunk.offset + 4;
							int chunkSize = Utils.byte2int(chunk.size);
							byte[] modifyByteSize = Utils.int2Byte(chunkSize+20);
							ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

							//����������ݵ�ԭ����chunk��
							ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, chunk.offset + chunkSize, data.getByte());

							modifStringChunk();

							modifyFileSize();

							return;
						}
					}
				}
			}
		}catch (IOException e){
			System.out.println("parse xml err:"+e.toString());
		}
	}
	
	/**
	 * ���²���String Chunk���ݿ�
	 */
	private void modifStringChunk(){
		try{
			//д��StartTagChunk chunk֮ǰ����Ϊ���ַ�����Ϣ���ӣ����Ե��޸��ַ�������
			StringChunk strChunk = ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk;
			byte[] newStrChunkB = strChunk.getByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList);
			//ɾ��ԭʼString Chunk
			ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.removeByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, ParserChunkUtilsHelper.get(fileName).stringChunkOffset, Utils.byte2int(strChunk.size));
			//�����µ�String Chunk
			ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.insertByte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, ParserChunkUtilsHelper.get(fileName).stringChunkOffset, newStrChunkB);
		}catch (IOException e){
			System.out.println("parse xml err:"+e.toString());
		}
	}
	
	/**
	 * �޸��ļ����յĴ�С
	 */
	public void modifyFileSize(){
		byte[] newFileSize = Utils.int2Byte(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc.length);
		ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc = Utils.replaceBytes(ParserChunkUtilsHelper.get(fileName).xmlStruct.byteSrc, newFileSize, 4);
	}
	
	/**
	 * ��ȡ�ַ���������ֵ������ַ�������ֱ�ӷ��أ������ھͷŵ�ĩβ���ض�Ӧ������ֵ
	 * @param str
	 * @return
	 */
	public int getStrIndex(String str){
		if(str == null || str.length() == 0){
			return -1;
		}
		for(int i=0; i<ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.size(); i++){
			if(ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.get(i).equals(str)){
				return i;
			}
		}
		ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.add(str);
		return ParserChunkUtilsHelper.get(fileName).xmlStruct.stringChunk.stringContentList.size()-1;
	}
	
	/**
	 * �ж��Ƿ���application�ⲿ�ı�ǩ��application���ڲ����ⲿ��ǩ��Ҫ���ֶԴ�
	 * @param tagName
	 * @return
	 */
	public boolean isNotAppTag(String tagName){
		for(String str : isNotAppTag){
			if(str.equals(tagName)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * ��ȡ���Զ�Ӧ����ֵ
	 * @param tagValue
	 * @return
	 */
	public int[] getAttrType(String tagValue){
		
		int[] result = new int[2];
		
		if(tagValue.equals("true") || tagValue.equals("false")){//boolean
			result[0] = result[0] | AttributeType.ATTR_BOOLEAN;
			if(tagValue.equals("true")){
				result[1] = 1;
			}else{
				result[1] = 0;
			}
		}else if(tagValue.equals("singleTask") || tagValue.equals("standard") 
				|| tagValue.equals("singleTop") || tagValue.equals("singleInstance")){//����ģʽint����
			result[0] = result[0] | AttributeType.ATTR_FIRSTINT;
			if(tagValue.equals("standard")){
				result[1] = 0;
			}else if(tagValue.equals("singleTop")){
				result[1] = 1;
			}else if(tagValue.equals("singleTask")){
				result[1] = 2;
			}else{
				result[1] = 3;
			}
		}else if(tagValue.equals("minSdkVersion") || tagValue.equals("versionCode")){
			result[0] = result[0] | AttributeType.ATTR_FIRSTINT;
			result[1] = Integer.valueOf(tagValue);
		}else if(tagValue.startsWith("@")){//����
			result[0] = result[0] | AttributeType.ATTR_REFERENCE;
			result[1] = 0x7F000000;
		}else if(tagValue.startsWith("#")){//ɫֵ
			result[0] = result[0] | AttributeType.ATTR_ARGB4;
			result[1] = 0xFFFFFFFF;
		}else{//�ַ���
			result[0] = result[0] | AttributeType.ATTR_STRING;
			result[1] = getStrIndex(tagValue);
		}
		
		result[0] = result[0] | 0x08000000;
		result[0] = Utils.byte2int(Utils.reverseBytes(Utils.int2Byte(result[0])));//�ֽ���Ҫ��תһ��
		
		return result;
	}
	
}
