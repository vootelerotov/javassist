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

package javassist.convert;

import javassist.bytecode.*;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.CannotCompileException;

final public class TransformFieldAccess extends Transformer {
    private String newClassname, newFieldname;
    private String fieldname;
    private CtClass fieldClass;
    private boolean isPrivate;

    /* cache */
    private int newIndex;
    private ConstPool constPool;

    public TransformFieldAccess(Transformer next, CtField field,
				String newClassname, String newFieldname)
    {
	super(next);
	this.fieldClass = field.getDeclaringClass();
	this.fieldname = field.getName();
	this.isPrivate = Modifier.isPrivate(field.getModifiers());
	this.newClassname = newClassname;
	this.newFieldname = newFieldname;
	this.constPool = null;
    }

    public void initialize(ConstPool cp, CodeAttribute attr) {
	if (constPool != cp)
	    newIndex = 0;
    }

    /**
     * Modify GETFIELD, GETSTATIC, PUTFIELD, and PUTSTATIC so that
     * a different field is accessed.  The new field must be declared
     * in a superclass of the class in which the original field is
     * declared.
     */
    public int transform(CtClass clazz, int pos,
			 CodeIterator iterator, ConstPool cp)
    {
	int c = iterator.byteAt(pos);
	if (c == GETFIELD || c == GETSTATIC
				|| c == PUTFIELD || c == PUTSTATIC) {
	    int index = iterator.u16bitAt(pos + 1);
	    String typedesc
		= TransformReadField.isField(clazz.getClassPool(), cp,
				fieldClass, fieldname, isPrivate, index);
	    if (typedesc != null) {
		if (newIndex == 0) {
		    int nt = cp.addNameAndTypeInfo(newFieldname,
						   typedesc);
		    newIndex = cp.addFieldrefInfo(
					cp.addClassInfo(newClassname), nt);
		    constPool = cp;
		}

		iterator.write16bit(newIndex, pos + 1);
	    }
	}

	return pos;
    }
}