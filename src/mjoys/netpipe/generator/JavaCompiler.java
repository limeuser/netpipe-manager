package mjoys.netpipe.generator;

import java.io.File;

import mjoys.io.FileFilter;
import mjoys.io.FileHandler;
import mjoys.io.FileUtil;
import mjoys.util.PathUtil;
import mjoys.util.SystemUtil;

public class JavaCompiler {
    public static class Context {
        public File root;
    }
    
    public static class JavaFilter implements FileFilter {
        public boolean accept(File f) {
            return f.isFile() && f.getName().endsWith(".java");
        }
    }
    
    public static class Compiler implements FileHandler {
		public void handle(File file, Object obj) {
			Context ctx = (Context) obj;
			String libs = PathUtil.combine(ctx.root.getAbsolutePath(), "libs");
			String classes = PathUtil.combine(ctx.root.getAbsolutePath(), "classes");
			String cmd = String.format("javac -Djava.ext.dirs=%s -d %s -classpath %s %s", libs, classes, classes, file.getAbsolutePath());
            SystemUtil.run(cmd);
		}
    }
    
    
    public static void compile(File root) {
        Context ctx = new Context();
        ctx.root = root;
        FileUtil.handle(new File(root, "src"), new JavaFilter(), new Compiler(), ctx);
    }
}
