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

package com.android.tools.build.bundletool.shards;

import static com.android.bundle.Targeting.TextureCompressionFormat.TextureCompressionFormatAlias.ATC;
import static com.android.bundle.Targeting.TextureCompressionFormat.TextureCompressionFormatAlias.ETC1_RGB8;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.androidManifest;
import static com.android.tools.build.bundletool.testing.TargetingUtils.alternativeTextureCompressionTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.apkDeviceTierTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.apkTextureTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.assets;
import static com.android.tools.build.bundletool.testing.TargetingUtils.assetsDirectoryTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.deviceTierTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.targetedAssetsDirectory;
import static com.android.tools.build.bundletool.testing.TargetingUtils.textureCompressionTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.variantTextureTargeting;
import static com.android.tools.build.bundletool.testing.TestUtils.createModuleEntryForFile;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;

import com.android.bundle.Config.SplitDimension;
import com.android.bundle.Config.SuffixStripping;
import com.android.bundle.Targeting.ApkTargeting;
import com.android.bundle.Targeting.VariantTargeting;
import com.android.tools.build.bundletool.model.AndroidManifest;
import com.android.tools.build.bundletool.model.BundleModuleName;
import com.android.tools.build.bundletool.model.ModuleEntry;
import com.android.tools.build.bundletool.model.ModuleSplit;
import com.android.tools.build.bundletool.model.ZipPath;
import com.android.tools.build.bundletool.model.targeting.TargetingDimension;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SuffixStripperTest {

  private static final byte[] DUMMY_CONTENT = new byte[1];

  @Test
  public void applySuffixStripping_tcf_suffixStrippingEnabled() {
    ModuleSplit split =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(VariantTargeting.getDefaultInstance())
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .setMasterSplit(true)
            .setEntries(
                ImmutableList.of(
                    createModuleEntryForFile(
                        "assets/textures/untargeted_texture.dat", DUMMY_CONTENT),
                    createModuleEntryForFile(
                        "assets/textures#tcf_etc1/etc1_texture.dat", DUMMY_CONTENT),
                    createModuleEntryForFile(
                        "assets/textures#tcf_atc/atc_texture.dat", DUMMY_CONTENT)))
            .setAssetsConfig(
                assets(
                    targetedAssetsDirectory(
                        "assets/textures",
                        assetsDirectoryTargeting(
                            alternativeTextureCompressionTargeting(ETC1_RGB8, ATC))),
                    targetedAssetsDirectory(
                        "assets/textures#tcf_etc1",
                        assetsDirectoryTargeting(textureCompressionTargeting(ETC1_RGB8))),
                    targetedAssetsDirectory(
                        "assets/textures#tcf_atc",
                        assetsDirectoryTargeting(textureCompressionTargeting(ATC)))))
            .build();

    ModuleSplit strippedSplit =
        SuffixStripper.createForDimension(TargetingDimension.TEXTURE_COMPRESSION_FORMAT)
            .applySuffixStripping(
                split,
                SuffixStripping.newBuilder().setDefaultSuffix("etc1").setEnabled(true).build());

    // Check that the ATC and untargeted sibling folders have been excluded, and suffix has been
    // stripped.
    assertThat(strippedSplit.getEntries()).hasSize(1);
    assertThat(strippedSplit.getEntries().get(0).getPath())
        .isEqualTo(ZipPath.create("assets/textures/etc1_texture.dat"));

    assertThat(strippedSplit.getAssetsConfig().get().getDirectoryCount()).isEqualTo(1);
    assertThat(strippedSplit.getAssetsConfig().get().getDirectory(0).getPath())
        .isEqualTo("assets/textures");

    // Check that the APK and Variant targeting were applied.
    assertThat(strippedSplit.getApkTargeting()).isEqualTo(apkTextureTargeting(ETC1_RGB8));
    assertThat(strippedSplit.getVariantTargeting()).isEqualTo(variantTextureTargeting(ETC1_RGB8));
  }

  @Test
  public void applySuffixStripping_tcf_suffixStrippingEnabledWithEmptyDefault() {
    ModuleSplit split =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(VariantTargeting.getDefaultInstance())
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .setMasterSplit(true)
            .setEntries(
                ImmutableList.of(
                    createModuleEntryForFile(
                        "assets/textures/untargeted_texture.dat", DUMMY_CONTENT),
                    createModuleEntryForFile(
                        "assets/textures#tcf_etc1/etc1_texture.dat", DUMMY_CONTENT),
                    createModuleEntryForFile(
                        "assets/textures#tcf_atc/atc_texture.dat", DUMMY_CONTENT)))
            .setAssetsConfig(
                assets(
                    targetedAssetsDirectory(
                        "assets/textures",
                        assetsDirectoryTargeting(
                            alternativeTextureCompressionTargeting(ETC1_RGB8, ATC))),
                    targetedAssetsDirectory(
                        "assets/textures#tcf_etc1",
                        assetsDirectoryTargeting(textureCompressionTargeting(ETC1_RGB8))),
                    targetedAssetsDirectory(
                        "assets/textures#tcf_atc",
                        assetsDirectoryTargeting(textureCompressionTargeting(ATC)))))
            .build();

    ModuleSplit strippedSplit =
        SuffixStripper.createForDimension(TargetingDimension.TEXTURE_COMPRESSION_FORMAT)
            .applySuffixStripping(
                split, SuffixStripping.newBuilder().setDefaultSuffix("").setEnabled(true).build());

    // Check that the ATC and ETC1 sibling folders have been excluded.
    assertThat(strippedSplit.getEntries()).hasSize(1);
    assertThat(strippedSplit.getEntries().get(0).getPath())
        .isEqualTo(ZipPath.create("assets/textures/untargeted_texture.dat"));

    assertThat(strippedSplit.getAssetsConfig().get().getDirectoryCount()).isEqualTo(1);
    assertThat(strippedSplit.getAssetsConfig().get().getDirectory(0).getPath())
        .isEqualTo("assets/textures");

    // Check that the APK and Variant targeting are empty.
    assertThat(strippedSplit.getApkTargeting()).isEqualToDefaultInstance();
    assertThat(strippedSplit.getVariantTargeting()).isEqualToDefaultInstance();
  }

  @Test
  public void applySuffixStripping_tcf_suffixStrippingDisabled_nonDefaultValuesExcluded() {
    ModuleSplit split =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(VariantTargeting.getDefaultInstance())
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .setMasterSplit(true)
            .setEntries(
                ImmutableList.of(
                    createModuleEntryForFile(
                        "assets/textures/untargeted_texture.dat", DUMMY_CONTENT),
                    createModuleEntryForFile(
                        "assets/textures#tcf_etc1/etc1_texture.dat", DUMMY_CONTENT),
                    createModuleEntryForFile(
                        "assets/textures#tcf_atc/atc_texture.dat", DUMMY_CONTENT)))
            .setAssetsConfig(
                assets(
                    targetedAssetsDirectory(
                        "assets/textures",
                        assetsDirectoryTargeting(
                            alternativeTextureCompressionTargeting(ETC1_RGB8, ATC))),
                    targetedAssetsDirectory(
                        "assets/textures#tcf_etc1",
                        assetsDirectoryTargeting(textureCompressionTargeting(ETC1_RGB8))),
                    targetedAssetsDirectory(
                        "assets/textures#tcf_atc",
                        assetsDirectoryTargeting(textureCompressionTargeting(ATC)))))
            .build();

    ModuleSplit strippedSplit =
        SuffixStripper.createForDimension(TargetingDimension.TEXTURE_COMPRESSION_FORMAT)
            .applySuffixStripping(
                split,
                SuffixStripping.newBuilder().setDefaultSuffix("etc1").setEnabled(false).build());

    // Check that the ATC and untargeted sibling folders have been excluded
    assertThat(strippedSplit.getEntries()).hasSize(1);
    assertThat(strippedSplit.getEntries().get(0).getPath())
        .isEqualTo(ZipPath.create("assets/textures#tcf_etc1/etc1_texture.dat"));

    assertThat(strippedSplit.getAssetsConfig().get().getDirectoryCount()).isEqualTo(1);
    assertThat(strippedSplit.getAssetsConfig().get().getDirectory(0).getPath())
        .isEqualTo("assets/textures#tcf_etc1");

    // Check that the APK and Variant targeting were applied.
    assertThat(strippedSplit.getApkTargeting()).isEqualTo(apkTextureTargeting(ETC1_RGB8));
    assertThat(strippedSplit.getVariantTargeting()).isEqualTo(variantTextureTargeting(ETC1_RGB8));
  }

  @Test
  public void applySuffixStripping_deviceTier_suffixStrippingEnabled() {
    ModuleSplit split =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(VariantTargeting.getDefaultInstance())
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .setMasterSplit(true)
            .setEntries(
                ImmutableList.of(
                    createModuleEntryForFile(
                        "assets/img#tier_low/low_res_image.dat", DUMMY_CONTENT),
                    createModuleEntryForFile(
                        "assets/img#tier_high/high_res_image.dat", DUMMY_CONTENT)))
            .setAssetsConfig(
                assets(
                    targetedAssetsDirectory(
                        "assets/img#tier_low",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                "low", /* alternatives= */ ImmutableList.of("high")))),
                    targetedAssetsDirectory(
                        "assets/img#tier_high",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                "high", /* alternatives= */ ImmutableList.of("low"))))))
            .build();

    ModuleSplit strippedSplit =
        SuffixStripper.createForDimension(TargetingDimension.DEVICE_TIER)
            .applySuffixStripping(
                split,
                SuffixStripping.newBuilder().setDefaultSuffix("low").setEnabled(true).build());

    // Check that the high tier sibling folder has been excluded
    assertThat(strippedSplit.getEntries()).hasSize(1);
    assertThat(strippedSplit.getEntries().get(0).getPath())
        .isEqualTo(ZipPath.create("assets/img/low_res_image.dat"));

    assertThat(strippedSplit.getAssetsConfig().get().getDirectoryCount()).isEqualTo(1);
    assertThat(strippedSplit.getAssetsConfig().get().getDirectory(0).getPath())
        .isEqualTo("assets/img");

    // Check that the APK and Variant targeting were applied.
    assertThat(strippedSplit.getApkTargeting())
        .isEqualTo(apkDeviceTierTargeting(deviceTierTargeting("low")));
    assertThat(strippedSplit.getVariantTargeting()).isEqualToDefaultInstance();
  }

  @Test
  public void applySuffixStripping_deviceTier_suffixStrippingDisabled_nonDefaultValuesExcluded() {
    ModuleSplit split =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(VariantTargeting.getDefaultInstance())
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .setMasterSplit(true)
            .setEntries(
                ImmutableList.of(
                    createModuleEntryForFile(
                        "assets/img#tier_low/low_res_image.dat", DUMMY_CONTENT),
                    createModuleEntryForFile(
                        "assets/img#tier_high/high_res_image.dat", DUMMY_CONTENT)))
            .setAssetsConfig(
                assets(
                    targetedAssetsDirectory(
                        "assets/img#tier_low",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                "low", /* alternatives= */ ImmutableList.of("high")))),
                    targetedAssetsDirectory(
                        "assets/img#tier_high",
                        assetsDirectoryTargeting(
                            deviceTierTargeting(
                                "high", /* alternatives= */ ImmutableList.of("low"))))))
            .build();

    ModuleSplit strippedSplit =
        SuffixStripper.createForDimension(TargetingDimension.DEVICE_TIER)
            .applySuffixStripping(
                split,
                SuffixStripping.newBuilder().setDefaultSuffix("low").setEnabled(false).build());

    // Check that the high tier sibling folder has been excluded
    assertThat(strippedSplit.getEntries()).hasSize(1);
    assertThat(strippedSplit.getEntries().get(0).getPath())
        .isEqualTo(ZipPath.create("assets/img#tier_low/low_res_image.dat"));

    assertThat(strippedSplit.getAssetsConfig().get().getDirectoryCount()).isEqualTo(1);
    assertThat(strippedSplit.getAssetsConfig().get().getDirectory(0).getPath())
        .isEqualTo("assets/img#tier_low");

    // Check that the APK and Variant targeting were applied.
    assertThat(strippedSplit.getApkTargeting())
        .isEqualTo(apkDeviceTierTargeting(deviceTierTargeting("low")));
    assertThat(strippedSplit.getVariantTargeting()).isEqualToDefaultInstance();
  }

  @Test
  public void removeAssetsTargeting_tcf() {
    ModuleSplit split =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(VariantTargeting.getDefaultInstance())
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .setMasterSplit(true)
            .setEntries(
                ImmutableList.of(
                    createModuleEntryForFile("assets/untargeted_texture.dat", DUMMY_CONTENT),
                    createModuleEntryForFile("assets/textures#tcf_etc1/texture.dat", DUMMY_CONTENT),
                    createModuleEntryForFile(
                        "assets/textures#tcf_etc1/other_texture.dat", DUMMY_CONTENT)))
            .setAssetsConfig(
                assets(
                    targetedAssetsDirectory(
                        "assets/textures#tcf_etc1",
                        assetsDirectoryTargeting(textureCompressionTargeting(ETC1_RGB8)))))
            .build();

    ModuleSplit strippedSplit =
        SuffixStripper.createForDimension(SplitDimension.Value.TEXTURE_COMPRESSION_FORMAT)
            .removeAssetsTargeting(split);

    // Check that the ATC and untargeted sibling folders have been excluded, and suffix has been
    // stripped.
    assertThat(strippedSplit.getEntries()).hasSize(3);
    assertThat(strippedSplit.getEntries().stream().map(ModuleEntry::getPath))
        .containsExactly(
            ZipPath.create("assets/untargeted_texture.dat"),
            ZipPath.create("assets/textures/texture.dat"),
            ZipPath.create("assets/textures/other_texture.dat"));

    assertThat(strippedSplit.getAssetsConfig().get().getDirectoryCount()).isEqualTo(1);
    assertThat(strippedSplit.getAssetsConfig().get().getDirectory(0).getPath())
        .isEqualTo("assets/textures");

    // Check that the APK and Variant targeting were applied.
    assertThat(strippedSplit.getApkTargeting()).isEqualToDefaultInstance();
    assertThat(strippedSplit.getVariantTargeting()).isEqualToDefaultInstance();
  }
}
