/*
 * Copyright 2016 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.client.expression;

import com.marklogic.client.type.XsStringExpr;
 import com.marklogic.client.type.XsStringSeqExpr;
 import com.marklogic.client.type.XsUnsignedIntExpr;
 import com.marklogic.client.type.MapMapExpr;
 import com.marklogic.client.type.XsBooleanExpr;
 import com.marklogic.client.type.ElementNodeExpr;
 import com.marklogic.client.type.ItemExpr;
 import com.marklogic.client.type.ItemSeqExpr;
 import com.marklogic.client.type.MapMapSeqExpr;


// IMPORTANT: Do not edit. This file is generated. 
public interface Map {
    public XsBooleanExpr contains(MapMapExpr map, String key);
    public XsBooleanExpr contains(MapMapExpr map, XsStringExpr key);
    public XsUnsignedIntExpr count(MapMapExpr map);
    public MapMapExpr entry(String key, ItemExpr... value);
    public MapMapExpr entry(XsStringExpr key, ItemSeqExpr value);
    public ItemSeqExpr get(MapMapExpr map, String key);
    public ItemSeqExpr get(MapMapExpr map, XsStringExpr key);
    public XsStringSeqExpr keys(MapMapExpr map);
    public MapMapExpr map();
    public MapMapExpr map(ElementNodeExpr map);     public MapMapSeqExpr map(MapMapExpr... items);

}