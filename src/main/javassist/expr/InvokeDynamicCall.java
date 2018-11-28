package javassist.expr;

import java.util.HashMap;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.BootstrapMethodsAttribute;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;
import javassist.compiler.JvstCodeGen;
import javassist.compiler.JvstTypeChecker;
import javassist.compiler.ProceedHandler;
import javassist.compiler.ast.ASTList;

public class InvokeDynamicCall extends Expr {

  public InvokeDynamicCall(int pos, CodeIterator iterator, CtClass clazz, MethodInfo minfo) {
    super(pos, iterator, clazz, minfo);
  }

  public String getCallsiteMethodSignature() {
    ConstPool cp = thisClass.getClassFile().getConstPool();
    return cp.getInvokeDynamicType(iterator.u16bitAt(currentPos + 1));
  }

  public String getCallsiteMethodName() {
    ConstPool cp = thisClass.getClassFile().getConstPool();
    int nameAndTypeIndex = cp.getInvokeDynamicNameAndType(iterator.u16bitAt(currentPos + 1));
    int nameIndex = cp.getNameAndTypeName(nameAndTypeIndex);
    return cp.getUtf8Info(nameIndex);
  }

  public String getBootstrapMethodName() {
    ConstPool cp = thisClass.getClassFile().getConstPool();
    BootstrapMethodsAttribute.BootstrapMethod bsm = getBootstrapMethod();
    int mhRefIndex = cp.getMethodHandleIndex(bsm.methodRef);

    // Can assume it's not fieldRef or interfaceMethodRef
    // JVMS §4.7.21 invokedynamic
    // The reference_kind item of the CONSTANT_MethodHandle_info structure should have the value 6 (REF_invokeStatic) or 8 (REF_newInvokeSpecial) (§5.4.3.5)
    // or else invocation of the bootstrap method handle during call site specifier resolution for an invokedynamic instruction will complete abruptly.
    return cp.getMethodrefName(mhRefIndex);
  }

  public String getBootstrapMethodSignature() {
    ConstPool cp = thisClass.getClassFile().getConstPool();
    BootstrapMethodsAttribute.BootstrapMethod bsm = getBootstrapMethod();
    int mhRefIndex = cp.getMethodHandleIndex(bsm.methodRef);
    return cp.getMethodrefType(mhRefIndex);
  }

  public String getBootstrapMethodClass() {
    ConstPool cp = thisClass.getClassFile().getConstPool();
    BootstrapMethodsAttribute.BootstrapMethod bsm = getBootstrapMethod();
    int mhRefIndex = cp.getMethodHandleIndex(bsm.methodRef);
    return cp.getMethodrefClassName(mhRefIndex);
  }

  public int getBootstrapMethodArgCount() {
    return getBootstrapMethod().arguments.length;
  }

  /**
   * Returns the argument array as specified in the BootstrapMethods attribute
   * These arguments are appended to the implicit default ones provided by InvokeDynamic(Lookup, String, MethodType)
   * and together match the bootstrap method signature.
   *
   * Constant pool entries for primitives are returned as their boxed types.
   * Constant_ClassInfo, Constant_MethodHandle and Constant_MethodType use separate types.
   */
  public Object[] getBootstrapMethodArguments() {
    int[] argConstPoolIndexes = getBootstrapMethod().arguments;
    Object[] args = new Object[argConstPoolIndexes.length];
    ConstPool constPool = thisClass.getClassFile().getConstPool();

    for (int i = 0; i < argConstPoolIndexes.length; i++) {
      int argIndex = argConstPoolIndexes[i];
      switch (constPool.getTag(argIndex)) {
      case ConstPool.CONST_String:
        args[i] = constPool.getStringInfo(argIndex);
        break;
      case ConstPool.CONST_Class:
        args[i] = new ConstClassInfo(constPool.getClassInfo(argIndex));
        break;
      case ConstPool.CONST_Integer:
        args[i] = constPool.getIntegerInfo(argIndex);
        break;
      case ConstPool.CONST_Long:
        args[i] = constPool.getLongInfo(argIndex);
        break;
      case ConstPool.CONST_Float:
        args[i] = constPool.getFloatInfo(argIndex);
        break;
      case ConstPool.CONST_Double:
        args[i] = constPool.getDoubleInfo(argIndex);
        break;
      case ConstPool.CONST_MethodHandle:
        args[i] = parseMethodHandleReference(constPool, argIndex);
        break;
      case ConstPool.CONST_MethodType:
        args[i] = new ConstMethodType(constPool.getUtf8Info(constPool.getMethodTypeInfo(argIndex)));
      }
    }
    return args;
  }

  private BootstrapMethodsAttribute.BootstrapMethod getBootstrapMethod() {
    int constantPoolIndex = iterator.u16bitAt(currentPos + 1);
    int bootMethodTableIndex = thisMethod.getConstPool().getInvokeDynamicBootstrap(constantPoolIndex);
    BootstrapMethodsAttribute tableEntry = (BootstrapMethodsAttribute) thisClass.getClassFile().getAttribute(BootstrapMethodsAttribute.tag);

    if (tableEntry == null) {
      throw new IllegalStateException("Found InvokeDynamic call but no BootstrapMethods attribute in class");
    }

    return tableEntry.getMethods()[bootMethodTableIndex];
  }

  private MethodHandleReference parseMethodHandleReference(ConstPool constPool, int argIndex) {
    int methodHandleKind = constPool.getMethodHandleKind(argIndex);
    int methodHandleRefIndex = constPool.getMethodHandleIndex(argIndex);

    MethodHandleRefKind refKind = MethodHandleRefKind.fromConstantPoolValue(methodHandleKind);
    String className, name, type;

    if (methodHandleKind >= ConstPool.REF_getField && methodHandleKind <= ConstPool.REF_putStatic) {
      className = constPool.getFieldrefClassName(methodHandleRefIndex);
      name = constPool.getFieldrefName(methodHandleRefIndex);
      type = constPool.getFieldrefType(methodHandleRefIndex);
    }
    else if (methodHandleKind >= ConstPool.REF_invokeVirtual && methodHandleKind <= ConstPool.REF_newInvokeSpecial) {
      className = constPool.getMethodrefClassName(methodHandleRefIndex);
      name = constPool.getMethodrefName(methodHandleRefIndex);
      type = constPool.getMethodrefType(methodHandleRefIndex);
    }
    else if (methodHandleKind == ConstPool.REF_invokeInterface) {
      className = constPool.getInterfaceMethodrefClassName(methodHandleRefIndex);
      name = constPool.getInterfaceMethodrefName(methodHandleRefIndex);
      type = constPool.getInterfaceMethodrefType(methodHandleRefIndex);
    }
    else {
      throw new IllegalStateException("Unknown CONSTANT_InvokeDynamic ReferenceKind " + methodHandleKind);
    }

    return new MethodHandleReference(refKind, className, name, type);
  }

  public void replace(String statement) throws CannotCompileException {
    thisClass.getClassFile();
    ConstPool constPool = getConstPool();
    ClassPool cp = thisClass.getClassPool();

    int pos = currentPos;
    int constIndyIndex = iterator.u16bitAt(pos + 1);
    int bsmIndex = constPool.getInvokeDynamicBootstrap(constIndyIndex);

    Javac jc = new Javac(thisClass);
    CodeAttribute ca = iterator.get();
    try {
      String className = thisClass.getName();
      String desc = constPool.getInvokeDynamicType(constIndyIndex);
      CtClass[] params = Descriptor.getParameterTypes(desc, cp);
      CtClass retType = Descriptor.getReturnType(desc, cp);

      int paramVar = ca.getMaxLocals();
      jc.recordParams(className, params, true, paramVar, true);

      int retVarSlot = jc.recordReturnType(retType, true);
      jc.recordProceed(new ProceedForIndy(retType, bsmIndex));

      // This only checks that source contains $_, but not that it is assigned to the first time
      checkResultValue(retType, statement);
      Bytecode bytecode = jc.getBytecode();

      storeStack(params, true, paramVar, bytecode);
      jc.recordLocalVariables(ca, pos);

      jc.compileStmnt(statement);
      if (retType != CtClass.voidType)
        bytecode.addLoad(retVarSlot, retType);

      int opcodeSize = 5;
      replace0(pos, bytecode, opcodeSize);
    }
    catch (CompileError e) { throw new CannotCompileException(e); }
    catch (NotFoundException e) { throw new CannotCompileException(e); }
    catch (BadBytecode e) { throw new CannotCompileException(e); }
  }

  static class ProceedForIndy implements ProceedHandler {

    CtClass retType;
    int bsmIndex;

    public ProceedForIndy(CtClass retType, int bsmIndex) {
      this.retType = retType;
      this.bsmIndex = bsmIndex;
    }

    @Override
    public void doit(JvstCodeGen gen, Bytecode b, ASTList args) throws CompileError {
      // TODO: Check that arguments in stack match callsite signature
      b.addOpcode(Opcode.INVOKEDYNAMIC);
      b.addIndex(bsmIndex);
      b.add(0, 0);
    }

    @Override
    public void setReturnType(JvstTypeChecker c, ASTList args) throws CompileError {
      c.setType(retType);
    }
  }

  public static class Constant {}

  public static class ConstClassInfo extends Constant {
    public final String descriptor;

    public ConstClassInfo(String descriptor) {
      this.descriptor = descriptor;
    }
  }

  public enum MethodHandleRefKind {
    GETFIELD,
    GETSTATIC,
    PUTFIELD,
    PUTSTATIC,
    INVOKEVIRTUAL,
    INVOKESTATIC,
    INVOKESPECIAL,
    NEWINVOKESPECIAL,
    INVOKEINTERFACE;

    private static final Map<Integer, MethodHandleRefKind> constPoolValueToEnum = new HashMap<Integer, MethodHandleRefKind>();

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

    private static MethodHandleRefKind fromConstantPoolValue(int i) {
      return constPoolValueToEnum.get(i);
    }
  }

  public static class MethodHandleReference extends Constant {

    private final MethodHandleRefKind refKind;
    private final String className;
    private final String name;
    private final String descriptor;

    public MethodHandleReference(MethodHandleRefKind refKind, String className, String name, String descriptor) {
      this.refKind = refKind;
      this.className = className;
      this.name = name;
      this.descriptor = descriptor;
    }

    public MethodHandleRefKind getReferenceKind() {
      return refKind;
    }

    public String getClassName() {
      return className;
    }

    public String getName() {
      return name;
    }

    public String getDescriptor() {
      return descriptor;
    }

    public boolean isFieldRef() {
      return refKind.ordinal() >= ConstPool.REF_getField && refKind.ordinal() <= ConstPool.REF_putStatic;
    }

    public boolean isMethodRef() {
      return refKind.ordinal() >= ConstPool.REF_invokeVirtual && refKind.ordinal() <= ConstPool.REF_newInvokeSpecial;
    }

    public boolean isInterfaceMethodRef() {
      return refKind == MethodHandleRefKind.INVOKEINTERFACE;
    }
  }

  public static class ConstMethodType extends Constant {

    public final String methodSignature;

    public ConstMethodType(String methodSignature) {
      this.methodSignature = methodSignature;
    }
  }
}
