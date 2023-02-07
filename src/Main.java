import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        String input = """
				"hello"
				"\t"
				"\\""
				"\""
				"\\"
				""";
        char[] inputChars = Arrays.copyOf(input.toCharArray(), input.length() + 1);
        for (char ch: inputChars) {
            System.out.print(ch + ": " + (int)ch + " | " );

        }
        System.out.println();
        System.out.println(input);
    }
}