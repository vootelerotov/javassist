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
import java.util.List;
import java.util.LinkedList;
import javassist.CannotCompileException;

/**
 * <code>method_info</code> structure.
 *
 * @see javassist.CtMethod#getMethodInfo()
 * @see javassist.CtConstructor#getMethodInfo()
 */
public final class MethodInfo {
    ConstPool constPool;
    int accessFlags;
    int name;
    int descriptor;
    LinkedList attribute;	// may be null

    /**
     * The name of constructors: <code>&lt;init&gt</code>.
     */
    public static final String nameInit = "<init>";

    /**
     * The name of class initializer (static initializer):
     * <code>&lt;clinit&gt</code>.
     */
    public static final String nameClinit = "<clinit>";

    private MethodInfo(ConstPool cp) {
	constPool = cp;
	attribute = null;
    }

    /**
     * Constructs a <code>method_info</code> structure.
     *
     * @param cp		a constant pool table
     * @param methodname	method name
     * @param desc		method descriptor
     *
     * @see Descriptor
     */
    public MethodInfo(ConstPool cp, String methodname, String desc) {
	this(cp);
	accessFlags = 0;
	name = cp.addUtf8Info(methodname);
	descriptor = constPool.addUtf8Info(desc);
    }

    MethodInfo(ConstPool cp, DataInputStream in) throws IOException {
	this(cp);
	read(in);
    }

    /**
     * Constructs a copy of <code>method_info</code> structure.
     * Class names appearing in the source <code>method_info</code>
     * are renamed according to <code>classnameMap</code>.
     *
     * <p>Note: only <code>Code</code> and <code>Exceptions</code>
     * attributes are copied from the source.  The other attributes
     * are ignored.
     *
     * @param cp		a constant pool table
     * @param methodname	a method name
     * @param src		a source <code>method_info</code>
     * @param classnameMap	specifies pairs of replaced and substituted
     *				name.
     * @see Descriptor
     */
    public MethodInfo(ConstPool cp, String methodname, MethodInfo src,
		      Map classnameMap) throws BadBytecode
    {
	this(cp);
	read(src, methodname, classnameMap);
    }

    /**
     * Returns a method name.
     */
    public String getName() {
	return constPool.getUtf8Info(name);
    }

    /**
     * Sets a method name.
     */
    public void setName(String newName) {
	name = constPool.addUtf8Info(newName);
    }

    /**
     * Returns true if this is not a constructor or a class initializer
     * (static initializer).
     */
    public boolean isMethod() {
	String n = getName();
	return !n.equals(nameInit) && !n.equals(nameClinit);
    }

    /**
     * Returns a constant pool table used by this method.
     */
    public ConstPool getConstPool() { return constPool; }

    /**
     * Returns true if this is a constructor.
     */
    public boolean isConstructor() { return getName().equals(nameInit); }

    /**
     * Returns true if this is a class initializer (static initializer).
     */
    public boolean isStaticInitializer() {
	return getName().equals(nameClinit);
    }

    /**
     * Returns access flags.
     *
     * @see AccessFlag
     */
    public int getAccessFlags() {
	return accessFlags;
    }

    /**
     * Sets access flags.
     *
     * @see AccessFlag
     */
    public void setAccessFlags(int acc) {
	accessFlags = acc;
    }

    /**
     * Returns a method descriptor.
     *
     * @see Descriptor
     */
    public String getDescriptor() {
	return constPool.getUtf8Info(descriptor);
    }

    /**
     * Sets a method descriptor.
     *
     * @see Descriptor
     */
    public void setDescriptor(String desc) {
	if (!desc.equals(getDescriptor()))
	    descriptor = constPool.addUtf8Info(desc);
    }

    /**
     * Returns all the attributes.
     *
     * @return a list of <code>AttributeInfo</code> objects.
     * @see AttributeInfo
     */
    public List getAttributes() {
	if (attribute == null)
	    attribute = new LinkedList();

	return attribute;
    }

    /**
     * Returns the attribute with the specified name.
     * If it is not found, this method returns null.
     *
     * @param name	attribute name
     * @return		an <code>AttributeInfo</code> object or null.
     */
    public AttributeInfo getAttribute(String name) {
	return AttributeInfo.lookup(attribute, name);
    }

    /**
     * Appends an attribute.  If there is already an attribute with
     * the same name, the new one substitutes for it.
     */
    public void addAttribute(AttributeInfo info) {
	if (attribute == null)
	    attribute = new LinkedList();

	AttributeInfo.remove(attribute, info.getName());
	attribute.add(info);
    }

    /**
     * Returns an Exceptions attribute.
     *
     * @return		an Exceptions attribute
     *			or null if it is not specified.
     */
    public ExceptionsAttribute getExceptionsAttribute() {
	AttributeInfo info
	    = AttributeInfo.lookup(attribute, ExceptionsAttribute.class);
	return (ExceptionsAttribute)info;
    }

    /**
     * Returns a Code attribute.
     *
     * @return		a Code attribute
     *			or null if it is not specified.
     */
    public CodeAttribute getCodeAttribute() {
	AttributeInfo info
	    = AttributeInfo.lookup(attribute, CodeAttribute.class);
	return (CodeAttribute)info;
    }

    /**
     * Removes an Exception attribute.
     */
    public void removeExceptionsAttribute() {
	AttributeInfo.remove(attribute, ExceptionsAttribute.class);
    }

    /**
     * Adds an Exception attribute.
     *
     * <p>The added attribute must share the same constant pool table
     * as this <code>method_info</code> structure.
     */
    public void setExceptionsAttribute(ExceptionsAttribute cattr) {
	removeExceptionsAttribute();
	if (attribute == null)
	    attribute = new LinkedList();

	attribute.add(cattr);
    }

    /**
     * Removes a Code attribute.
     */
    public void removeCodeAttribute() {
	AttributeInfo.remove(attribute, CodeAttribute.class);
    }

    /**
     * Adds a Code attribute.
     *
     * <p>The added attribute must share the same constant pool table
     * as this <code>method_info</code> structure.
     */
    public void setCodeAttribute(CodeAttribute cattr) {
	removeCodeAttribute();
	if (attribute == null)
	    attribute = new LinkedList();

	attribute.add(cattr);
    }

    /**
     * Returns the line number of the source line corresponding to the
     * specified bytecode contained in this method.
     *
     * @param pos	the position of the bytecode (&gt;= 0).
     *			an index into the code array.
     * @return -1	if this information is not available.
     */
    public int getLineNumber(int pos) {
	CodeAttribute ca = getCodeAttribute();
	if (ca == null)
	    return -1;

	LineNumberAttribute ainfo
	    = (LineNumberAttribute)ca.getAttribute(LineNumberAttribute.tag);
	if (ainfo == null)
	    return -1;

	return ainfo.toLineNumber(pos);
    }

    /**
     * Changes a super constructor called by this constructor.
     *
     * <p>This method modifies a call to <code>super()</code>,
     * which should be at the
     * head of a constructor body, so that a constructor in a different
     * super class is called.  This method does not change actural
     * parameters.  Hence the new super class must have a constructor
     * with the same signature as the original one.
     *
     * <p>This method should be called when the super class
     * of the class declaring this method is changed.
     *
     * <p>This method does not perform anything unless this
     * <code>MethodInfo</code> represents a constructor.
     *
     * @param superclass	the new super class
     */
    public void setSuperclass(String superclass) throws BadBytecode {
	if (!isConstructor())
	    return;

	CodeAttribute ca = getCodeAttribute();
	byte[] code = ca.getCode();
	CodeIterator iterator = ca.iterator();
	int pos = iterator.skipSuperConstructor();
	if (pos >= 0) {		// not this()
	    ConstPool cp = constPool;
	    int mref = ByteArray.readU16bit(code, pos + 1);
	    int nt = cp.getMethodrefNameAndType(mref);
	    int sc = cp.addClassInfo(superclass);
	    int mref2 = cp.addMethodrefInfo(sc, nt);
	    ByteArray.write16bit(mref2, code, pos + 1);
	}
    }

    private void read(MethodInfo src, String methodname, Map classnames)
	throws BadBytecode
    {
	ConstPool destCp = constPool;
	accessFlags = src.accessFlags;
	name = destCp.addUtf8Info(methodname);

	ConstPool srcCp = src.constPool;
	String desc = srcCp.getUtf8Info(src.descriptor);
	String desc2 = Descriptor.rename(desc, classnames);
	descriptor = destCp.addUtf8Info(desc2);

	attribute = new LinkedList();
	ExceptionsAttribute eattr = src.getExceptionsAttribute();
	if (eattr != null)
	    attribute.add(eattr.copy(destCp, classnames));

	CodeAttribute cattr = src.getCodeAttribute();
	if (cattr != null)
	    attribute.add(cattr.copy(destCp, classnames));
    }

    private void read(DataInputStream in) throws IOException {
	accessFlags = in.readUnsignedShort();
	name = in.readUnsignedShort();
	descriptor = in.readUnsignedShort();
	int n = in.readUnsignedShort();
	attribute = new LinkedList();
	for (int i = 0; i < n; ++i)
	    attribute.add(AttributeInfo.read(constPool, in));
    }

    void write(DataOutputStream out) throws IOException {
	out.writeShort(accessFlags);
	out.writeShort(name);
	out.writeShort(descriptor);

	if (attribute == null)
	    out.writeShort(0);
	else {
	    out.writeShort(attribute.size());
	    AttributeInfo.writeAll(attribute, out);
	}
    }
}