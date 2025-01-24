package fi.csc.mscr.tranformation.rml.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomFunctions {
	public static Object passthrough(Object obj) {
		return obj;
	}

	public static String addSuffix(String value, String suffix) {
		return value + suffix;
	}

	public static String addPrefix(Object value, String prefix) {
		return prefix + value;
	}

	public static List<String> concat(List<String> values, String delimiter) {
		String result = String.join(delimiter, values);
		return List.of(result);
	}

	public static List<String> split(String value, String delimiter) {
		return Arrays.asList(value.split(delimiter));
	}

	public static Object arrayGet(List<String> values, int index) {
		String result = values.get(index);
		return result;
	}

	public static List<String> createArray(String s) {
		List<String> a = new ArrayList<String>();
		a.add(s);
		return a;
	}

	public static List<String> addToArray(List<String> a, String s) {
		a.add(s);
		return a;

	}
}
