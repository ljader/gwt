/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.editor.client;

import com.google.gwt.user.client.ui.TakesValue;

/**
 * Adapts various interfaces that provide Integer values to the Editor
 * architecture.
 */
public abstract class IntegerEditor extends PrimitiveValueEditor<Integer> {
  /**
   * Returns an editor with a <code>null</code> value.
   */
  public static IntegerEditor of() {
    return of((Integer) null);
  }

  /**
   * Returns an editor with a default value.
   */
  public static IntegerEditor of(final Integer value) {
    return new IntegerEditor() {
      private Integer v = value;

      public Integer getValue() {
        return v;
      }

      public void setValue(Integer value) {
        this.v = value;
      }
    };
  }

  /**
   * Returns an editor backed by a {@link TakesValue}, which is implemented by
   * many Widgets.
   */
  public static IntegerEditor of(final TakesValue<Integer> hasValue) {
    return new IntegerEditor() {
      public Integer getValue() {
        return hasValue.getValue();
      }

      public void setValue(Integer value) {
        hasValue.setValue(value);
      }
    };
  }

  /**
   * Prevent subclassing.
   */
  IntegerEditor() {
  }
}