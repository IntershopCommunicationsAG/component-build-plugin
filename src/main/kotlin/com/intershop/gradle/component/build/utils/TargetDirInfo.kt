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
package com.intershop.gradle.component.build.utils

/**
 * Data class for target directories with all
 * necessary information also for error output.
 *
 * @property target target path
 * @property classifiers classifier - support for different OS
 * @property types information for deployment and environment types
 * @property owner information about the owner of the target
 *
 * @constructor provides the data class
 */
data class TargetDirInfo @JvmOverloads constructor(val target: String,
                                                   val classifiers: MutableSet<String>,
                                                   val types: MutableSet<String>,
                                                   val owner: String)
