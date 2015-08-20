package netpipe.generator;

import java.io.File;

import mjoys.util.FileUtil;
import mjoys.util.Logger;
import mjoys.util.SystemUtil;

public class JavaCompiler {
    private final static Logger logger = new Logger().addPrinter(System.out);
    
    public static class Context {
        public String classesDir;
    }
    
    public static class JavaFilter implements FileUtil.Filter {
        @Override
        public boolean filter(File f) {
            return f.isFile() && f.getName().endsWith(".java");
        }
    }
    
    public static class Compiler implements FileUtil.Handler<Context> {
        @Override
        public void handle(File file, Context ctx) {
            String cmd = "javac -Djava.ext.dirs=./libs -d " + ctx.classesDir + " " + file.getPath();
            logger.log(SystemUtil.run(cmd));
        }
    }
    
    
    public static void compile(File src, String classes) {
        Context ctx = new Context();
        ctx.classesDir = classes;
        FileUtil.handle(src, new JavaFilter(), new Compiler(), ctx);
    }
}
