package org.stringtemplate.test;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.stringtemplate.Misc;
import org.stringtemplate.STGroup;
import org.stringtemplate.STGroupDir;
import org.stringtemplate.ST;

import java.io.File;

public class TestGroupDir extends BaseTest {
    @Test public void testSimpleGroup() throws Exception {
        String dir = getRandomDir();
        String a =
            "a(x) ::= <<"+newline+
            "foo"+newline+
            ">>"+newline;
        Misc.writeFile(dir, "a.st", a);
        STGroup group = new STGroupDir(dir);
        ST st = group.getInstanceOf("a");
        String expected = "foo"+newline;
        String result = st.render();
        assertEquals(expected, result);
    }

    @Test public void testGroupWithTwoTemplates() throws Exception {
        String dir = getRandomDir();
        String a =
            "a(x) ::= <<"+newline+
            "foo"+newline+
            ">>"+newline;
        Misc.writeFile(dir, "a.st", a);
        String b =
            "b() ::= \"bar\""+newline;
        Misc.writeFile(dir, "b.st", b);
        STGroup group = new STGroupDir(dir);
        ST st1 = group.getInstanceOf("a");
        ST st2 = group.getInstanceOf("b");
        String expected = "foo"+newline+"bar";
        String result = st1.render()+st2.render();
        assertEquals(expected, result);
    }

    @Test public void testSubdir() throws Exception {
        // /randomdir/a and /randomdir/subdir/b
        String dir = getRandomDir();
        String a =
            "a(x) ::= <<"+newline+
            "foo"+newline+
            ">>"+newline;
        Misc.writeFile(dir, "a.st", a);
        String b =
            "b() ::= \"bar\""+newline;
        Misc.writeFile(dir+"/subdir", "b.st", b);
        STGroup group = new STGroupDir(dir);
        ST st1 = group.getInstanceOf("a");
        ST st2 = group.getInstanceOf("subdir/b");
        String expected = "foo"+newline+"bar";
        String result = st1.render()+st2.render();
        assertEquals(expected, result);
    }

    @Test public void testGroupFileInDir() throws Exception {
        // /randomdir/a and /randomdir/group.stg with b and c templates
        String dir = getRandomDir();
        String a =
            "a(x) ::= <<\n"+
            "foo\n"+
            ">>\n";
        Misc.writeFile(dir, "a.st", a);
        String groupFile =
            "b() ::= \"bar\"\n"+
            "c() ::= \"duh\"\n";
        Misc.writeFile(dir, "group.stg", groupFile);
        STGroup group = new STGroupDir(dir);
        ST st1 = group.getInstanceOf("a");
        ST st2 = group.getInstanceOf("group/b");
        ST st3 = group.getInstanceOf("group/c");
        String expected = "foo"+newline+"barduh";
        String result = st1.render()+st2.render()+st3.render();
        assertEquals(expected, result);
    }

    @Test public void testSubSubdir() throws Exception {
        // /randomdir/a and /randomdir/subdir/b
        String dir = getRandomDir();
        String a =
            "a(x) ::= <<"+newline+
            "foo"+newline+
            ">>"+newline;
        Misc.writeFile(dir, "a.st", a);
        String b =
            "b() ::= \"bar\""+newline;
        Misc.writeFile(dir+"/sub1/sub2", "b.st", b);
        STGroup group = new STGroupDir(dir);
        ST st1 = group.getInstanceOf("a");
        ST st2 = group.getInstanceOf("sub1/sub2/b");
        String expected = "foo"+newline+"bar";
        String result = st1.render()+st2.render();
        assertEquals(expected, result);
    }

    @Test public void testGroupFileInSubDir() throws Exception {
        // /randomdir/a and /randomdir/group.stg with b and c templates
        String dir = getRandomDir();
        String a =
            "a(x) ::= <<\n"+
            "foo\n"+
            ">>\n";
        Misc.writeFile(dir, "a.st", a);
        String groupFile =
            "b() ::= \"bar\"\n"+
            "c() ::= \"duh\"\n";
        Misc.writeFile(dir, "subdir/group.stg", groupFile);
        STGroup group = new STGroupDir(dir);
        ST st1 = group.getInstanceOf("a");
        ST st2 = group.getInstanceOf("subdir/group/b");
        ST st3 = group.getInstanceOf("subdir/group/c");
        String expected = "foo"+newline+"barduh";
        String result = st1.render()+st2.render()+st3.render();
        assertEquals(expected, result);
    }

}