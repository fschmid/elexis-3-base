/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.client.solrj.io.eval;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;

public class TruncEvaluator extends RecursiveObjectEvaluator implements TwoValueWorker {
	protected static final long serialVersionUID = 1L;

	public TruncEvaluator(StreamExpression expression, StreamFactory factory) throws IOException {
		super(expression, factory);

		if (2 != containedEvaluators.size()) {
			throw new IOException(
					String.format(Locale.ROOT, "Invalid expression %s - expecting exactly 2 values but found %d",
							expression, containedEvaluators.size()));
		}
	}

	@Override
	public Object doWork(Object value1, Object value2) {
		if (null == value1) {
			return null;
		}

		int endIndex = ((Number) value2).intValue();

		if (value1 instanceof List) {
			return ((List<?>) value1).stream().map(innerValue -> doWork(innerValue, endIndex))
					.collect(Collectors.toList());
		} else {
			return value1.toString().substring(0, endIndex);
		}
	}
}
