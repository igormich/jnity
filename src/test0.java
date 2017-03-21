import java.util.stream.IntStream;

public class test0 {
	private static String whatDivider(int n) {
		StringBuilder result = new StringBuilder();
		if (n % 3 == 0)
			result.append("Fizz");
		if (n % 5 == 0)
			result.append("Buzz");
		if (result.length() == 0)
			result.append(n);
		return result.toString();
    }
	public static void main(String[] args) {
		
		IntStream.range(1, 101)
			.mapToObj(test0::whatDivider)
			.forEach(System.out::println);
	}
}
