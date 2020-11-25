// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package statefun

import (
	"github.com/apache/flink-statefun/statefun-go-sdk/pkg/flink/statefun/internal/messages"
	"github.com/stretchr/testify/assert"
	"testing"
	"time"
)

func TestDefaultStateSpecTranslation(t *testing.T) {
	spec := StateSpec{
		StateName: "A",
	}.toInternal()

	assert.Equal(t, "A", spec.StateName, "wrong name for state value")
	assert.Equal(t, messages.FromFunction_ExpirationSpec_NONE, spec.ExpirationSpec.Mode, "wrong default expiration mode")
}

func TestStateSpecTranslationWithExpiration(t *testing.T) {
	spec := StateSpec{
		StateName: "A",
		ExpirationSpec: ExpirationSpec{
			ExpireMode: AfterWrite,
			Ttl:        1 * time.Minute,
		},
	}.toInternal()

	assert.Equal(t, "A", spec.StateName, "wrong name for state value")
	assert.Equal(t, messages.FromFunction_ExpirationSpec_AFTER_WRITE, spec.ExpirationSpec.Mode, "wrong expiration mode")
	assert.Equal(t, int64(60000), spec.ExpirationSpec.ExpireAfterMillis, "wrong expiration time")
}
