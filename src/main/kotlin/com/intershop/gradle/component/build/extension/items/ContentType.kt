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
 * Enumeration with possible content types. This
 * configuration is used for the deployment self.
 *
 * @property description description of the enum.
 */
@Suppress("unused")
enum class ContentType (val description: String) {

    /**
     * Static content - can be installed without any exception. Old content
     * will be replaced by new one.
     */
    IMMUTABLE("Immutable / Static Content"),
    /**
     * Data content - must be special handled during a
     * deployment. Existing content can not be replaced easily.
     */
    DATA("Data Content"),
    /**
     * Configuration content - this content must be adapted during the
     * installation.
     */
    CONFIGURATION("Configuration Content"),
    /**
     * Unspecified content.
     */
    UNSPECIFIED("Unspecified Content")
}
