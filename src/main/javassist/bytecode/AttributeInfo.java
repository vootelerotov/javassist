/*
 * This file is part of the Javassist toolkit.
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * either http://www.mozilla.org/MPL/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is Javassist.
 *
 * The Initial Developer of the Original Code is Shigeru Chiba.  Portions
 * created by Shigeru Chiba are Copyright (C) 1999-2003 Shigeru Chiba.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * The development of this software is supported in part by the PRESTO
 * program (Sakigake Kenkyu 21) of Japan Science and Technology Corporation.
 */

package javassist.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.LinkedList;
import java.util.ListIterator;

// Note: if you define a new subclass of AttributeInfo, then
//	 update AttributeInfo.read().

/**
 * <code>attribute_info</code> structure.
 */
public class AttributeInfo {
    protected ConstPool constPool;
    int name;
    byte[] info;

    protected AttributeInfo(ConstPool cp, int attrname, byte[] attrinfo) {
	constPool = cp;
	name = attrname;
	info = attrinfo;
    }

    protected AttributeInfo(ConstPool cp, String attrname) {
	this(cp, attrname, (byte[])null);
    }

    /**
     * Constructs an <code>attribute_info</code> structure.
     *
     * @param cp		constant pool table
     * @param attrname		attribute name
     * @param attrinfo		<code>info</code> field
     *				of <code>attribute_info</code> structure.
     */
    public AttributeInfo(ConstPool cp, String attrname, byte[] attrinfo) {
	this(cp, cp.addUtf8Info(attrname), attrinfo);
    }

    protected AttributeInfo(ConstPool cp, int n, DataInputStream in)
	throws IOException
    {
	constPool = cp;
	name = n;
	int len = in.readInt();
	info = new byte[len];
	if (len > 0)
	    in.readFully(info);
    }

    static AttributeInfo read(ConstPool cp, DataInputStream in)
	throws IOException
    {
	int name = in.readUnsignedShort();
	String nameStr = cp.getUtf8Info(name);
	if (nameStr.equals(CodeAttribute.tag))
	    return new CodeAttribute(cp, name, in);
	else if (nameStr.equals(ExceptionsAttribute.tag))
	    return new ExceptionsAttribute(cp, name, in);
	else if (nameStr.equals(ConstantAttribute.tag))
	    return new ConstantAttribute(cp, name, in);
	else if (nameStr.equals(SourceFileAttribute.tag))
	    return new SourceFileAttribute(cp, name, in);
	else if (nameStr.equals(LineNumberAttribute.tag))
	    return new LineNumberAttribute(cp, name, in);
	else if (nameStr.equals(SyntheticAttribute.tag))
	    return new SyntheticAttribute(cp, name, in);
	else if (nameStr.equals(InnerClassesAttribute.tag))
	    return new InnerClassesAttribute(cp, name, in);
	else
	    return new AttributeInfo(cp, name, in);
    }

    /**
     * Returns an attribute name.
     */
    public String getName() {
	return constPool.getUtf8Info(name);
    }

    /**
     * Returns a constant pool table.
     */
    public ConstPool getConstPool() { return constPool; }

    /**
     * Returns the length of this <code>attribute_info</code>
     * structure.
     * The returned value is <code>attribute_length + 6</code>.
     */
    public int length() {
	return info.length + 6;
    }

    /**
     * Returns the <code>info</code> field
     * of this <code>attribute_info</code> structure.
     *
     * <p>This method is not available if the object is an instance
     * of <code>CodeAttribute</code>.
     */
    public byte[] get() { return info; }

    /**
     * Sets the <code>info</code> field
     * of this <code>attribute_info</code> structure.
     *
     * <p>This method is not available if the object is an instance
     * of <code>CodeAttribute</code>.
     */
    public void set(byte[] newinfo) { info = newinfo; }

    /**
     * Makes a copy.  Class names are replaced according to the
     * given <code>Map</code> object.
     *
     * @param newCp	the constant pool table used by the new copy.
     * @param classnames	pairs of replaced and substituted
     *				class names.
     */
    public AttributeInfo copy(ConstPool newCp, Map classnames) {
	int s = info.length;
	byte[] newInfo = new byte[s];
	for (int i = 0; i < s; ++i)
	    newInfo[i] = info[i];

	return new AttributeInfo(newCp, getName(), newInfo);
    }

    void write(DataOutputStream out) throws IOException {
	out.writeShort(name);
	out.writeInt(info.length);
	if (info.length > 0)
	    out.write(info);
    }

    static int getLength(LinkedList list) {
	int size = 0;
	int n = list.size();
	for (int i = 0; i < n; ++i) {
	    AttributeInfo attr = (AttributeInfo)list.get(i);
	    size += attr.length();
	}

	return size;
    }

    static AttributeInfo lookup(LinkedList list, String name) {
	if (list == null)
	    return null;

	ListIterator iterator = list.listIterator();
	while (iterator.hasNext()) {
	    AttributeInfo ai = (AttributeInfo)iterator.next();
	    if (ai.getName().equals(name))
		return ai;
	}

	return null;		// no such attribute
    }

    static AttributeInfo lookup(LinkedList list, Class type) {
	if (list == null)
	    return null;

	ListIterator iterator = list.listIterator();
	while (iterator.hasNext()) {
	    Object obj = iterator.next();
	    if (type.isInstance(obj))
		return (AttributeInfo)obj;
	}

	return null;		// no such attribute
    }

    static synchronized void remove(LinkedList list, String name) {
	if (list == null)
	    return;

	ListIterator iterator = list.listIterator();
	while (iterator.hasNext()) {
	    AttributeInfo ai = (AttributeInfo)iterator.next();
	    if (ai.getName().equals(name))
		iterator.remove();
	}
    }

    static synchronized void remove(LinkedList list, Class type) {
	if (list == null)
	    return;

	ListIterator iterator = list.listIterator();
	while (iterator.hasNext()) {
	    Object obj = iterator.next();
	    if (type.isInstance(obj))
		iterator.remove();
	}
    }

    static void writeAll(LinkedList list, DataOutputStream out)
	throws IOException
    {
	if (list == null)
	    return;

	int n = list.size();
	for (int i = 0; i < n; ++i) {
	    AttributeInfo attr = (AttributeInfo)list.get(i);
	    attr.write(out);
	}
    }
}