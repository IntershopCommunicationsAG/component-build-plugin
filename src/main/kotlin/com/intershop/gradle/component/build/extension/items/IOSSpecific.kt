/*
 * Copyright 2018 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intershop.gradle.component.build.extension.items

/**
 * This interface provides the basic property of a OS specific object.
 */
interface IOSSpecific {

    /**
     * The classifier of the package type is used
     * for special platforms, like Windows, Linux, MacOS.
     * The default value is an empty string.
     *
     * @property classifier description of a target platform
     */
    val classifier: String
}
