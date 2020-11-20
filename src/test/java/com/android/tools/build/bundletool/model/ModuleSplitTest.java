/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tools.build.bundletool.model;

import static com.android.tools.build.bundletool.model.AndroidManifest.ACTIVITY_ELEMENT_NAME;
import static com.android.tools.build.bundletool.model.AndroidManifest.ANDROID_NAMESPACE_URI;
import static com.android.tools.build.bundletool.model.AndroidManifest.IS_FEATURE_SPLIT_RESOURCE_ID;
import static com.android.tools.build.bundletool.model.AndroidManifest.NAME_RESOURCE_ID;
import static com.android.tools.build.bundletool.model.SourceStamp.STAMP_SOURCE_METADATA_KEY;
import static com.android.tools.build.bundletool.model.SourceStamp.STAMP_TYPE_METADATA_KEY;
import static com.android.tools.build.bundletool.testing.CertificateFactory.buildSelfSignedCertificate;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.androidManifest;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.withMainActivity;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.withSplitNameActivity;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.xmlAttribute;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.xmlBooleanAttribute;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.xmlDecimalIntegerAttribute;
import static com.android.tools.build.bundletool.testing.TargetingUtils.abiTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.alternativeLanguageTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.apkAbiTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.apkDensityTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.apkDeviceTierTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.apkLanguageTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.apkMultiAbiTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.apkSanitizerTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.apkTextureTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.deviceTierTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.lPlusVariantTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.languageTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.multiAbiTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.textureCompressionTargeting;
import static com.android.tools.build.bundletool.testing.TestUtils.createModuleEntryForFile;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.android.aapt.Resources.XmlElement;
import com.android.aapt.Resources.XmlNode;
import com.android.bundle.Targeting.Abi;
import com.android.bundle.Targeting.Abi.AbiAlias;
import com.android.bundle.Targeting.AbiTargeting;
import com.android.bundle.Targeting.ApkTargeting;
import com.android.bundle.Targeting.Sanitizer.SanitizerAlias;
import com.android.bundle.Targeting.ScreenDensity;
import com.android.bundle.Targeting.ScreenDensity.DensityAlias;
import com.android.bundle.Targeting.ScreenDensityTargeting;
import com.android.bundle.Targeting.TextureCompressionFormat.TextureCompressionFormatAlias;
import com.android.bundle.Targeting.VariantTargeting;
import com.android.tools.build.bundletool.model.SourceStamp.StampType;
import com.android.tools.build.bundletool.model.utils.xmlproto.XmlProtoElement;
import com.android.tools.build.bundletool.testing.BundleModuleBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ModuleSplitTest {

  private static final byte[] DUMMY_CONTENT = new byte[1];

  private static final int VERSION_CODE_RESOURCE_ID = 0x0101021b;

  private static SigningConfiguration stampSigningConfig;

  @BeforeClass
  public static void setUpClass() throws Exception {
    KeyPair keyPair = KeyPairGenerator.getInstance("RSA").genKeyPair();
    PrivateKey privateKey = keyPair.getPrivate();
    X509Certificate certificate = buildSelfSignedCertificate(keyPair, "CN=ModuleSplitTest");
    stampSigningConfig =
        SigningConfiguration.builder().setSignerConfig(privateKey, certificate).build();
  }

  @Test
  public void notPossibleToTargetMultipleDimensions() {
    ModuleSplit.Builder builder =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("testModule"))
            .setVariantTargeting(lPlusVariantTargeting())
            .setApkTargeting(
                ApkTargeting.newBuilder()
                    .setAbiTargeting(
                        AbiTargeting.newBuilder().addValue(Abi.newBuilder().setAlias(AbiAlias.X86)))
                    .setScreenDensityTargeting(
                        ScreenDensityTargeting.newBuilder()
                            .addValue(
                                ScreenDensity.newBuilder().setDensityAlias(DensityAlias.HDPI)))
                    .build());
    assertThrows(IllegalStateException.class, () -> builder.build());
  }

  @Test
  public void testMasterSplitIdEqualsToModuleName_Base() {
    ModuleSplit masterSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(lPlusVariantTargeting())
            .setMasterSplit(true)
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .build();
    masterSplit = masterSplit.writeSplitIdInManifest(masterSplit.getSuffix());
    assertThat(masterSplit.getAndroidManifest().getSplitId()).isEmpty();
  }

  @Test
  public void testMasterSplitIdEqualsToModuleName_nonBase() {
    ModuleSplit masterSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("moduleA"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(lPlusVariantTargeting())
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .setMasterSplit(true)
            .build();
    masterSplit = masterSplit.writeSplitIdInManifest(masterSplit.getSuffix());
    assertThat(masterSplit.getAndroidManifest().getSplitId()).hasValue("moduleA");
  }

  @Test
  public void testAbiSplitIdContainsModuleName() {
    ModuleSplit abiSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setEntries(fakeEntriesOf("lib/x86/libsome.so"))
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .setVariantTargeting(lPlusVariantTargeting())
            .setApkTargeting(
                apkAbiTargeting(ImmutableSet.of(AbiAlias.X86), ImmutableSet.of(AbiAlias.X86)))
            .setMasterSplit(false)
            .build();
    abiSplit = abiSplit.writeSplitIdInManifest(abiSplit.getSuffix());
    assertThat(abiSplit.getAndroidManifest().getSplitId()).hasValue("config.x86");
  }

  @Test
  public void masterSplitGetsManifestForFeatureSplit() throws Exception {
    BundleModule module =
        new BundleModuleBuilder("testModule").setManifest(androidManifest("com.test.app")).build();
    ModuleSplit split = ModuleSplit.forModule(module);

    split = split.writeSplitIdInManifest(split.getSuffix());
    XmlNode writtenManifest = split.getAndroidManifest().getManifestRoot().getProto();

    assertThat(writtenManifest.getElement().getAttributeList())
        .containsExactly(
            xmlAttribute("package", "com.test.app"),
            xmlDecimalIntegerAttribute(
                ANDROID_NAMESPACE_URI, "versionCode", VERSION_CODE_RESOURCE_ID, 1),
            xmlAttribute("split", "testModule"),
            xmlBooleanAttribute(
                ANDROID_NAMESPACE_URI, "isFeatureSplit", IS_FEATURE_SPLIT_RESOURCE_ID, true));
  }

  @Test
  public void moduleResourceSplitSuffixAndName() {
    ModuleSplit resSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setVariantTargeting(lPlusVariantTargeting())
            .setApkTargeting(apkDensityTargeting(DensityAlias.HDPI))
            .setMasterSplit(false)
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .build();
    resSplit = resSplit.writeSplitIdInManifest(resSplit.getSuffix());
    assertThat(resSplit.getAndroidManifest().getSplitId()).hasValue("config.hdpi");
  }

  @Test
  public void moduleTextureSplitSuffixAndName_alternatives() {
    ModuleSplit resSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setVariantTargeting(lPlusVariantTargeting())
            .setApkTargeting(
                apkTextureTargeting(
                    textureCompressionTargeting(
                        ImmutableSet.of(),
                        ImmutableSet.of(TextureCompressionFormatAlias.ATC)))) // not ATC.
            .setMasterSplit(false)
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .build();
    resSplit = resSplit.writeSplitIdInManifest(resSplit.getSuffix());
    assertThat(resSplit.getAndroidManifest().getSplitId()).hasValue("config.other_tcf");
  }

  @Test
  public void moduleAbiSplitSuffixAndName_alternatives() {
    ModuleSplit resSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setVariantTargeting(lPlusVariantTargeting())
            .setApkTargeting(
                apkAbiTargeting(
                    abiTargeting(
                        ImmutableSet.of(),
                        ImmutableSet.of(
                            AbiAlias.ARM64_V8A,
                            AbiAlias.ARMEABI,
                            AbiAlias.ARMEABI_V7A)))) // not "ARM".
            .setMasterSplit(false)
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .build();
    resSplit = resSplit.writeSplitIdInManifest(resSplit.getSuffix());
    assertThat(resSplit.getAndroidManifest().getSplitId()).hasValue("config.other_abis");
  }

  @Test
  public void moduleDeviceTierSplitSuffixAndName() {
    ModuleSplit deviceTieredSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setVariantTargeting(lPlusVariantTargeting())
            .setApkTargeting(
                apkDeviceTierTargeting(
                    deviceTierTargeting("low", ImmutableList.of("medium", "high"))))
            .setMasterSplit(false)
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .build();
    deviceTieredSplit = deviceTieredSplit.writeSplitIdInManifest(deviceTieredSplit.getSuffix());
    assertThat(deviceTieredSplit.getAndroidManifest().getSplitId()).hasValue("config.tier_low");
  }

  @Test
  public void apexModuleMultiAbiSplitSuffixAndName() {
    ModuleSplit resSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setVariantTargeting(VariantTargeting.getDefaultInstance())
            .setApkTargeting(
                apkMultiAbiTargeting(
                    multiAbiTargeting(
                        ImmutableSet.of(
                            ImmutableSet.of(AbiAlias.X86_64, AbiAlias.X86),
                            ImmutableSet.of(AbiAlias.ARM64_V8A, AbiAlias.ARMEABI_V7A)),
                        ImmutableSet.of())))
            .setMasterSplit(false)
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .build();
    resSplit = resSplit.writeSplitIdInManifest(resSplit.getSuffix());
    assertThat(resSplit.getAndroidManifest().getSplitId())
        .hasValue("config.armeabi_v7a.arm64_v8a_x86.x86_64");
  }

  @Test
  public void moduleLanguageSplitSuffixAndName() {
    ModuleSplit langSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setVariantTargeting(lPlusVariantTargeting())
            .setApkTargeting(apkLanguageTargeting(languageTargeting("es")))
            .setMasterSplit(false)
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .build();
    langSplit = langSplit.writeSplitIdInManifest(langSplit.getSuffix());
    assertThat(langSplit.getAndroidManifest().getSplitId()).hasValue("config.es");
  }

  @Test
  public void moduleLanguageSplitFallback_suffixAndName() {
    ModuleSplit langSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setVariantTargeting(lPlusVariantTargeting())
            .setApkTargeting(apkLanguageTargeting(alternativeLanguageTargeting("es")))
            .setMasterSplit(false)
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .build();
    langSplit = langSplit.writeSplitIdInManifest(langSplit.getSuffix());
    assertThat(langSplit.getAndroidManifest().getSplitId()).hasValue("config.other_lang");
  }

  @Test
  public void moduleSanitizerSplitSuffixAndName() {
    ModuleSplit sanitizerSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setVariantTargeting(lPlusVariantTargeting())
            .setApkTargeting(apkSanitizerTargeting(SanitizerAlias.HWADDRESS))
            .setMasterSplit(false)
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .build();
    sanitizerSplit = sanitizerSplit.writeSplitIdInManifest(sanitizerSplit.getSuffix());
    assertThat(sanitizerSplit.getAndroidManifest().getSplitId()).hasValue("config.hwasan");
  }

  @Test
  public void splitNameRemoved() {
    AndroidManifest manifest =
        AndroidManifest.create(
            androidManifest(
                "com.test.app",
                withMainActivity("MainActivity"),
                withSplitNameActivity("FooActivity", "foo")));
    ModuleSplit masterSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(lPlusVariantTargeting())
            .setMasterSplit(true)
            .setAndroidManifest(manifest)
            .build();
    masterSplit = masterSplit.removeSplitName();

    ImmutableList<XmlElement> activities =
        masterSplit
            .getAndroidManifest()
            .getManifestElement()
            .getChildElement("application")
            .getChildrenElements(ACTIVITY_ELEMENT_NAME)
            .map(XmlProtoElement::getProto)
            .collect(toImmutableList());
    assertThat(activities).hasSize(2);
    XmlElement activityElement = activities.get(1);
    assertThat(activityElement.getAttributeList())
        .containsExactly(
            xmlAttribute(ANDROID_NAMESPACE_URI, "name", NAME_RESOURCE_ID, "FooActivity"));
  }

  @Test
  public void removeUnknownSplits() {
    AndroidManifest manifest =
        AndroidManifest.create(
            androidManifest(
                "com.test.app",
                withMainActivity("MainActivity"),
                withSplitNameActivity("FooActivity", "foo")));
    ModuleSplit masterSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(lPlusVariantTargeting())
            .setMasterSplit(true)
            .setAndroidManifest(manifest)
            .build();
    masterSplit = masterSplit.removeUnknownSplitComponents(ImmutableSet.of());

    ImmutableList<XmlElement> activities =
        masterSplit
            .getAndroidManifest()
            .getManifestElement()
            .getChildElement("application")
            .getChildrenElements(ACTIVITY_ELEMENT_NAME)
            .map(XmlProtoElement::getProto)
            .collect(toImmutableList());
    assertThat(activities).hasSize(1);
    XmlElement activityElement = activities.get(0);
    assertThat(activityElement.getAttributeList())
        .containsExactly(
            xmlAttribute(ANDROID_NAMESPACE_URI, "name", NAME_RESOURCE_ID, "MainActivity"));
  }

  @Test
  public void testMasterBaseSplit_containsStamp() throws Exception {
    ModuleSplit masterSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(lPlusVariantTargeting())
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .setMasterSplit(true)
            .build();
    SourceStamp sourceStamp =
        SourceStamp.builder()
            .setSource("https://www.example.com")
            .setSigningConfiguration(stampSigningConfig)
            .build();
    StampType stampType = StampType.STAMP_TYPE_DISTRIBUTION_APK;

    masterSplit = masterSplit.writeSourceStampInManifest(sourceStamp.getSource(), stampType);

    assertThat(masterSplit.getAndroidManifest().getMetadataValue(STAMP_TYPE_METADATA_KEY))
        .hasValue(stampType.toString());
    assertThat(masterSplit.getAndroidManifest().getMetadataValue(STAMP_SOURCE_METADATA_KEY))
        .hasValue(sourceStamp.getSource());
  }

  @Test
  public void testNonMasterBaseSplit_doesNotContainStamp() throws Exception {
    ModuleSplit abiSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .setVariantTargeting(lPlusVariantTargeting())
            .setApkTargeting(
                apkAbiTargeting(ImmutableSet.of(AbiAlias.X86), ImmutableSet.of(AbiAlias.X86)))
            .setMasterSplit(false)
            .build();
    SourceStamp sourceStamp =
        SourceStamp.builder()
            .setSource("https://www.example.com")
            .setSigningConfiguration(stampSigningConfig)
            .build();
    StampType stampType = StampType.STAMP_TYPE_DISTRIBUTION_APK;

    abiSplit = abiSplit.writeSourceStampInManifest(sourceStamp.getSource(), stampType);

    assertThat(abiSplit.getAndroidManifest().getMetadataValue(STAMP_TYPE_METADATA_KEY)).isEmpty();
    assertThat(abiSplit.getAndroidManifest().getMetadataValue(STAMP_SOURCE_METADATA_KEY)).isEmpty();
  }

  @Test
  public void testStampSource_invalidUrl() throws Exception {
    ModuleSplit masterSplit =
        ModuleSplit.builder()
            .setModuleName(BundleModuleName.create("base"))
            .setApkTargeting(ApkTargeting.getDefaultInstance())
            .setVariantTargeting(lPlusVariantTargeting())
            .setAndroidManifest(AndroidManifest.create(androidManifest("com.test.app")))
            .setMasterSplit(true)
            .build();
    SourceStamp sourceStamp =
        SourceStamp.builder()
            .setSource("test-source")
            .setSigningConfiguration(stampSigningConfig)
            .build();
    StampType stampType = StampType.STAMP_TYPE_DISTRIBUTION_APK;

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> masterSplit.writeSourceStampInManifest(sourceStamp.getSource(), stampType));
    assertThat(exception)
        .hasMessageThat()
        .contains("Invalid stamp source. Stamp sources should be URLs.");
  }

  private ImmutableList<ModuleEntry> fakeEntriesOf(String... entries) {
    return Arrays.stream(entries)
        .map(entry -> createModuleEntryForFile(entry, DUMMY_CONTENT))
        .collect(toImmutableList());
  }

}
