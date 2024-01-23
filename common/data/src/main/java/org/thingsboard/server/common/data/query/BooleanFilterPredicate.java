/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.query;

import lombok.Data;

@Data
public class BooleanFilterPredicate implements SimpleKeyFilterPredicate<Boolean> {

    private static final long serialVersionUID = 8308177419956886468L;

    private BooleanOperation operation;
    private FilterPredicateValue<Boolean> value;

    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.BOOLEAN;
    }

    public enum BooleanOperation {
        EQUAL,
        NOT_EQUAL
    }
}
