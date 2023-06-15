package cn.wjdiankong.main;

import java.util.ArrayList;
import java.util.List;

import cn.wjdiankong.chunk.EndNameSpaceChunk;
import cn.wjdiankong.chunk.EndTagChunk;
import cn.wjdiankong.chunk.ResourceChunk;
import cn.wjdiankong.chunk.StartNameSpaceChunk;
import cn.wjdiankong.chunk.StartTagChunk;
import cn.wjdiankong.chunk.StringChunk;
import cn.wjdiankong.chunk.TagChunk;
import cn.wjdiankong.chunk.TextChunk;
import cn.wjdiankong.chunk.XmlStruct;

public class ParserChunkUtils {

    private String fileName;

    public int stringChunkOffset = 8;
    public int resourceChunkOffset;
    public int nextChunkOffset;

    public XmlStruct xmlStruct = new XmlStruct();

    public boolean isApplication = false;
    public boolean isManifest = false;

    public List<TagChunk> tagChunkList = new ArrayList<TagChunk>();

    public ParserChunkUtils(String fileName) {
        this.fileName = fileName;
    }

    public void clear() {
        resourceChunkOffset = 0;
        nextChunkOffset = 0;
        isApplication = false;
        isManifest = false;
        tagChunkList.clear();
        xmlStruct.clear();
    }

    public void parserXml() throws java.io.UnsupportedEncodingException {
        clear();
        parserXmlHeader(xmlStruct.byteSrc);
        parserStringChunk(xmlStruct.byteSrc);
        parserResourceChunk(xmlStruct.byteSrc);
        parserXmlContent(xmlStruct.byteSrc);
    }

    /**
     * 解析xml的头部信息
     *
     * @param byteSrc
     */
    public void parserXmlHeader(byte[] byteSrc) {
        byte[] xmlMagic = Utils.copyByte(byteSrc, 0, 4);
        byte[] xmlSize = Utils.copyByte(byteSrc, 4, 4);
        xmlStruct.magicNumber = xmlMagic;
        xmlStruct.fileSize = xmlSize;
    }

    /**
     * 解析StringChunk
     *
     * @param byteSrc
     */
    public void parserStringChunk(byte[] byteSrc) throws java.io.UnsupportedEncodingException {
        xmlStruct.stringChunk = StringChunk.createChunk(byteSrc, stringChunkOffset);
        byte[] chunkSizeByte = Utils.copyByte(byteSrc, 12, 4);
        resourceChunkOffset = stringChunkOffset + Utils.byte2int(chunkSizeByte);
    }

    /**
     * 解析Resource Chunk
     *
     * @param byteSrc
     */
    public void parserResourceChunk(byte[] byteSrc) {
        xmlStruct.resChunk = ResourceChunk.createChunk(byteSrc, resourceChunkOffset);
        byte[] chunkSizeByte = Utils.copyByte(byteSrc, resourceChunkOffset + 4, 4);
        int chunkSize = Utils.byte2int(chunkSizeByte);
        nextChunkOffset = (resourceChunkOffset + chunkSize);
        XmlEditorHelper.get(fileName).tagStartChunkOffset = nextChunkOffset;
    }

    /**
     * 解析StartNamespace Chunk
     *
     * @param byteSrc
     */
    public void parserStartNamespaceChunk(byte[] byteSrc) {
        xmlStruct.startNamespaceChunk = StartNameSpaceChunk.createChunk(byteSrc);
    }

    /**
     * 解析EndNamespace Chunk
     *
     * @param byteSrc
     */
    public void parserEndNamespaceChunk(byte[] byteSrc) {
        xmlStruct.endNamespaceChunk = EndNameSpaceChunk.createChunk(byteSrc);
    }

    /**
     * 解析StartTag Chunk
     *
     * @param byteSrc
     */
    public void parserStartTagChunk(byte[] byteSrc, int offset) {

        StartTagChunk tagChunk = StartTagChunk.createChunk(byteSrc, offset);
        xmlStruct.startTagChunkList.add(tagChunk);
        TagChunk chunk = new TagChunk();
        chunk.startTagChunk = tagChunk;
        tagChunkList.add(chunk);

        //解析TagName
        byte[] tagNameByte = Utils.copyByte(byteSrc, 20, 4);
        int tagNameIndex = Utils.byte2int(tagNameByte);
        String tagName = xmlStruct.stringChunk.stringContentList.get(tagNameIndex);

        //标记是否为application标签
        if (tagName.equals("application")) {
            isApplication = true;
        }

    }

    /**
     * 解析EndTag Chunk
     *
     * @param byteSrc
     */
    public void parserEndTagChunk(byte[] byteSrc, int offset) {
        EndTagChunk tagChunk = EndTagChunk.createChunk(byteSrc, offset);
        TagChunk chunk = tagChunkList.remove(tagChunkList.size() - 1);
        chunk.endTagChunk = tagChunk;
        xmlStruct.endTagChunkList.add(tagChunk);
        xmlStruct.tagChunkList.add(chunk);//标签结束了，需要把标签放入池子中
    }

    /**
     * 解析Text Chunk
     *
     * @param byteSrc
     */
    public void parserTextChunk(byte[] byteSrc) {
        xmlStruct.textChunkList.add(TextChunk.createChunk(byteSrc));
    }

    /**
     * 开始解析xml的正文内容Chunk
     *
     * @param byteSrc
     */
    public void parserXmlContent(byte[] byteSrc) {
        while (!isEnd(byteSrc.length)) {
            byte[] chunkTagByte = Utils.copyByte(byteSrc, nextChunkOffset, 4);
            byte[] chunkSizeByte = Utils.copyByte(byteSrc, nextChunkOffset + 4, 4);
            int chunkTag = Utils.byte2int(chunkTagByte);
            int chunkSize = Utils.byte2int(chunkSizeByte);
            switch (chunkTag) {
                case ChunkTypeNumber.CHUNK_STARTNS:
                    parserStartNamespaceChunk(Utils.copyByte(byteSrc, nextChunkOffset, chunkSize));
                    isManifest = true;
                    break;
                case ChunkTypeNumber.CHUNK_STARTTAG:
                    parserStartTagChunk(Utils.copyByte(byteSrc, nextChunkOffset, chunkSize), nextChunkOffset);
                    //是否为application标签
                    if (isApplication) {
                        XmlEditorHelper.get(fileName).subAppTagChunkOffset = nextChunkOffset + chunkSize;
                        isApplication = false;
                    }
                    //是否为manifest标签
                    if (isManifest) {
                        XmlEditorHelper.get(fileName).subTagChunkOffsets = nextChunkOffset + chunkSize;
                        isManifest = false;
                    }
                    break;
                case ChunkTypeNumber.CHUNK_ENDTAG:
                    parserEndTagChunk(Utils.copyByte(byteSrc, nextChunkOffset, chunkSize), nextChunkOffset);
                    break;
                case ChunkTypeNumber.CHUNK_ENDNS:
                    parserEndNamespaceChunk(Utils.copyByte(byteSrc, nextChunkOffset, chunkSize));
                    break;
            }
            nextChunkOffset += chunkSize;
        }

    }

    /**
     * 判断是否到文件结束位置了
     *
     * @param totalLen
     * @return
     */
    public boolean isEnd(int totalLen) {
        return nextChunkOffset >= totalLen;
    }

}
