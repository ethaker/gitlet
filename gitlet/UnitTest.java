package gitlet;

import ucb.junit.textui;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Eesha Thaker
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(UnitTest.class));
    }

    /** A dummy test to avoid complaint. */
    @Test
    public void placeholderTest() {
    }

    /** Tests the helper method to merge, finding the split
     * point/common ancestor between two branch heads
     */
    @Test
    public void testFindSplitPoint() throws IOException {
        //creates a new file, hello, called Hello.txt in the CWD
        Repo r = new Repo();
        r.init();
        File g = new File(r.getCurrentWorkingDir().getPath()+"/"+"g.txt");
        r.add("g");
        File f = new File(r.getCurrentWorkingDir().getPath()+"/"+"f.txt");
        r.add("f");
        r.commit("Two files");
        r.branch("other");
        File h = new File(r.getCurrentWorkingDir().getPath()+"/"+"h.txt");
        r.add("h");
        r.rm("g");
        r.commit("Add h.txt and remove g.txt");
        r.checkout("other");
        r.rm("f");
        File k = new File(r.getCurrentWorkingDir().getPath()+"/"+"k.txt");
        r.add("k");
        r.commit("Add k.txt and remove f.txt");
        r.checkout("master");
        assertEquals("Two files", r.getSplitPoint("other").getName());
    }

    @Test
    public void testCommitandCheckout() throws IOException {
        Repo r = new Repo();
        File hello = new File(r.getCurrentWorkingDir().getPath()+"/"+"Hello.txt");
        //Adds Hello.txt to staging area, asserts that file exists there

        r.checkout("Hello.txt");
        String testLine = Utils.readContentsAsString(hello);
    }

    @Test
    public void testRM() {
        Repo r = new Repo();
        File hello = new File(r.getCurrentWorkingDir().getPath()+"/"+"Hello.txt");
        r.add("Hello.txt");
        r.rm("Hello.txt");
        File[] cwdFiles = r.getCurrentWorkingDir().listFiles();
    }


}


