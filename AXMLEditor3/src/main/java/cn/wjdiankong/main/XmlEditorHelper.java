package cn.wjdiankong.main;

import java.util.HashMap;
import java.util.Map;

public class XmlEditorHelper {

	public static Map<String, XmlEditor> xmlEditorHashMap = new HashMap<>();
	public static XmlEditor get(String fileName) {
		if (xmlEditorHashMap.getOrDefault(fileName, null) == null) {
			put(fileName, new XmlEditor(fileName));
		}
		return xmlEditorHashMap.getOrDefault(fileName, null);
	}

	public static void put(String fileName, XmlEditor xmlEditor) {
		xmlEditorHashMap.put(fileName, xmlEditor);
	}

	public static void clearAll() {
		xmlEditorHashMap.clear();
	}
}
