package cn.wjdiankong.chunk;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import cn.wjdiankong.main.Utils;

public class StringChunk {

	public byte[] type;
	public byte[] size;
	public byte[] strCount;
	public byte[] styleCount;
	public byte[] unknown;
	public byte[] strPoolOffset;
	public byte[] stylePoolOffset;
	public byte[] strOffsets;
	public byte[] styleOffsets;
	public byte[] strPool;
	public byte[] stylePool;

	public ArrayList<String> stringContentList;
	
	public byte[] getByte(ArrayList<String> strList) throws UnsupportedEncodingException {
		
		byte[] strB = getStrListByte(strList);
		
		byte[] src = new byte[0];
		
		src = Utils.addByte(src, type);
		src = Utils.addByte(src, size);
		src = Utils.addByte(src, Utils.int2Byte(strList.size()));//�ַ�����
		src = Utils.addByte(src, styleCount);
		src = Utils.addByte(src, unknown);
		src = Utils.addByte(src, strPoolOffset);
		src = Utils.addByte(src, stylePoolOffset);
		
		byte[] strOffsets = new byte[0];
		ArrayList<byte[]> convertList = convertStrList(strList);
		
		int len = 0;
		for(int i=0;i<convertList.size();i++){
			strOffsets = Utils.addByte(strOffsets, Utils.int2Byte(len));
			len += (convertList.get(i).length+4);//�����4�����ַ���ͷ�����ַ�������2���ֽڣ����ַ�����β��2���ֽ�
		}
		
		src = Utils.addByte(src, strOffsets);//д��string offsetsֵ
		
		int newStyleOffsets = src.length;//д��strOffsets֮�����styleOffsets��ֵ
		
		src = Utils.addByte(src, styleOffsets);//д��style offsetsֵ
		
		int newStringPools = src.length;
		
		src = Utils.addByte(src, strB);//д��string pools
		
		src = Utils.addByte(src, stylePool); //д��style pools
		
		//��ΪstrOffsets��С�ĸı䣬�����styleOffsetsҲ��Ҫ�䶯
		if(styleOffsets != null && styleOffsets.length > 0){
			//ֻ��style��Ч����д��
			src = Utils.replaceBytes(src, Utils.int2Byte(newStyleOffsets), 28+strList.size()*4);
		}
		
		//��ΪstrOffsets��С�ı䣬�����strPoolOffsets��stylePoolOffsetҲҪ�䶯
		src = Utils.replaceBytes(src, Utils.int2Byte(newStringPools), 20);//�޸�strPoolOffsetsƫ��ֵ
		
		//����String Chunk�Ĵ�С������4�ı�����������ǲ��룬��ΪChunkһ����2�ı���������ֻ��Ҫ����2���ֽڼ���
		if(src.length %4 != 0){
			src = Utils.addByte(src, new byte[]{0,0});
		}
		
		//�޸�chunk���յĴ�С
		src = Utils.replaceBytes(src, Utils.int2Byte(src.length), 4);
		
		return src;
	}
	
	public int getLen(){
		return type.length+size.length+strCount.length+styleCount.length+
				unknown.length+strPoolOffset.length+stylePoolOffset.length+
				strOffsets.length+styleOffsets.length+strPool.length+stylePool.length;
	}
	
	public static StringChunk createChunk(byte[] byteSrc, int stringChunkOffset) throws UnsupportedEncodingException{

		StringChunk chunk = new StringChunk();

		//String Chunk�ı�ʾ
		chunk.type = Utils.copyByte(byteSrc, 0+stringChunkOffset, 4);

		//String Size
		chunk.size = Utils.copyByte(byteSrc, 4+stringChunkOffset, 4);
		int chunkSize = Utils.byte2int(chunk.size);

		//String Count
		chunk.strCount = Utils.copyByte(byteSrc, 8+stringChunkOffset, 4);
		int chunkStringCount = Utils.byte2int(chunk.strCount);

		chunk.stringContentList = new ArrayList<String>(chunkStringCount);

		//Style Count
		chunk.styleCount = Utils.copyByte(byteSrc, 12+stringChunkOffset, 4);
		int chunkStyleCount = Utils.byte2int(chunk.styleCount);

		//unknown
		chunk.unknown = Utils.copyByte(byteSrc, 16+stringChunkOffset, 4);

		//������Ҫע����ǣ�������ĸ��ֽ���Style�����ݣ�Ȼ������ŵ��ĸ��ֽ�ʼ����0������������Ҫֱ�ӹ�����8���ֽ�
		//String Offset �����String Chunk����ʼλ��0x00000008
		chunk.strPoolOffset = Utils.copyByte(byteSrc, 20+stringChunkOffset, 4);

		//Style Offset
		chunk.stylePoolOffset = Utils.copyByte(byteSrc, 24+stringChunkOffset, 4);

		//String Offsets
		chunk.strOffsets = Utils.copyByte(byteSrc, 28+stringChunkOffset, 4*chunkStringCount);

		//Style Offsets
		chunk.styleOffsets = Utils.copyByte(byteSrc, 28+stringChunkOffset+4*chunkStringCount, 4*chunkStyleCount);
		
		int stringContentStart = stringChunkOffset + Utils.byte2int(chunk.strPoolOffset);

		//String Content
		int contentLen =  chunkSize - Utils.byte2int(chunk.strPoolOffset);
		byte[] chunkStringContentByte = Utils.copyByte(byteSrc, stringContentStart, contentLen);

		/**
		 * �ڽ����ַ�����ʱ���и����⣬���Ǳ��룺UTF-8��UTF-16,�����UTF-8�Ļ�����00��β�ģ������UTF-16�Ļ���00 00��β��
		 */
		//����ĸ�ʽ�ǣ�ƫ��ֵ��ʼ�������ֽ����ַ����ĳ��ȣ��������ַ��������ݣ�������������ַ����Ľ�����00
		//���ַ������ŵ�ArrayList��
		int endStringIndex = 0;
		while(chunk.stringContentList.size() < chunkStringCount){
			//һ���ַ���Ӧ�����ֽڣ�����Ҫ����2
			int stringSize = Utils.byte2Short(Utils.copyByte(chunkStringContentByte, endStringIndex, 2))*2;
			byte[] temp = (stringSize > 0)? Utils.copyByte(chunkStringContentByte, endStringIndex+2, stringSize): new byte[0];
			String str = new String(temp, "UTF-16LE");
			chunk.stringContentList.add(str);
			endStringIndex += (2+stringSize+2);
		}
		
		chunk.strPool = Utils.copyByte(chunkStringContentByte, 0, endStringIndex);
		chunk.stylePool = Utils.copyByte(chunkStringContentByte, endStringIndex, contentLen - endStringIndex);
		return chunk;
	}
	
	private byte[] getStrListByte(ArrayList<String> strList) throws UnsupportedEncodingException{
		byte[] src = new byte[0];
		ArrayList<byte[]> stringContentListInBytes = convertStrList(strList);
		for(int i=0;i<stringContentListInBytes.size();i++){
			byte[] tempAry = new byte[0];
			short len = (short)(stringContentListInBytes.get(i).length/2);
			byte[] lenAry = Utils.shortToByte(len);
			tempAry = Utils.addByte(tempAry, lenAry);
			tempAry = Utils.addByte(tempAry, stringContentListInBytes.get(i));
			tempAry = Utils.addByte(tempAry, new byte[]{0,0});
			src = Utils.addByte(src, tempAry);
		}
		return src;
	}
	
	private ArrayList<byte[]> convertStrList(ArrayList<String> stringContentList) throws UnsupportedEncodingException{
		ArrayList<byte[]> destList = new ArrayList<>(stringContentList.size());
		for(String str : stringContentList){
			byte[] temp = str.getBytes("UTF-16LE");
			destList.add(temp);
		}
		return destList;
	}
	
}
