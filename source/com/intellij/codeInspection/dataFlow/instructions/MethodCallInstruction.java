/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Jan 26, 2002
 * Time: 10:48:52 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInspection.dataFlow.instructions;

import com.intellij.codeInspection.dataFlow.DataFlowRunner;
import com.intellij.codeInspection.dataFlow.DfaInstructionState;
import com.intellij.codeInspection.dataFlow.DfaMemoryState;
import com.intellij.codeInspection.dataFlow.value.DfaValue;
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory;
import com.intellij.codeInspection.dataFlow.value.DfaUnknownValue;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class MethodCallInstruction extends Instruction {
  private final PsiCallExpression myCall;
  private DfaValueFactory myFactory;
  private boolean myIsNullable;
  private boolean myIsNotNull;
  private boolean[] myParametersNotNull;
  private @Nullable PsiType myType;
  private @NotNull PsiExpression[] myArgs;

  public MethodCallInstruction(PsiCallExpression call, DfaValueFactory factory) {
    myCall = call;
    myFactory = factory;
    final PsiMethod callee = call.resolveMethod();
    final PsiExpressionList argList = myCall.getArgumentList();
    myArgs = argList != null ? argList.getExpressions() : new PsiExpression[0];

    if (callee != null) {
      final PsiModifierList modifierList = callee.getModifierList();
      myIsNullable = modifierList.findAnnotation(DataFlowRunner.NULLABLE) != null;
      myIsNotNull = modifierList.findAnnotation(DataFlowRunner.NOT_NULL) != null;
      final PsiParameter[] params = callee.getParameterList().getParameters();
      myParametersNotNull = new boolean[params.length];
      for (int i = 0; i < params.length; i++) {
        PsiParameter param = params[i];
        final PsiModifierList modList = param.getModifierList();
        myParametersNotNull[i] = modList != null && modList.findAnnotation(DataFlowRunner.NOT_NULL) != null;
      }
    }
    else {
      myParametersNotNull = new boolean[0];
    }
    myType = myCall.getType();

    if (call instanceof PsiNewExpression) {
      myIsNullable = false;
      myIsNotNull = true;
    }
  }

  public DfaInstructionState[] apply(DataFlowRunner runner, DfaMemoryState memState) {
    final @NotNull DfaValue qualifier = memState.pop();

    for (int i = 0; i < myArgs.length; i++) {
      final DfaValue arg = memState.pop();
      if (i < myParametersNotNull.length && myParametersNotNull[i] && !memState.applyNotNull(arg)) {
        runner.onPassingNullParameter(myArgs[i]);
        return new DfaInstructionState[0];
      }
    }

    try {
      if (!memState.applyNotNull(qualifier)) {
        runner.onInstructionProducesNPE(this);
        return new DfaInstructionState[0];
      }

      return new DfaInstructionState[]{new DfaInstructionState(runner.getInstruction(getIndex() + 1), memState)};
    }
    finally {
      pushResult(memState);
      memState.flushFields(runner);
    }

  }

  private void pushResult(DfaMemoryState state) {
    final DfaValue dfaValue;
    if (myType != null && myType instanceof PsiClassType) {
      dfaValue = myIsNotNull ? myFactory.getNotNullFactory().create(myType) : myFactory.getTypeFactory().create(myType, myIsNullable);
    }
    else {
      dfaValue = DfaUnknownValue.getInstance();
    }

    state.push(dfaValue);
  }

  public PsiCallExpression getCallExpression() {
    return myCall;
  }

  public String toString() {
    return "CALL_METHOD: " + myCall.getText();
  }
}
