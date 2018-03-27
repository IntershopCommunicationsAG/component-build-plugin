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

import org.gradle.api.tasks.Input

/**
 * This class contains methods for items with additional
 * dependencies.
 *
 * @constructor provides an empty item with parent item
 */
abstract class ADependencyItem : AItem(), IDependency {

    /**
     * This property configures the dependency resolution
     * of the configured dependency during the creation
     * of the descriptor. The descriptor must be complete!
     * The default value is true.
     *
     * @property resolveTransitive if true dependencies will be resolved transitive.
     */
    @get:Input
    override var resolveTransitive: Boolean = true

}
