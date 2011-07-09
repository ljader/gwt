/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dom.builder.shared;

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * HTML-based implementation of {@link TableSectionBuilder}.
 */
public class HtmlTableSectionBuilder extends HtmlElementBuilderBase<TableSectionBuilder> implements
    TableSectionBuilder {

  HtmlTableSectionBuilder(HtmlBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public TableSectionBuilder align(String align) {
    return attribute("align", align);
  }

  @Override
  public TableSectionBuilder ch(String ch) {
    return attribute("ch", ch);
  }

  @Override
  public TableSectionBuilder chOff(String chOff) {
    return attribute("chOff", chOff);
  }

  @Override
  public TableSectionBuilder html(SafeHtml html) {
    throw new UnsupportedOperationException(UNSUPPORTED_HTML);
  }

  @Override
  public TableSectionBuilder text(String text) {
    throw new UnsupportedOperationException(UNSUPPORTED_HTML);
  }

  @Override
  public TableSectionBuilder vAlign(String vAlign) {
    return attribute("vAlign", vAlign);
  }
}