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
 * This interface provides basic methods of a container object.
 */
interface IContainer {

    /**
     * If the target path is included in the file container it returns true.
     */
    val targetIncluded: Boolean

    /**
     * Target path of the container item.
     */
    val targetPath: String

    /**
     * Exclude patterns for installation.
     */
    val excludes: Set<String>

    /**
     * Preserve exclude patterns for update installation.
     */
    val preserveExcludes: Set<String>

    /**
     * Preserve include patterns for update installation.
     */
    val preserveIncludes: Set<String>

    /**
     * This property can be used to add a special type
     * description for a module.
     */
    var itemType: String
}
