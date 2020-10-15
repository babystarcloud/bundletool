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

package com.android.tools.build.bundletool.splitters;

import static com.android.tools.build.bundletool.model.BundleModule.ASSETS_DIRECTORY;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.androidManifest;
import static com.android.tools.build.bundletool.testing.TargetingUtils.apkDeviceTierTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.assets;
import static com.android.tools.build.bundletool.testing.TargetingUtils.assetsDirectoryTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.deviceTierTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.getSplitsWithDefaultTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.getSplitsWithTargetingEqualTo;
import static com.android.tools.build.bundletool.testing.TargetingUtils.targetedAssetsDirectory;
import static com.android.tools.build.bundletool.testing.TestUtils.extractPaths;
import static com.google.common.truth.Truth.assertThat;

import com.android.bundle.Targeting.AssetsDirectoryTargeting;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.model.ModuleSplit;
import com.android.tools.build.bundletool.testing.BundleModuleBuilder;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link DeviceTierAssetsSplitter}. */
@RunWith(JUnit4.class)
public class DeviceTierAssetsSplitterTest {

  @Test
  public void multipleDeviceTiersAndUntargetedFile() {
    BundleModule testModule =
        new BundleModuleBuilder("testModule")
            .addFile("assets/images#tier_low/image.jpg")
            .addFile("assets/images#tier_medium/image.jpg")
            .addFile("assets/images#tier_high/image.jpg")
            .addFile("assets/file.txt")
            .setAssetsConfig(
                assets(
                    targetedAssetsDirectory(
                        "assets/images#tier_low",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                /* value= */ "low",
                                /* alternatives= */ ImmutableList.of("medium", "high")))),
                    targetedAssetsDirectory(
                        "assets/images#tier_medium",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                /* value= */ "medium",
                                /* alternatives= */ ImmutableList.of("low", "high")))),
                    targetedAssetsDirectory(
                        "assets/images#tier_high",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                /* value= */ "high",
                                /* alternatives= */ ImmutableList.of("low", "medium")))),
                    targetedAssetsDirectory(
                        "assets", AssetsDirectoryTargeting.getDefaultInstance())))
            .setManifest(androidManifest("com.test.app"))
            .build();

    ModuleSplit baseSplit = ModuleSplit.forAssets(testModule);
    Collection<ModuleSplit> assetsSplits =
        DeviceTierAssetsSplitter.create(/* stripTargetingSuffix= */ false).split(baseSplit);

    assertThat(assetsSplits).hasSize(4);
    List<ModuleSplit> defaultSplits = getSplitsWithDefaultTargeting(assetsSplits);
    assertThat(defaultSplits).hasSize(1);
    assertThat(extractPaths(defaultSplits.get(0).findEntriesUnderPath(ASSETS_DIRECTORY)))
        .containsExactly("assets/file.txt");

    List<ModuleSplit> lowSplits =
        getSplitsWithTargetingEqualTo(
            assetsSplits,
            apkDeviceTierTargeting(
                deviceTierTargeting(
                    /* value= */ "low", /* alternatives= */ ImmutableList.of("high", "medium"))));
    assertThat(lowSplits).hasSize(1);
    assertThat(extractPaths(lowSplits.get(0).findEntriesUnderPath(ASSETS_DIRECTORY)))
        .containsExactly("assets/images#tier_low/image.jpg");

    List<ModuleSplit> mediumSplits =
        getSplitsWithTargetingEqualTo(
            assetsSplits,
            apkDeviceTierTargeting(
                deviceTierTargeting(
                    /* value= */ "medium", /* alternatives= */ ImmutableList.of("high", "low"))));
    assertThat(mediumSplits).hasSize(1);
    assertThat(extractPaths(mediumSplits.get(0).findEntriesUnderPath(ASSETS_DIRECTORY)))
        .containsExactly("assets/images#tier_medium/image.jpg");

    List<ModuleSplit> highSplits =
        getSplitsWithTargetingEqualTo(
            assetsSplits,
            apkDeviceTierTargeting(
                deviceTierTargeting(
                    /* value= */ "high", /* alternatives= */ ImmutableList.of("low", "medium"))));
    assertThat(highSplits).hasSize(1);
    assertThat(extractPaths(highSplits.get(0).findEntriesUnderPath(ASSETS_DIRECTORY)))
        .containsExactly("assets/images#tier_high/image.jpg");
  }

  @Test
  public void deviceTiers_lowTierMissing_ok() {
    BundleModule testModule =
        new BundleModuleBuilder("testModule")
            .addFile("assets/images#tier_medium/image.jpg")
            .addFile("assets/images#tier_high/image.jpg")
            .setAssetsConfig(
                assets(
                    targetedAssetsDirectory(
                        "assets/images#tier_medium",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                /* value= */ "medium",
                                /* alternatives= */ ImmutableList.of("high")))),
                    targetedAssetsDirectory(
                        "assets/images#tier_high",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                /* value= */ "high",
                                /* alternatives= */ ImmutableList.of("medium"))))))
            .setManifest(androidManifest("com.test.app"))
            .build();

    ModuleSplit baseSplit = ModuleSplit.forAssets(testModule);
    Collection<ModuleSplit> assetsSplits =
        DeviceTierAssetsSplitter.create(/* stripTargetingSuffix= */ false).split(baseSplit);

    assertThat(assetsSplits).hasSize(3);
    List<ModuleSplit> defaultSplits = getSplitsWithDefaultTargeting(assetsSplits);
    assertThat(defaultSplits).hasSize(1);
    assertThat(extractPaths(defaultSplits.get(0).findEntriesUnderPath(ASSETS_DIRECTORY))).isEmpty();

    List<ModuleSplit> mediumSplits =
        getSplitsWithTargetingEqualTo(
            assetsSplits,
            apkDeviceTierTargeting(
                deviceTierTargeting(
                    /* value= */ "medium", /* alternatives= */ ImmutableList.of("high"))));
    assertThat(mediumSplits).hasSize(1);
    assertThat(extractPaths(mediumSplits.get(0).findEntriesUnderPath(ASSETS_DIRECTORY)))
        .containsExactly("assets/images#tier_medium/image.jpg");

    List<ModuleSplit> highSplits =
        getSplitsWithTargetingEqualTo(
            assetsSplits,
            apkDeviceTierTargeting(
                deviceTierTargeting(
                    /* value= */ "high", /* alternatives= */ ImmutableList.of("medium"))));
    assertThat(highSplits).hasSize(1);
    assertThat(extractPaths(highSplits.get(0).findEntriesUnderPath(ASSETS_DIRECTORY)))
        .containsExactly("assets/images#tier_high/image.jpg");
  }

  @Test
  public void multipleDeviceTiers_withSuffixStripping() {
    BundleModule testModule =
        new BundleModuleBuilder("testModule")
            .addFile("assets/images#tier_low/image.jpg")
            .addFile("assets/images#tier_medium/image.jpg")
            .addFile("assets/images#tier_high/image.jpg")
            .setAssetsConfig(
                assets(
                    targetedAssetsDirectory(
                        "assets/images#tier_low",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                /* value= */ "low",
                                /* alternatives= */ ImmutableList.of("high", "medium")))),
                    targetedAssetsDirectory(
                        "assets/images#tier_medium",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                /* value= */ "medium",
                                /* alternatives= */ ImmutableList.of("high", "low")))),
                    targetedAssetsDirectory(
                        "assets/images#tier_high",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                /* value= */ "high",
                                /* alternatives= */ ImmutableList.of("low", "medium"))))))
            .setManifest(androidManifest("com.test.app"))
            .build();

    ModuleSplit baseSplit = ModuleSplit.forAssets(testModule);
    Collection<ModuleSplit> assetsSplits =
        DeviceTierAssetsSplitter.create(/* stripTargetingSuffix= */ true).split(baseSplit);

    assertThat(assetsSplits).hasSize(4);
    List<ModuleSplit> defaultSplits = getSplitsWithDefaultTargeting(assetsSplits);
    assertThat(defaultSplits).hasSize(1);
    assertThat(extractPaths(defaultSplits.get(0).findEntriesUnderPath(ASSETS_DIRECTORY))).isEmpty();

    List<ModuleSplit> lowSplits =
        getSplitsWithTargetingEqualTo(
            assetsSplits,
            apkDeviceTierTargeting(
                deviceTierTargeting(
                    /* value= */ "low", /* alternatives= */ ImmutableList.of("high", "medium"))));
    assertThat(lowSplits).hasSize(1);
    assertThat(extractPaths(lowSplits.get(0).findEntriesUnderPath(ASSETS_DIRECTORY)))
        .containsExactly("assets/images/image.jpg");

    List<ModuleSplit> mediumSplits =
        getSplitsWithTargetingEqualTo(
            assetsSplits,
            apkDeviceTierTargeting(
                deviceTierTargeting(
                    /* value= */ "medium", /* alternatives= */ ImmutableList.of("high", "low"))));
    assertThat(mediumSplits).hasSize(1);
    assertThat(extractPaths(mediumSplits.get(0).findEntriesUnderPath(ASSETS_DIRECTORY)))
        .containsExactly("assets/images/image.jpg");

    List<ModuleSplit> highSplits =
        getSplitsWithTargetingEqualTo(
            assetsSplits,
            apkDeviceTierTargeting(
                deviceTierTargeting(
                    /* value= */ "high", /* alternatives= */ ImmutableList.of("low", "medium"))));
    assertThat(highSplits).hasSize(1);
    assertThat(extractPaths(highSplits.get(0).findEntriesUnderPath(ASSETS_DIRECTORY)))
        .containsExactly("assets/images/image.jpg");
  }
}
