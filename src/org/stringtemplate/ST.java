/*
 [The "BSD licence"]
 Copyright (c) 2009 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.stringtemplate;

import org.antlr.runtime.RecognitionException;

import java.util.*;
import java.io.StringWriter;
import java.io.IOException;

public class ST {
    public static final String UNKNOWN_NAME = "unknown";
    public String name = UNKNOWN_NAME;
    
    /** The code to interpret; it pulls from attributes and this template's
     *  group of templates to evaluate to string.
     */
    public CompiledST code; // TODO: is this the right name?

    public static final ST BLANK = new BlankST();

    /** The group that was asked to create this instance.  This is
     *  fixed once we call toString() on an ST.  I wish we could leave
     *  this field out but people won't want to specify group to toString().
     */
    public STGroup group = STGroup.defaultGroup;
    
    /** Map an attribute name to its value(s). */
    Map<String,Object> attributes;

    /** Normally, formal parameters hide any attributes inherited from the
     *  enclosing template with the same name.  This is normally what you
     *  want, but makes it hard to invoke another template passing in all
     *  the data.  Use notation now: <otherTemplate(...)> to say "pass in
     *  all data".  Works great.  Can also say <otherTemplate(foo="xxx",...)>
     */
    protected boolean passThroughAttributes = false;    

    /** Enclosing instance if I'm embedded within another template.
     *  IF-subtemplates are considered embedded as well.
     */
    ST enclosingInstance; // who's your daddy?

    /** Just an alias for ArrayList, but this way I can track whether a
     *  list is something ST created or it's an incoming list.
     */
    public static final class AttributeList extends ArrayList {
        public AttributeList(int size) { super(size); }
        public AttributeList() { super(); }
    }
    
    public ST() {;}
    
    public ST(String template) {
        code = group.defineTemplate(UNKNOWN_NAME, template);
/*
        try {
            code = group.defineTemplate(UNKNOWN_NAME, template);
        }
        catch (STRecognitionException e) {
            int i = group.getCharPositionInLine(null, e);
	        group.listener.error(e.msg, null);
        }
         */
    }

    public ST(STGroup group, String template) {
        code = group.defineTemplate(UNKNOWN_NAME, template);
    }

    // TODO: who uses this?
    public ST(String template, char delimiterStartChar, char delimiterStopChar) {
        code = group.defineTemplate(UNKNOWN_NAME, template);
    }

    public void add(String name, Object value) {
        if ( name==null ) return; // allow null value
        if ( name.indexOf('.')>=0 ) {
            throw new IllegalArgumentException("cannot have '.' in attribute names");
        }

        if ( value instanceof ST ) ((ST)value).enclosingInstance = this;

        Object curvalue = null;
        if ( attributes==null || !attributes.containsKey(name) ) { // new attribute
            rawSetAttribute(name, value);
            return;
        }
        if ( attributes!=null ) curvalue = attributes.get(name);

        // attribute will be multi-valued for sure now
        // convert current attribute to list if not already
        // copy-on-write semantics; copy a list injected by user to add new value
        AttributeList multi = convertToAttributeList(curvalue);
        rawSetAttribute(name, multi); // replace with list

        // now, add incoming value to multi-valued attribute
        if ( value instanceof List ) {
            // flatten incoming list into existing list
            multi.addAll((List)value);
        }
        else if ( value!=null && value.getClass().isArray() ) {
            multi.addAll(Arrays.asList((Object[])value));
        }
        else {
            multi.add(value);
        }
    }

    protected void rawSetAttribute(String name, Object value) {
        if ( attributes==null ) attributes = new HashMap<String,Object>();
        attributes.put(name, value);
    }

    /** Find an attr with dynamic scoping up enclosing ST chain.
     *  If not found, look for a map.  So attributes sent in to a template
     *  override dictionary names.
     */
    public Object getAttribute(String name) {
        Object o = null;
        if ( attributes!=null ) o = attributes.get(name);
        if ( o!=null ) return o;
        
        if ( code.formalArguments!=null &&
             code.formalArguments.get(name)!=null &&  // no local value && it's a formal arg
             !passThroughAttributes )                 // but no ... in arg list
        {
            // if you've defined attribute as formal arg for this
            // template and it has no value, do not look up the
            // enclosing dynamic scopes.
            return null;
        }

        ST p = this.enclosingInstance;
        while ( p!=null ) {
            if ( p.attributes!=null ) o = p.attributes.get(name);
            if ( o!=null ) return o;
            p = p.enclosingInstance;
        }
        if ( code.formalArguments==null || code.formalArguments.get(name)==null ) {
            // if not hidden by formal args, return any dictionary
            return group.dictionaries.get(name);
        }
        return null;
    }

    protected AttributeList convertToAttributeList(Object curvalue) {
        AttributeList multi;
        if ( curvalue == null ) {
            multi = new AttributeList(); // make list to hold multiple values
            multi.add(curvalue);         // add previous single-valued attribute            
        }
        else if ( curvalue.getClass() == AttributeList.class ) { // already a list made by ST
            multi = (AttributeList)curvalue;
        }
        else if ( curvalue instanceof List) { // existing attribute is non-ST List
            // must copy to an ST-managed list before adding new attribute
            // (can't alter incoming attributes)
            List listAttr = (List)curvalue;
            multi = new AttributeList(listAttr.size());
            multi.addAll(listAttr);
        }
        else if ( curvalue.getClass().isArray() ) { // copy array to list
            Object[] a = (Object[])curvalue;
            multi = new AttributeList(a.length);
            multi.addAll(Arrays.asList(a)); // asList doesn't copy as far as I can tell
        }
        else {
            // curvalue nonlist and we want to add an attribute
            // must convert curvalue existing to list
            multi = new AttributeList(); // make list to hold multiple values
            multi.add(curvalue);         // add previous single-valued attribute
        }
        return multi;
    }

    /** If an instance of x is enclosed in a y which is in a z, return
     *  a String of these instance names in order from topmost to lowest;
     *  here that would be "[z y x]".
     */
    public String getEnclosingInstanceStackString() {
        List<String> names = new LinkedList<String>();
        ST p = this;
        while ( p!=null ) {
            String name = p.name;
            names.add(0,name);
            p = p.enclosingInstance;
        }
        return names.toString().replaceAll(",","");
    }    

    public int write(STWriter out) throws IOException {
        StringWriter sw = new StringWriter();
        Interpreter interp = new Interpreter(group);
        interp.exec(new AutoIndentWriter(sw), this);
        int n = out.write(sw.toString());
        return n;
    }

    public String render() {
        StringWriter out = new StringWriter();
        STWriter wr = new AutoIndentWriter(out);
        try {
            write(wr);
            /*
            System.err.println("template size = "+code.template.length()+
                               ", code size = "+code.instrs.length+", ratio = "+
                               ((float)code.instrs.length/code.template.length()));
                               */
        }
        catch (IOException io) {
            System.err.println("Got IOException writing to writer");
        }
        return out.toString();
    }

    public String toString() {
        return name+"()";
    }
}