package javassist.expr;

import java.util.HashMap;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.bytecode.BootstrapMethodsAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class InvokeDynamicCall extends Expr {
  public InvokeDynamicCall(int pos, CodeIterator iterator, CtClass clazz, MethodInfo minfo) {
    super(pos, iterator, clazz, minfo);
  }

  public ConstMethodHandle getMethodInfo() {
    return parseMethodHandleConst(thisClass.getClassFile().getConstPool(), getBootstrapMethodsAttribute().methodRef);
  }

  public int argumentCount() {
    return getBootstrapMethodsAttribute().arguments.length;
  }

  public Constant[] getArguments() {
    int[] argIndexes = getBootstrapMethodsAttribute().arguments;
    Constant[] args = new Constant[argIndexes.length];
    ConstPool constPool = thisClass.getClassFile().getConstPool();
    for (int i = 0; i < argIndexes.length; i++) {
      int argIndex = argIndexes[i];
      switch (constPool.getTag(argIndex)) {
      case ConstPool.CONST_String:
        args[i] = new ConstString(constPool.getStringInfo(argIndex));
        break;
      case ConstPool.CONST_Class:
        args[i] = new ConstClass(constPool.getClassInfo(argIndex));
        break;
      case ConstPool.CONST_Integer:
        args[i] = new ConstInteger(constPool.getIntegerInfo(argIndex));
        break;
      case ConstPool.CONST_Long:
        args[i] = new ConstLong(constPool.getLongInfo(argIndex));
        break;
      case ConstPool.CONST_Float:
        args[i] = new ConstFloat(constPool.getFloatInfo(argIndex));
        break;
      case ConstPool.CONST_Double:
        args[i] = new ConstDouble(constPool.getDoubleInfo(argIndex));
        break;
      case ConstPool.CONST_MethodHandle:
        args[i] = parseMethodHandleConst(constPool, argIndex);
        break;
      case ConstPool.CONST_MethodType:
        args[i] = new ConstMethodType(constPool.getUtf8Info(constPool.getMethodTypeInfo(argIndex)));

      }
    }
    return args;
  }

  private BootstrapMethodsAttribute.BootstrapMethod getBootstrapMethodsAttribute() {
    int constantPoolIndex = iterator.u16bitAt(currentPos + 1);
    int bootMethodTableIndex = thisMethod.getConstPool().getInvokeDynamicBootstrap(constantPoolIndex);
    BootstrapMethodsAttribute tableEntry = (BootstrapMethodsAttribute) thisClass.getClassFile().getAttribute(BootstrapMethodsAttribute.tag);
    return tableEntry.getMethods()[bootMethodTableIndex];
  }

  private ConstMethodHandle parseMethodHandleConst(ConstPool constPool, int argIndex) {
    int methodHandleKind = constPool.getMethodHandleKind(argIndex);
    int methodHandleIndex = constPool.getMethodHandleIndex(argIndex);
    if (methodHandleKind == ConstPool.REF_getField ||
        methodHandleKind == ConstPool.REF_getStatic ||
        methodHandleKind == ConstPool.REF_putField ||
        methodHandleKind == ConstPool.REF_putStatic) {

      return new FieldConst(
          ConstPoolRef.fromConstantPoolValue(methodHandleKind),
          constPool.getFieldrefClassName(methodHandleIndex),
          constPool.getFieldrefName(methodHandleIndex),
          constPool.getFieldrefType(methodHandleIndex)
      );

    }
    else if (methodHandleKind == ConstPool.REF_invokeVirtual ||
    methodHandleKind == ConstPool.REF_invokeStatic ||
    methodHandleKind == ConstPool.REF_invokeSpecial ||
    methodHandleKind == ConstPool.REF_newInvokeSpecial) {
      return new MethodConst(
          ConstPoolRef.fromConstantPoolValue(methodHandleKind),
          constPool.getMethodrefClassName(methodHandleIndex),
          constPool.getMethodrefName(methodHandleIndex),
          constPool.getMethodrefType(methodHandleIndex)
      );
    }
    else if (methodHandleKind == ConstPool.REF_invokeInterface) {
      return new MethodConst(
          ConstPoolRef.fromConstantPoolValue(methodHandleKind),
          constPool.getInterfaceMethodrefClassName(methodHandleIndex),
          constPool.getInterfaceMethodrefName(methodHandleIndex),
          constPool.getInterfaceMethodrefType(methodHandleIndex)
      );
    }
    throw new IllegalStateException("Unknown ref");
  }

  public void replace(String statement) throws CannotCompileException {
    throw new NotImplementedException();
  }

  public static class Constant {

  }

  public static class ConstString extends Constant {
    public final String stringInfo;

    public ConstString(String stringInfo) {
      this.stringInfo = stringInfo;
    }
  }

  public static class ConstClass extends Constant {
    public final String classInfo;

    public ConstClass(String classInfo) {
      this.classInfo = classInfo;
    }
  }

  public static class ConstLong extends Constant {
    public long longInfo;

    public ConstLong(long longInfo) {
      this.longInfo = longInfo;
    }
  }

  public class ConstDouble extends Constant {
    public final double doubleInfo;

    public ConstDouble(double doubleInfo) {
      this.doubleInfo = doubleInfo;
    }
  }

  public static class ConstInteger extends Constant {
    public final int integerInfo;

    public ConstInteger(int integerInfo) {
      this.integerInfo = integerInfo;
    }
  }

  public static class ConstFloat extends Constant {

    public float floatInfo;

    public ConstFloat(float floatInfo) {
      this.floatInfo = floatInfo;
    }
  }

  private class ConstMethodHandle extends Constant {

  }

  public enum ConstPoolRef {
    GETFIELD,
    GETSTATIC,
    PUTFIELD,
    PUTSTATIC,
    INVOKEVIRTUAL,
    INVOKESTATIC,
    INVOKESPECIAL,
    NEWINVOKESPECIAL,
    INVOKEINTERFACE;



    private static final Map<Integer, ConstPoolRef> constPoolValueToEnum = new HashMap<Integer, ConstPoolRef>();

    static {
      constPoolValueToEnum.put(ConstPool.REF_getField, GETFIELD);
      constPoolValueToEnum.put(ConstPool.REF_getStatic, GETSTATIC);
      constPoolValueToEnum.put(ConstPool.REF_putField, PUTFIELD);
      constPoolValueToEnum.put(ConstPool.REF_putStatic, PUTSTATIC);
      constPoolValueToEnum.put(ConstPool.REF_invokeVirtual, INVOKEVIRTUAL);
      constPoolValueToEnum.put(ConstPool.REF_invokeStatic, INVOKESTATIC);
      constPoolValueToEnum.put(ConstPool.REF_invokeSpecial, INVOKESPECIAL);
      constPoolValueToEnum.put(ConstPool.REF_newInvokeSpecial, NEWINVOKESPECIAL);
      constPoolValueToEnum.put(ConstPool.REF_invokeInterface, INVOKEINTERFACE);
    }

    private static ConstPoolRef fromConstantPoolValue(int i) {
      return constPoolValueToEnum.get(i);
    }
  }

  public class FieldConst extends ConstMethodHandle {
    public final ConstPoolRef constPoolRef;
    public final String fieldrefClassName;
    public final String fieldrefName;
    public final String fieldrefType;

    public FieldConst(ConstPoolRef constPoolRef, String fieldrefClassName, String fieldrefName, String fieldrefType) {
      this.constPoolRef = constPoolRef;
      this.fieldrefClassName = fieldrefClassName;
      this.fieldrefName = fieldrefName;
      this.fieldrefType = fieldrefType;
    }
  }

  public class MethodConst extends ConstMethodHandle {
    public final ConstPoolRef constPoolRef;
    public final String className;
    public final String methodName;
    public final String desc;

    public MethodConst(ConstPoolRef constPoolRef, String className, String methodName, String desc) {
      this.constPoolRef = constPoolRef;
      this.className = className;
      this.methodName = methodName;
      this.desc = desc;
    }
  }

  public class ConstMethodType extends Constant {
    public final String methodSignature;

    public ConstMethodType(String methodSignature) {
      this.methodSignature = methodSignature;
    }
  }
}
