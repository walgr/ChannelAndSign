package cn.wjdiankong.main;

import java.util.HashMap;
import java.util.Map;

public class ParserChunkUtilsHelper {

	public static Map<String, ParserChunkUtils> ParserChunkUtilsMap = new HashMap<>();
	public static ParserChunkUtils get(String fileName) {
		if (ParserChunkUtilsMap.getOrDefault(fileName, null) == null) {
			put(fileName, new ParserChunkUtils(fileName));
		}
		return ParserChunkUtilsMap.getOrDefault(fileName, null);
	}

	public static void put(String fileName, ParserChunkUtils ParserChunkUtils) {
		ParserChunkUtilsMap.put(fileName, ParserChunkUtils);
	}

	public static void clearAll() {
		ParserChunkUtilsMap.clear();
	}
}
