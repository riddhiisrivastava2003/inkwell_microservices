import java.nio.file.*;
public class ReadClassBytes {
  public static void main(String[] args) throws Exception {
    byte[] b = Files.readAllBytes(Paths.get(args[0]));
    String s = new String(b);
    System.out.println(s.contains("/newsletter/subscribers/") ? "FOUND" : "NOT_FOUND");
  }
}
