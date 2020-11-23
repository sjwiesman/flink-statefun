#!/usr/bin/env bash
################################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
################################################################################

# fail immediately
set -o errexit
set -o nounset

BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
PROJECT_ROOT="${BASE_DIR}/../../"

# Sanity check to ensure that resolved paths are valid
if [ ! -f ${PROJECT_ROOT}/docs/build_docs.sh ]; then
    echo "Project root path ${PROJECT_ROOT} is not valid; script may be in the wrong directory."
    exit 1
fi

bundle exec --gemfile=${PROJECT_ROOT}/docs/Gemfile \
		jekyll serve --baseurl= --watch \
		--config ${PROJECT_ROOT}/docs/_config.yml \
		--source ${PROJECT_ROOT}/docs \
		--destination ${PROJECT_ROOT}/docs/content &

for i in `seq 1 30`;
do
	echo "Waiting for server..."
	curl -Is http://localhost:4000 --fail
	if [ $? -eq 0 ]; then
		break
	fi
	sleep 10
done

echo "built docs"
exit 0
# ${PROJECT_ROOT}/docs/check_links.sh
