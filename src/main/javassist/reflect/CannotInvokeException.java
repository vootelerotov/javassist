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

package javassist.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.IllegalAccessException;

/**
 * Thrown when method invocation using the reflection API has thrown
 * an exception.
 *
 * @see javassist.reflect.Metaobject#trapMethodcall(int, Object[])
 * @see javassist.reflect.ClassMetaobject#trapMethodcall(int, Object[])
 * @see javassist.reflect.ClassMetaobject#invoke(Object, int, Object[])
 */
public class CannotInvokeException extends RuntimeException {
    /**
     * @serial
     */
    private Throwable err = null;

    /**
     * Constructs a CannotInvokeException with an error message.
     */
    public CannotInvokeException(String reason) {
	super(reason);
    }

    /**
     * Constructs a CannotInvokeException with an InvocationTargetException.
     */
    public CannotInvokeException(InvocationTargetException e) {
	super("by " + e.getTargetException().toString());
	err = e.getTargetException();
    }

    /**
     * Constructs a CannotInvokeException with an IllegalAccessException.
     */
    public CannotInvokeException(IllegalAccessException e) {
	super("by " + e.toString());
	err = e;
    }

    /**
     * Constructs a CannotInvokeException with an ClassNotFoundException.
     */
    public CannotInvokeException(ClassNotFoundException e) {
	super("by " + e.toString());
	err = e;
    }
}