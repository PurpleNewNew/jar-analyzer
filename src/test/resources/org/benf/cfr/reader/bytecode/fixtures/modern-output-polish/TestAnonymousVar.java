public class TestAnonymousVar {
  public void testNamelessTypeVirtual() {
    var printer = new Object() {
      void println(String s) {
        System.out.println(s);
      }
    };
    printer.println("goodbye, world!");
  }

  public void testNamelessTypeVirtual2() {
    var printer = new TestAnonymousVar() {
      void out(String s) {
        System.out.println(s);
      }
    };
    printer.out("goodbye, world!");
  }

  public void testNamelessTypeVirtual3() {
    TestAnonymousVar printer = new TestAnonymousVar() {
      @Override
      public void testNamelessTypeVirtual() {
        System.out.println();
      }
    };
    printer.testNamelessTypeVirtual();
  }
}
