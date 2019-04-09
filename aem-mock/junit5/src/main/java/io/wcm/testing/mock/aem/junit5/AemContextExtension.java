/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.testing.mock.aem.junit5;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * JUnit 5 extension that allows to inject {@link AemContext} (or subclasses of it) parameters in test methods,
 * and ensures that the context is set up and teared down properly for each test method.
 */
public final class AemContextExtension implements ParameterResolver, TestInstancePostProcessor,
    BeforeEachCallback, AfterEachCallback, AfterTestExecutionCallback {

  /**
   * Checks if test class has a {@link AemContext} or derived field.
   * If it has and is not instantiated, create an new {@link AemContext} and store it in the field.
   * If it is already instantiated reuse this instance and use it for all test methods.
   */
  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) throws Exception {
    Field aemContextField = getFieldFromTestInstance(testInstance, AemContext.class);
    if (aemContextField != null) {
      AemContext context = (AemContext)aemContextField.get(testInstance);
      if (context != null) {
        if (!context.isSetUp()) {
          context.setUpContext();
        }
        AemContextStore.storeAemContext(extensionContext, testInstance, context);
      }
      else {
        context = AemContextStore.getOrCreateAemContext(extensionContext, testInstance,
            Optional.of(aemContextField.getType()));
        aemContextField.set(testInstance, context);
      }
    }
  }

  /**
   * Support parameter injection for test methods of parameter type is derived from {@link AemContext}.
   */
  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return AemContext.class.isAssignableFrom(parameterContext.getParameter().getType());
  }

  /**
   * Resolve (or create) {@link AemContext} instance for test method parameter.
   */
  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    AemContext aemContext = AemContextStore.getOrCreateAemContext(extensionContext, extensionContext.getRequiredTestInstance(),
        getAemContextType(parameterContext, extensionContext));
    if (paramIsNotInstanceOfExistingContext(parameterContext, aemContext)) {
      throw new ParameterResolutionException(
          "Found AemContext instance of type: " + aemContext.getClass().getName() + "\n"
              + "Required is: " + parameterContext.getParameter().getType().getName() + "\n"
              + "Verify that all test lifecycle methods (@BeforeEach, @Test, @AfterEach) "
              + "use the same AemContext type.");
    }
    return aemContext;
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    applyAemContext(extensionContext, aemContext -> {
      // call context plugins setup after all @BeforeEach methods were called
      aemContext.getContextPlugins().executeAfterSetUpCallback(aemContext);
    });
  }

  @Override
  public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
    applyAemContext(extensionContext, aemContext -> {
      // call context plugins setup before any @AfterEach method is called
      aemContext.getContextPlugins().executeBeforeTearDownCallback(aemContext);
    });
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    applyAemContext(extensionContext, aemContext -> {
      // call context plugins setup after all @AfterEach methods were called
      aemContext.getContextPlugins().executeAfterTearDownCallback(aemContext);

      // Tear down {@link AemContext} after test is complete.
      aemContext.tearDownContext();
      AemContextStore.removeAemContext(extensionContext, extensionContext.getRequiredTestInstance());
    });
  }

  private void applyAemContext(ExtensionContext extensionContext, Consumer<AemContext> consumer) {
    AemContext aemContext = AemContextStore.getAemContext(extensionContext, extensionContext.getRequiredTestInstance());
    if (aemContext != null) {
      consumer.accept(aemContext);
    }
  }

  private Optional<Class<?>> getAemContextType(ParameterContext parameterContext, ExtensionContext extensionContext) {
    // If a @BeforeEach or @AfterEach method has only a generic AemContext parameter check if
    // test method has a more specific parameter and use this
    if (isAbstractAemContext(parameterContext)) {
      return getParameterFromTestMethod(extensionContext, AemContext.class);
    }
    else {
      return Optional.of(parameterContext.getParameter().getType());
    }
  }

  private boolean isAbstractAemContext(ParameterContext parameterContext) {
    return parameterContext.getParameter().getType().equals(AemContext.class);
  }

  private boolean paramIsNotInstanceOfExistingContext(ParameterContext parameterContext, AemContext aemContext) {
    return !parameterContext.getParameter().getType().isInstance(aemContext);
  }

  private Optional<Class<?>> getParameterFromTestMethod(ExtensionContext extensionContext, Class<?> type) {
    return Arrays.stream(extensionContext.getRequiredTestMethod().getParameterTypes())
        .filter(clazz -> type.isAssignableFrom(clazz))
        .findFirst();
  }

  private Field getFieldFromTestInstance(Object testInstance, Class<?> type) {
    return getFieldFromTestInstance(testInstance.getClass(), type);
  }

  private Field getFieldFromTestInstance(Class<?> instanceClass, Class<?> type) {
    if (instanceClass == null) {
      return null;
    }
    Field contextField = Arrays.stream(instanceClass.getDeclaredFields())
        .filter(field -> type.isAssignableFrom(field.getType()))
        .findFirst()
        .orElse(null);
    if (contextField != null) {
      contextField.setAccessible(true);
    }
    else {
      return getFieldFromTestInstance(instanceClass.getSuperclass(), type);
    }
    return contextField;
  }

}
