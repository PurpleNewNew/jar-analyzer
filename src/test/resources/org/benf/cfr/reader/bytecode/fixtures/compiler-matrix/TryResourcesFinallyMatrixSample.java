import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class TryResourcesFinallyMatrixSample {
  public void test(File file) throws FileNotFoundException {
    try (Scanner scanner = new Scanner(file)) {
      scanner.next();
    } finally {
      System.out.println("Hello");
    }
  }
}
