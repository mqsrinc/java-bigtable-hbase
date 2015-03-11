/*
 * Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.bigtable.hbase.adapters;

import com.google.bigtable.anviltop.AnviltopData;
import com.google.cloud.bigtable.hbase.BigtableConstants;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapt a Row from Bigtable to an HBase Result
 */
public class RowAdapter implements ResponseAdapter<AnviltopData.Row, Result> {
  /**
   * Transform an Bigtable server response to an HBase Result instance.
   *
   * @param response The Bigtable response to transform.
   */
  @Override
  public Result adaptResponse(AnviltopData.Row response) {
    List<Cell> cells = new ArrayList<Cell>();
    byte[] rowKey = response.getRowKey().toByteArray();

    for (AnviltopData.Column column : response.getColumnsList()) {
      byte[] fullColumnName = column.getColumnName().toByteArray();

      int separatorIndex = getColumnSeparatorIndex(fullColumnName);

      byte[] columnFamily = new byte[separatorIndex];
      System.arraycopy(fullColumnName, 0, columnFamily, 0, separatorIndex);

      // Remove one extra to account for the separator byte:
      byte[] qualifier = new byte[
          fullColumnName.length
              - separatorIndex
              - BigtableConstants.BIGTABLE_COLUMN_SEPARATOR_LENGTH];

      System.arraycopy(
          fullColumnName,
          separatorIndex + BigtableConstants.BIGTABLE_COLUMN_SEPARATOR_LENGTH,
          qualifier,
          0,
          qualifier.length);

      for (AnviltopData.Cell cell  : column.getCellsList()) {
        long hbaseTimestamp = BigtableConstants.HBASE_TIMEUNIT.convert(
            cell.getTimestampMicros(),
            BigtableConstants.BIGTABLE_TIMEUNIT);

        KeyValue keyValue = new KeyValue(
            rowKey,
            columnFamily,
            qualifier,
            hbaseTimestamp,
            cell.getValue().toByteArray());

        cells.add(keyValue);
      }
    }

    Collections.sort(cells, KeyValue.COMPARATOR);

    return Result.create(cells);
  }

  private int getColumnSeparatorIndex(byte[] fullColumnName) {
    for (int idx = 0; idx < fullColumnName.length; idx++) {
      if (fullColumnName[idx] == BigtableConstants.BIGTABLE_COLUMN_SEPARATOR_BYTE) {
        return idx;
      }
    }
    throw new IllegalStateException("Failed to find column separator.");
  }
}
