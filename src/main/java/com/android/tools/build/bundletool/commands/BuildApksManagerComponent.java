/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.android.tools.build.bundletool.commands;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.android.tools.build.bundletool.io.TempDirectory;
import com.android.tools.build.bundletool.io.ZipReader;
import com.android.tools.build.bundletool.model.AppBundle;
import dagger.BindsInstance;
import dagger.Component;
import java.lang.annotation.Retention;
import javax.inject.Qualifier;

/** Dagger component to create a {@link BuildApksManager}. */
@CommandScoped
@Component(modules = BuildApksModule.class)
public interface BuildApksManagerComponent {
  BuildApksManager create();

  /** Builder for the {@link BuildApksManagerComponent}. */
  @Component.Builder
  interface Builder {
    BuildApksManagerComponent build();

    @BindsInstance
    Builder setTempDirectory(TempDirectory tempDirectory);

    @BindsInstance
    Builder setBuildApksCommand(BuildApksCommand command);

    @BindsInstance
    Builder setAppBundle(AppBundle appBundle);

    @BindsInstance
    Builder setZipReader(ZipReader zipReader);

    @BindsInstance
    Builder setUseBundleCompression(@UseBundleCompression boolean useBundleCompression);
  }

  /**
   * Qualifying annotation a {@code boolean} on whether entries in the APKs can be serialized with
   * the same compression as the entries in the App Bundle.
   */
  @Qualifier
  @Retention(RUNTIME)
  public @interface UseBundleCompression {}
}
