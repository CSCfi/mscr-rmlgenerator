package fi.csc.mscr.tranformation.rml.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomFunctions {

	public static String addSuffix(Object value, String suffix) {
		return value + suffix;
	}

	public static String addPrefix(Object value, String prefix) {
		return prefix + value;
	}

	public static List<String> concat(List<String> values, String delimiter) {
		String result = String.join(delimiter, values);
		return List.of(result);
	}

	public static List<String> split(List<String> values, String delimiter) {
		List<String> result = Arrays.asList(values.get(0).toString().split(delimiter));
		return result;
	}

	public static List<Object> createArray(Object s) {
		List<Object> a = new ArrayList<Object>();
		a.add(s);
		return a;
	}

	public static List<String> addToArray(List<String> a, Object s) {
		a.add(s.toString());
		return a;
	}
}
