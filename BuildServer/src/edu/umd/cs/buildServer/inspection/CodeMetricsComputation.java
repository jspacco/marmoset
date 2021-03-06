package edu.umd.cs.buildServer.inspection;

import java.io.File;
import java.util.List;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.DescendingVisitor;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Visitor;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

public class CodeMetricsComputation {

	static class Tracker {
		private int size;

		/**
		 * @return Returns the size.
		 */
		public int getSize() {
			return size;
		}

		/**
		 * @param size
		 *            The size to set.
		 */
		public void setSize(int size) {
			this.size = size;
		}

		public void addSize(int size) {
			this.size += size;
		}
	}

	public static int computeCodeSegmentSize(File dir, List<File> list,
			String classpath) throws ClassNotFoundException {
		Repository.clearCache();
	    SyntheticRepository repos = SyntheticRepository.getInstance(new ClassPath(classpath));
	    Repository.setRepository(repos);
	    int size=0;
	    for (File file : list) {
	        String classname=file.getAbsolutePath().replace(dir.getAbsolutePath()+"/","");
	        classname = classname.replace(".class","");
	        classname = classname.replace('/','.');
	        //if (classname.contains("package-info")) continue;
	        size += CodeMetricsComputation.sizeOfCodeSegment(classname);
	    }
		return size;
	}

	/**
	 * @param classname
	 * @return
	 * @throws ClassNotFoundException
	 */
	static int sizeOfCodeSegment(String classname)
	throws ClassNotFoundException
	{
	    JavaClass javaClass = Repository.lookupClass(classname);
	    final CodeMetricsComputation.Tracker tracker=new CodeMetricsComputation.Tracker();
	    Visitor v = new EmptyVisitor() {


	        @Override
			public void visitCode(Code code)
	        {
	            super.visitCode(code);
	            tracker.addSize(code.getLength());
	        }
	    };
	    javaClass.accept(new DescendingVisitor(javaClass,v));
	    return tracker.getSize();
	}

}
