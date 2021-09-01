import app.ConsoleApplication;

import java.io.File;

public class Compiler extends ConsoleApplication {
    Compiler(String[] args) {
        super(args);
    }

    void main() throws Exception {
        if (super.args[0].equals("clean")){
            super.clearFiles();
        }
        else if (super.args[0].equals("test")){
            super.prepare();
            super.testFiles();
        }
        else {
            super.prepare();
            super.calculate();
        }
    }

    public static void main(String[] args) throws Exception {
        Compiler compiler = new Compiler(args);
        compiler.main();
    }
}
