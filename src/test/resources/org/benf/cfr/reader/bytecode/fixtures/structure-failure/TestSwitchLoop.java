public class TestSwitchLoop {
  public void test8(int i) {
    switch (i) {
      case 0:
        label: {
          for (int j = 0; j < 10; j++) {
            if (j == 3) {
              break label;
            }
          }

          System.out.println(0);
        }
        System.out.println("after");
      case 1:
        System.out.println(1);
    }

    System.out.println("after2");
  }
}
